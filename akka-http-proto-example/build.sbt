val http_client_version = "4.1.3"
val jackson_version = "2.8.8"
val slf4j_version = "1.6.4"
val scalatest_version = "3.0.4"
val scala_version = "2.12.4"

val group_id = "net.ndolgov"
val project_version = "1.0.2-SNAPSHOT"

val akka_http_proto_example_web_id = "akka-http-proto-example-web"
val akka_http_proto_example_web = Project(id = akka_http_proto_example_web_id, base = file(akka_http_proto_example_web_id)).
  settings(
    name         := akka_http_proto_example_web_id,
    organization := group_id,
    version      := project_version
  ).
  settings(
    PB.protoSources in Compile := Seq(sourceDirectory.value / "main/proto"),
    PB.targets in Compile := Seq(
      // compile your proto files into scala source files
      scalapb.gen(grpc=false) -> (sourceManaged in Compile).value,
      // generate the akka-http skeleton
      net.ndolgov.akkahttproto.generator.GatewayGenerator -> (sourceManaged in Compile).value
    )
  ).
  settings(
    resolvers += Resolver.bintrayRepo("beyondthelines", "maven")
  ).
  settings(
    libraryDependencies ++= Seq(
      "net.ndolgov" %% "akka-http-proto-runtime" % project_version % "compile,protobuf",
      "org.apache.httpcomponents" % "httpasyncclient" % http_client_version % Test,
      "org.apache.httpcomponents" % "httpcore" % http_client_version % Test,
      "org.scalatest" %% "scalatest" % scalatest_version % Test,
      "org.slf4j" % "slf4j-api" % slf4j_version,
      "org.slf4j" % "slf4j-log4j12" % slf4j_version
    )
  )

val akka_http_proto_example_root_id = "akka_http_proto_example_root"
val root = Project(id = akka_http_proto_example_root_id, base = file(".") ).
  settings(
    scalaVersion := scala_version,
    scalacOptions ++= Seq("-deprecation", "-Xfatal-warnings")
  ).
  settings(
    name         := akka_http_proto_example_root_id,
    organization := group_id,
    version      := project_version
  ).
  settings(
    resolvers += Resolver.defaultLocal
  ).
  settings(
    packageBin := { new File("") },
    packageSrc := { new File("") }
  ).
  aggregate(akka_http_proto_example_web)


