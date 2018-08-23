addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.13")

resolvers += Resolver.bintrayRepo("beyondthelines", "maven")
resolvers += Resolver.defaultLocal // todo remove eventually

val scalapb_plugin_version = "0.6.7"
val generator_version = "1.0.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % scalapb_plugin_version,
  "net.ndolgov" %% "akka-http-proto-generator" % generator_version
)