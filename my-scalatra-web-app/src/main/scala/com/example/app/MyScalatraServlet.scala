package com.example.app

import java.util.concurrent.Semaphore

import com.example.app.MyScalatraServlet._
import com.google.common.cache.LoadingCache
import org.scalatra._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

// JSON-related libraries
import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

class MyScalatraServlet(redisClient: GetAndSettable, cache: LoadingCache[String, String], requestSemaphore: Semaphore)
  extends ScalatraServlet with JacksonJsonSupport with FutureSupport {
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  protected implicit def executor: ExecutionContext = ExecutionContext.Implicits.global

  private val logger = LoggerFactory.getLogger(getClass)

  before() {
    contentType = formats("json")
    if (!requestSemaphore.tryAcquire()) {
      halt(ServiceUnavailable(""))
    }
  }

  after() {
    requestSemaphore.release()
  }

  get("/keys/:id") {
    new AsyncResult {
      val is =
        Future {
          val key = params("id")
          Try(cache.get(key)) match {
            case Success(value) =>
              logger.debug(s"key:value ${params("id")}:$value")
              Ok(Pair(Some(key), value))
            case Failure(exception) =>
              logger.error(s"Cannot find $key", exception)
              NotFound("")
          }
        }
    }
  }

  put("/keys/:id") {
    val key = params("id")
    val pair: Pair = parsedBody.extract[Pair]
    logger.error(s"Request body from curl: $pair")
    redisClient.set(key, pair.value)
    Ok(pair.copy(key = Some(key)))
  }
}

object MyScalatraServlet {
  case class Pair(key: Option[String], value: String)
}