package com.example.app

import org.scalatra._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.redis._

class MyScalatraServlet(redisclient: RedisClient) extends ScalatraServlet {

  get("/keys/:id") {
      redisclient.get(params("id")) match {
        case Some(value) => Ok(value)
        case None => NotFound()
      }
  }

  put("/keys/:id") {
    redisclient.set(params("id"), request.body)
    Ok()
  }
}
