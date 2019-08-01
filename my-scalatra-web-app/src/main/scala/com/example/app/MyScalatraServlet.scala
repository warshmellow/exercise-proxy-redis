package com.example.app

import org.scalatra._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.redis._
import org.slf4j.LoggerFactory
import com.example.app.MyScalatraServlet._

// JSON-related libraries
import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

class MyScalatraServlet(redisclient: RedisClient) extends ScalatraServlet with JacksonJsonSupport {
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  private val logger = LoggerFactory.getLogger(getClass)

  before() {
    contentType = formats("json")
  }

  get("/keys/:id") {
    val key = params("id")
    redisclient.get(key) match {
      case Some(value) =>
        logger.error(s"key:value ${params("id")}:$value")
        Ok(Pair(Some(key), value))
      case None => NotFound(s"Cannot find $key")
    }
  }

  put("/keys/:id") {
    val key = params("id")
    val pair: Pair = parsedBody.extract[Pair]
    logger.error(s"Request body from curl: $pair")
    redisclient.set(key, pair.value)
    Ok(pair.copy(key = Some(key)))
  }
}

object MyScalatraServlet {
  case class Pair(key: Option[String], value: String)
}