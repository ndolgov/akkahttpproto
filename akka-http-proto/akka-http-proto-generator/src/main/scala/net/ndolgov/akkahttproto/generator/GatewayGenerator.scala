package net.ndolgov.akkahttproto.generator

import com.google.api.HttpRule.PatternCase
import com.google.api.{AnnotationsProto, HttpRule}
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.Descriptors.{FileDescriptor, MethodDescriptor, ServiceDescriptor}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import com.trueaccord.scalapb.compiler.FunctionalPrinter.PrinterEndo
import com.trueaccord.scalapb.compiler.{DescriptorPimps, FunctionalPrinter}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scalapbshade.v0_6_7.com.trueaccord.scalapb.Scalapb

// mostly borrowed from https://github.com/btlines/grpcgateway
object GatewayGenerator extends protocbridge.ProtocCodeGenerator with DescriptorPimps {
  override val params = com.trueaccord.scalapb.compiler.GeneratorParams()

  override def run(requestBytes: Array[Byte]): Array[Byte] = {
    val registry = ExtensionRegistry.newInstance()
    Scalapb.registerAllExtensions(registry)
    AnnotationsProto.registerAllExtensions(registry)

    val b = CodeGeneratorResponse.newBuilder
    val request = CodeGeneratorRequest.parseFrom(requestBytes, registry)
    val fileDescByName: Map[String, FileDescriptor] =
      request.getProtoFileList.asScala.foldLeft[Map[String, FileDescriptor]](Map.empty) {
        case (acc, fp) =>
          val deps = fp.getDependencyList.asScala.map(acc)
          acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
      }

    request.getFileToGenerateList.asScala.foreach { name =>
      val fileDesc = fileDescByName(name)

      b.addFile(new AkkaHttpServicePrinter(params).generateServiceTraitFile(fileDesc))

      val responseFile = generateFile(fileDesc)
      b.addFile(responseFile)
    }
    b.build.toByteArray
  }

  private def generateFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    val objectName = fileDesc.fileDescriptorObjectName.substring(0, fileDesc.fileDescriptorObjectName.length - 5) + "HttpEndpoint"
    b.setName(s"${fileDesc.scalaDirectory}/$objectName.scala")

    val fp = FunctionalPrinter()
      .add(s"package ${fileDesc.scalaPackageName}")
      .newline
      .add(
        "import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}",
        "import akka.http.scaladsl.server.Directives.{as, complete, entity, get, onComplete, path, pathPrefix, post, _}",
        "import akka.http.scaladsl.server.Route",
        "import org.slf4j.LoggerFactory",
        "import scala.util.{Failure, Success}"
      )
      .newline
      .add(
        "import net.ndolgov.akkahttproto.runtime.HttpEndpoints._"
      )
      .newline
      .add(
        "import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport",
        "import akka.http.scaladsl.marshalling.{PredefinedToEntityMarshallers, ToEntityMarshaller}",
        "import spray.json.{DefaultJsonProtocol, RootJsonFormat}"
      )
      .add(
        "import com.google.protobuf.ByteString"
      ) // entity/body conversion
      .newline
      .print(fileDesc.getServices.asScala) { case (p, s) => generateService(s)(p) }
      .newline

