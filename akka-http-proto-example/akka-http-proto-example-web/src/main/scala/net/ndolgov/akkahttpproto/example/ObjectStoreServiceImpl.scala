package net.ndolgov.akkahttpproto.example

import java.util.concurrent.atomic.AtomicReference

import com.google.protobuf.ByteString
import net.ndolgov.akkahttpproto.example.api.objectstore.{CreateObjectRequest, CreateObjectResponse, GetObjectRequest, GetObjectResponse, ObjectStoreService}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ObjectStoreServiceImpl(implicit ec: ExecutionContext) extends ObjectStoreService {
  private val logger = LoggerFactory.getLogger(classOf[ObjectStoreServiceImpl])

  private val obj = new AtomicReference[ByteString]()

  override def getObject(request: GetObjectRequest): Future[GetObjectResponse] = Future {
    logger.info(s"Processing $request")
    GetObjectResponse(status = 200, blob = obj.get())
  }

  override def createObject(request: CreateObjectRequest): Future[CreateObjectResponse] = Future {
    logger.info(s"Processing $request")
    obj.set(request.blob)
    CreateObjectResponse(status = 200, objectId = "OBJ" + System.currentTimeMillis())
  }
}
