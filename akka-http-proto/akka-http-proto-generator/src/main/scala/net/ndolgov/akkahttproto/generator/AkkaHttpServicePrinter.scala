package net.ndolgov.akkahttproto.generator

import com.google.protobuf.Descriptors.{FileDescriptor, MethodDescriptor, ServiceDescriptor}
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.trueaccord.scalapb.compiler.FunctionalPrinter.PrinterEndo
import com.trueaccord.scalapb.compiler.{DescriptorPimps, FunctionalPrinter, GeneratorParams, StreamType}

import scala.collection.JavaConverters._

/**
  * To generate only a service trait when scalapb.gen(grpc=false) is used.
  * Liberally borrowed from
  * https://github.com/scalapb/ScalaPB/blob/master/compiler-plugin/src/main/scala/scalapb/compiler/GrpcServicePrinter.scala
  */
final class AkkaHttpServicePrinter(parameters: GeneratorParams) extends DescriptorPimps {
  override val params: GeneratorParams = parameters

  def generateServiceTraitFile(fileDesc: FileDescriptor): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    val objectName = fileDesc.fileDescriptorObjectName.substring(0, fileDesc.fileDescriptorObjectName.length - 5)
    b.setName(s"${fileDesc.scalaDirectory}/$objectName.scala")

    val fp = FunctionalPrinter()
      .add(s"package ${fileDesc.scalaPackageName}")
      .print(fileDesc.getServices.asScala) { case (p, s) => generateService(s)(p) }

    b.setContent(fp.result)
    b.build
  }

  private def generateService(service: ServiceDescriptor): PrinterEndo = { printer =>
    printer
//      .add("package " + service.getFile.scalaPackageName, "", s"object ${service.objectName} {")
      .indent
      .call(serviceTrait(service))
      .outdent
//      .add("}")
  }

  private def serviceTrait(service: ServiceDescriptor): PrinterEndo = {
    val endos: PrinterEndo = { p =>
      p.seq(service.methods.map(m => serviceMethodSignature(m)))
    }

    p =>
      p.add(s"trait ${service.name} {")
        .indent
        .call(endos)
        .outdent
        .add("}")
  }

  private def serviceMethodSignature(method: MethodDescriptor) = {
    s"def ${method.name}" + (method.streamType match {
      case StreamType.Unary => s"(request: ${method.scalaIn}): scala.concurrent.Future[${method.scalaOut}]"

      case _ => throw new IllegalArgumentException(s"Unsupported stream type: ${method.streamType}")
    })
  }
}
