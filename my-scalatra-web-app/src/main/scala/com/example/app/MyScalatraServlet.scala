package com.example.app

import org.scalatra._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.redis._
import org.slf4j.LoggerFactory
import com.example.app.MyScalatraServlet._
import com.google.common.cache.LoadingCache

import scala.util.{Failure, Success, Try}

// JSON-related libraries
import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

class MyScalatraServlet(redisClient: GetAndSettable, cache: LoadingCache[String, String]) extends ScalatraServlet with JacksonJsonSupport {
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  private val logger = LoggerFactory.getLogger(getClass)

  before() {
    contentType = formats("json")
  }

  get("/keys/:id") {
    val key = params("id")

    Try(cache.get(key)) match {
      case Success(value) =>
        logger.error(s"key:value ${params("id")}:$value")
        Ok(Pair(Some(key), value))
      case Failure(exception) =>
        logger.error(s"Cannot find $key", exception)
        NotFound("")
    }
  }

  put("/keys/:id") {
    val key = params("id")
    val pair: Pair = parsedBody.extract[Pair]
    logger.error(s"Request body from curl: $pair")
    redisClient.set(key, pair.value.toString)
    Ok(pair.copy(key = Some(key)))
  }
}

object MyScalatraServlet {
  case class Pair(key: Option[String], value: String)
}