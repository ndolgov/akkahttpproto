package net.ndolgov.akkahttproto.generator

import java.nio.file.{Files, Paths}

import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import org.scalatest.{Assertions, FlatSpec}
import protocbridge.frontend.PluginFrontend

class GatewayGeneratorTest extends FlatSpec with Assertions {
  private val destDir = "akka-http-proto-generator/target/scala-2.12/test-classes/"
  private val srcDir = "/ndolgov/github/akka-http-proto/data/"

  it should "parse protobuf" in {
    val protoPath = destDir + "testsvcA.proto"

    val requestProtoStream = Files.newInputStream(Paths.get(srcDir + "request.bin"))
    val request = CodeGeneratorRequest.parseFrom(requestProtoStream)
    //println(request)
    //Files.write(Paths.get(destDir + "request.proto"), request.toString.getBytes)
    requestProtoStream.close()

//    val responseProtoStream = Files.newInputStream(Paths.get(srcDir + "response.bin"))
//    val response = CodeGeneratorRequest.parseFrom(responseProtoStream)
    //Files.write(Paths.get(destDir + "reponse.proto"), response.toString.getBytes)
//    println(response)
//    responseProtoStream.close()

    //    val request = CodeGeneratorRequest.newBuilder()
//      .addFileToGenerate(protoPath)
//      .addProtoFile(FileDescriptorProto.newBuilder()
//        .setName(protoPath)
//        .setPackage("net.ndolgov.akkahttpproto")
//        .addMessageType(DescriptorProto.newBuilder()
//          .setName("AkkaService"))).build()

    val responseBytes: Array[Byte] = PluginFrontend.runWithBytes(GatewayGenerator, request.toByteArray)

    val generatedResponse = CodeGeneratorResponse.parseFrom(responseBytes)
    println(generatedResponse)

    for (i <- 0 until generatedResponse.getFileCount) {
      val file  = generatedResponse.getFile(i)
      Files.write(Paths.get(destDir + file.getName.substring(file.getName.lastIndexOf("/") + 1)), file.getContent.getBytes())
    }

    //new CodeGenerator(sources).generateCode(request)
    //val protoStream = Files.newInputStream(Paths.get(dir + "testsvcA.proto"))
    //val req = CodeGeneratorRequest.parseFrom(protoStream)
    //val req = CodeGeneratorRequest.parseFrom(protoStream)
    //protoStream.close()
      //CodeGeneratorRequest.newBuilder().addProtoFile(FileDescriptorProto.newBuilder().)

//    val fsin = Files.newInputStream(Paths.get(dir + "testsvcA.proto"))
//    val response2 = PluginFrontend.runWithInputStream(GatewayGenerator, fsin)
//    Files.write(Paths.get(dir + "testsvcA.scala"), response2)
//    fsin.close()

    //GatewayGenerator.run()
  }
}
