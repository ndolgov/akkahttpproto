package net.ndolgov.akkahttpproto.example

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import com.google.protobuf.ByteString
import net.ndolgov.akkahttpproto.example.api.objectstore.{CreateObjectRequest, GetObjectRequest, ObjectStoreServiceHttpEndpoint}
import net.ndolgov.akkahttproto.runtime.AkkaHttpServer
import org.scalatest.{Assertions, FlatSpec}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AkkaHttpSkeletonTest extends FlatSpec with Assertions {
  private val logger = LoggerFactory.getLogger(classOf[AkkaHttpSkeletonTest])
  private val PORT = 20000
  private val userId = "123"
  private val REQUEST_IDB = 124
  private val HOSTNAME = "127.0.0.1"
  private val THREADS = 4

  it should "support two service endpoints on the same port" in {
    val server = AkkaHttpServer(HOSTNAME, PORT, Some(serverExecutorSvc()))
    val f= server.start(ec =>
      new ObjectStoreServiceHttpEndpoint(
        new ObjectStoreServiceImpl()(ec)).
      endpointRoutes())

    Await.ready(f, Duration.create(5, TimeUnit.SECONDS))

    val client = AkkaHttpSkeletonClient(s"http://$HOSTNAME:$PORT/tree/trunk/branch/leaf", clientExecutorSvc())

    try {
      implicit val clientExecutionContext: ExecutionContext = client.executionContext()

      val body = "covfefe"

      val createAndGet =
        handle(client.createObject(CreateObjectRequest(userId, ByteString.copyFrom(body.getBytes())))).
        flatMap(cor =>
          handle(client.getObject(GetObjectRequest(userId, cor.objectId)))).
        map(gor => new String(gor.blob.toByteArray))

      assert(Await.result(createAndGet, Duration.create(5, TimeUnit.SECONDS)) == body)
    } finally {
      client.stop()
      server.stop()
    }
  }

  private def handle[A](future: Future[A])(implicit ec: ExecutionContext): Future[A] = {
    future.onComplete((triedA: Try[A]) => triedA match {
      case Success(response) =>
        logger.info("Processing response: " + response)

      case Failure(e) =>
        logger.error("RPC call failed ", e)
    })

    future
  }

  private def serverExecutorSvc(): ExecutorService = {
    executorSvc("rpc-server-%d")
  }

  private def clientExecutorSvc(): ExecutorService =
    executorSvc("rpc-client-%d")

  private def executorSvc(format: String): ExecutorService =
    Executors.newFixedThreadPool(THREADS)
}