    b.setContent(fp.result)
    b.build
  }

  private def generateRoutes(methods: mutable.Seq[MethodDescriptor]): PrinterEndo = { printer =>
    printer.
      indent.
      add(methods.map(md => routeMethodName(md)).mkString(" ~ ")).
      outdent
  }

  private def routeMethodName(md: MethodDescriptor): String = {
    md.getName + "Route"
  }

  private def serviceMarshallersObjectName(service: ServiceDescriptor): String = {
    s"${service.getName}JsonMarshallers"
  }

  private def generateService(service: ServiceDescriptor): PrinterEndo = { printer =>
    val unaryMethods = getUnaryCallsWithHttpExtension(service)
    val marshallersClass = serviceMarshallersObjectName(service)

    val serviceName = s"${service.getName}"

    printer.
//      add(s"import ${serviceName}Akka.$serviceName").
//      newline.
      add(s"class ${serviceName}HttpEndpoint(service: $serviceName) {").
      indent.
      add(s"import $marshallersClass._").
      newline.
      add("private val logger = LoggerFactory.getLogger(this.getClass)").
      newline.
      add("private val exceptionHandler = unexpectedExceptionHandler(logger)").
      newline.
      add("private val rejectionHandler = garbledRequestHandler(logger)").
      newline.
      add("private val chunkMatcher = \"\"\"[^/]+\"\"\".r").
      newline.
      add("/** @return all Routes supported by this HTTP end point */").
      add("def endpointRoutes(): Route = {").
      call(generateRoutes(unaryMethods)).
      add("}").
      newline.
      call(generateRouteHandlersByVerb(unaryMethods)).
      outdent.
      add("}").
      newline.
      call(generateMessageMarshallers(marshallersClass, unaryMethods))
  }

  private def getUnaryCallsWithHttpExtension(service: ServiceDescriptor) = {
    service.getMethods.asScala.filter { m =>
      // only unary calls with http method specified
      !m.isClientStreaming && !m.isServerStreaming && m.getOptions.hasExtension(AnnotationsProto.http)
    }
  }

  private def generateMessageMarshallers(md: MethodDescriptor): PrinterEndo = { printer =>
    val requestType = md.getInputType.getName
    val requestFieldCount = md.getInputType.getFields.size()

    val responseType = md.getOutputType.getName
    val responseFieldCount = md.getOutputType.getFields.size()

    printer.
      //add(s"implicit lazy val ${requestType}Marshaller: FromEntityUnmarshaller[$requestType] = PredefinedFromEntityUnmarshallers.byteArrayUnmarshaller.map($requestType.parseFrom)").
      add(s"implicit lazy val ${responseType}Marshaller: ToEntityMarshaller[$responseType] = PredefinedToEntityMarshallers.ByteArrayMarshaller.wrap(MediaTypes.`application/octet-stream`)(_.toByteArray)")
  }

  private def generateMessageMarshallers(marshallersClass: String, methods: mutable.Buffer[MethodDescriptor]): PrinterEndo = { printer =>
    printer.
      add(s"private object $marshallersClass extends SprayJsonSupport with DefaultJsonProtocol {").
      indent.
      print(methods) { case (p, method) => generateMessageMarshallers(method)(p) }.
      outdent.
      add("}")
  }

  private def generateRouteHandlersByVerb(descritors: mutable.Seq[MethodDescriptor]): PrinterEndo = { printer =>
    val verbToMethods: mutable.Map[PatternCase, ArrayBuffer[RestfulMethod]] = MethodDescriptors.methodsByVerb(descritors)
    printer.
      print(verbToMethods) { case (p, (pattern, methods)) => generateRouteHandler(pattern, methods)(p) }
  }

  private def generateRouteHandler(verb: PatternCase, methods: Seq[RestfulMethod]): PrinterEndo = { printer =>
    printer
      .print(methods) { case (p, method) => generateRouteHandler(verb, method)(p) }
  }

  private def generateRouteHandler(verb: PatternCase, method: RestfulMethod): PrinterEndo = { printer =>
    printer.
      add(s"private def ${routeMethodName(method.method)}: Route =").
      indent.
      add("handleExceptions(exceptionHandler) {").
      indent.
      add(s"${verb.name().toLowerCase} {").
      indent.
      add(s"path(${method.path.pathMatcher()}) { ${method.path.matchedTuple()}").
      indent.
      add("handleRejections(rejectionHandler) {").
      indent.
      call(generateVerbRouteHandler(verb, method)).
      add("}"). // hr
      outdent.
      add("}"). // path
      outdent.
      add("}"). // method
      outdent.
      add("}"). // he
      outdent.
      newline
  }

  private def generateVerbRouteHandler(verb: PatternCase, method: RestfulMethod): PrinterEndo = { printer =>
    val http = method.method.getOptions.getExtension(AnnotationsProto.http)

    http.getPatternCase match {
      case PatternCase.GET =>
        printer.call(
          generateGetRouteHandler(verb, method))

      case PatternCase.POST => printer.call(
        generatePostRouteHandler(verb, method))

      case PatternCase.PUT => printer.call(
        generatePostRouteHandler(verb, method))

      // case PatternCase.DELETE => TODO

      case _ => printer
    }

  }

  private def generateGetRouteHandler(verb: PatternCase, method: RestfulMethod): PrinterEndo = { printer =>
    val requestType = method.method.getInputType.getName

    printer.
      add(s"val request = $requestType(${method.path.requestParams()})").
      call(generateRequestCompletion(method.method))
  }

  private def generatePostRouteHandler(verb: PatternCase, method: RestfulMethod): PrinterEndo = { printer =>
    val requestType = method.method.getInputType.getName

    if (method.httpBody.isDefined) {
      val bodyType = method.method.getInputType.findFieldByName(method.httpBody.get).getType

      if (bodyType == Type.BYTES) {
        printer.
          add(s"extractRequestEntity { entity =>").
          indent.
          add(s"entity match {").
          indent.
          add(s"case strict: HttpEntity.Strict =>").
          indent.
          add(s"val body = ByteString.copyFrom(strict.data.toArray)").
          add(s"val request = $requestType(${method.path.requestParams()}, ${method.httpBody.get} = body)").
          call(generateRequestCompletion(method.method)).
          newline.
          add("case _ => complete(httpErrorResponse(StatusCodes.BadRequest, \"Unexpected request body type\"))").
          outdent.
          add("}"). // e m
          outdent.
          add("}"). // eRE
          outdent
      } else {
        throw new IllegalArgumentException(s"Unsupported HTTP request body type: $bodyType") // TODO support actual Request field conversion
      }
    } else {
      printer.
        add(s"val request = $requestType(${method.path.requestParams()})").
        indent.
        call(generateRequestCompletion(method.method)).
        add("}"). // entity
        outdent
    }
  }

  private def generateRequestCompletion(method: MethodDescriptor): PrinterEndo = { printer =>
    printer.
      add(s"onComplete(service.${method.getName}(request)) {").
      indent.
      add("case Success(response) =>").
      indent.
      add("complete(response)").
      outdent.
      newline.
      add("case Failure(e) =>").
      indent.
      add("val message = \"Unexpectedly failed to process request\"").
      add("logger.error(message, e)").
      add("complete(httpErrorResponse(StatusCodes.InternalServerError, message))").
      outdent.
      outdent.
      add("}"). // onC
      outdent
  }
}

private case class RestfulMethod(path: ParsedPath, method: MethodDescriptor, httpBody: Option[String])

private object MethodDescriptors {
  private val matcher = "chunkMatcher" // the name of the regex matching path parameters in the generated code

  def methodsByVerb(descriptors: mutable.Seq[MethodDescriptor]) : mutable.Map[PatternCase, ArrayBuffer[RestfulMethod]] = {
    val map = mutable.Map[PatternCase, ArrayBuffer[RestfulMethod]]()

    descriptors.foreach((md: MethodDescriptor) => {
      val http = md.getOptions.getExtension(AnnotationsProto.http)
      val seq = map.getOrElseUpdate(http.getPatternCase, ArrayBuffer())
      seq += RestfulMethod(ParsedPath(urlTemplate(http), matcher), md, Option(http.getBody))
    })

    map
  }

  private def urlTemplate(http: HttpRule): String = {
    http.getPatternCase match {
      case PatternCase.GET => http.getGet
      case PatternCase.POST => http.getPost
      case PatternCase.PUT => http.getPut
      case PatternCase.DELETE => http.getDelete
      case _ => throw new IllegalArgumentException(s"Unsupported HTTP verb pattern: ${http.getPatternCase}")
    }
  }
}
