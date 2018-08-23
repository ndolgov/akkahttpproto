package net.ndolgov.akkahttpproto.example

import java.util.concurrent.ExecutorService

import net.ndolgov.akkahttpproto.example.JsonMarshaller._
import net.ndolgov.akkahttpproto.example.api.objectstore.{CreateObjectRequest, CreateObjectResponse, GetObjectRequest, GetObjectResponse}
import org.apache.http.HttpResponse
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/** This class represents the idea of "an HTTP client that can make a POST request with JSON body attached".
  * For simplicity it's actually implemented with Apache httpclient library. Jackson JSON marshaller is used for message
  * (de)serialization.
  * Both could be compared to Akka HTTP-based client and Spray-based marshaller in [[https://github.com/ndolgov/experiments/tree/master/akkahttptest akkahttptest]]. */
final class AkkaHttpSkeletonClient(client: CloseableHttpClient, url: String)(implicit val ec: ExecutionContext)  {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def stop(): Unit = {
    client.close()
  }

  def executionContext() : ExecutionContext = ec

  def getObject(request: GetObjectRequest) : Future[GetObjectResponse] = {
    get(request, s"/${request.userId}/padding/${request.objectId}", classOf[GetObjectResponse])
  }
  def createObject(request: CreateObjectRequest) : Future[CreateObjectResponse] = {
    post(request, s"/${request.userId}", request.blob.toByteArray, classOf[CreateObjectResponse])
  }

  private def post[REQUEST, RESPONSE](request: REQUEST, path: String, body: Array[Byte], clazz: Class[RESPONSE]) : Future[RESPONSE] = {
    Future {
      logger.info(s"Sending $request")

      val httpRequest = new HttpPost(url + path)
      httpRequest.setEntity(new ByteArrayEntity(body))
      client.execute(httpRequest, handler(clazz))
    }
  }

  private def get[REQUEST, RESPONSE](request: REQUEST, path: String, clazz: Class[RESPONSE]) : Future[RESPONSE] = {
    Future {
      logger.info(s"Sending $request")

      val httpRequest = new HttpGet(path)
      client.execute(httpRequest, handler(clazz))
    }
  }

  private def handler[RESPONSE](clazz: Class[RESPONSE]): ResponseHandler[RESPONSE] = {
    (httpResponse: HttpResponse) => {
      val status = httpResponse.getStatusLine.getStatusCode
      logger.info(s"Handling response with status $status")

      if (status < 200 && status >= 300) {
        throw new RuntimeException("Unexpected response status: " + status)
      }

      val entity = httpResponse.getEntity
      if (entity == null) {
        throw new RuntimeException("No response body found")
      }

      val response = fromJson(EntityUtils.toString(entity), clazz)
      logger.info(s"Received $response")
      response
    }
  }
}

/** Create HTTP client to a given service URL */
object AkkaHttpSkeletonClient {
  def apply(url: String, executor: ExecutorService) : AkkaHttpSkeletonClient = {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
    new AkkaHttpSkeletonClient(HttpClients.createDefault(), url)
  }
}
