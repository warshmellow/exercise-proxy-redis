package com.example.app

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.redis.RedisClient
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest._
import com.example.app.MyScalatraServlet._
import org.json4s._
import org.json4s.jackson.JsonMethods._

class MyScalatraServletTests extends ScalatraFunSuite with MockFactory {
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  val redisMock: GetAndSettable = mock[GetAndSettable]

  val cache: LoadingCache[String, String] = CacheBuilder.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(1, TimeUnit.SECONDS)
    .build[String, String](
      new CacheLoader[String, String]() {
        def load(key: String): String = {
          redisMock.get(key) match {
            case Some(value) => value
            case None => throw new Exception(s"Cannot find $key")
          }
        }
      })
  addServlet(new MyScalatraServlet(redisMock, cache), "/*")

  test("GET /key/:id on MyScalatraServlet should return status 200 if key is found") {
    val validKey = "validKey"
    val validValue = "validValue"
    (redisMock.get _).expects(validKey).returns(Some("validValue"))

    get(s"/keys/$validKey") {
      assertResult(200)(status)
      assertResult(Pair(Some(validKey), validValue)) {
        parse(body).extract[Pair]
      }
    }
  }
}
