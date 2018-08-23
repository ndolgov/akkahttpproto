#### protobuf to akka-http code generator

This experimental utility can be used to generate an akka-http service skeleton from a protobuf IDL.

In Jan 2018 it was intended as a PoC for replacing RESTful drudgery with a gRPC-style approach. It is not
production ready, it depends on obscure SBT plugin(s) and esoteric incantations in SBT files. I may improve it a little but make no promises. 

It is heavily based on [Scala grpcgateway](https://github.com/btlines/grpcgateway) project.
The gateway is a rare example of plugging JVM code into the protoc tool chain.

##### References

* [protobuf HTTP extension spec](https://cloud.google.com/service-management/reference/rpc/google.api#httprule)
* [protobuf HTTP extension](https://github.com/googleapis/googleapis/blob/master/google/api/http.proto)
* [Scala grpcgateway blog](https://www.beyondthelines.net/computing/grpc-rest-gateway-in-scala/)   


##### Running locally

* ```sbt publishLocal``` to build a local version of the generator. Its use is explained by the akka-http-proto-example app.