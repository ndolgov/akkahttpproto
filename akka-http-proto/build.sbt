import com.trueaccord.scalapb.compiler.Version.scalapbVersion
import sbt.Keys.crossScalaVersions

val akka_http_version = "10.0.10"
val googleapis_version = "0.0.3"
val slf4j_version = "1.6.4"
val scalatest_version = "3.0.4"
val scala_version = "2.12.4"

val group_id = "net.ndolgov"
val project_version = "1.0.2-SNAPSHOT"

val akka_http_proto_runtime_id = "akka-http-proto-runtime"
lazy val akka_http_proto_runtime = Project(id = akka_http_proto_runtime_id, base = file(akka_http_proto_runtime_id)).
  settings(
    name         := akka_http_proto_runtime_id,
    organization := group_id,
    version      := project_version
  ).settings(
    crossScalaVersions := Seq("2.12.4"),
  ).
  settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akka_http_version,
      "com.typesafe.akka" %% "akka-http-core" % akka_http_version,
      "com.typesafe.akka" %% "akka-http-spray-json" % akka_http_version,
      "org.slf4j" % "slf4j-api" % slf4j_version,
      "org.slf4j" % "slf4j-log4j12" % slf4j_version,
      "org.scalatest" %% "scalatest" % scalatest_version % Test,
      "com.google.api.grpc" % "googleapis-common-protos" % "0.0.3" % "protobuf"
    )).settings(
  PB.protoSources in Compile += target.value / "protobuf_external",
  includeFilter in PB.generate := new SimpleFilter(
    file => file.endsWith("annotations.proto") || file.endsWith("http.proto")
  ),
  PB.targets in Compile += scalapb.gen() -> (sourceManaged in Compile).value,
  mappings in (Compile, packageBin) ++= Seq(
    baseDirectory.value / "target" / "protobuf_external" / "google" / "api" / "annotations.proto" -> "google/api/annotations.proto",
    baseDirectory.value / "target" / "protobuf_external" / "google" / "api" / "http.proto"        -> "google/api/http.proto"
  )
)

val akka_http_proto_generator_id = "akka-http-proto-generator"
val akka_http_proto_generator = Project(id = akka_http_proto_generator_id, base = file(akka_http_proto_generator_id)).
  settings(
    name         := akka_http_proto_generator_id,
    organization := group_id,
    version      := project_version
  ).settings(
    crossScalaVersions := Seq("2.12.4", "2.10.6"),
  ).
  settings(
    PB.protoSources in Compile += target.value / "protobuf_external",
    includeFilter in PB.generate := new SimpleFilter(
      file => file.endsWith("annotations.proto") || file.endsWith("http.proto")
    ),
    PB.targets in Compile += PB.gens.java -> (sourceManaged in Compile).value
  ).
  settings(
    libraryDependencies ++= Seq(
      "com.trueaccord.scalapb" %% "compilerplugin" % scalapbVersion,
      "com.google.api.grpc" % "googleapis-common-protos" % googleapis_version % "protobuf",
      "org.scalatest" %% "scalatest" % scalatest_version % Test
    )
  )

val akka_http_proto_root_id = "akka-http-proto-root"
val root = Project(id = akka_http_proto_root_id, base = file(".") ).
  settings(
    scalaVersion := scala_version,
    scalacOptions ++= Seq("-deprecation", "-Xfatal-warnings")
  ).
  settings(
    name         := akka_http_proto_root_id,
    organization := group_id,
    version      := project_version
  ).
  settings(
    packageBin := { new File("") },
    packageSrc := { new File("") }
  ).
  aggregate(akka_http_proto_generator, akka_http_proto_runtime)
