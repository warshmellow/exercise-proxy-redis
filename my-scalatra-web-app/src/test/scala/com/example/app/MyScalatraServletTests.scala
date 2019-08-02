package com.example.app

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.redis.RedisClient
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest._

class MyScalatraServletTests extends ScalatraFunSuite with MockFactory {

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

  test("GET / on MyScalatraServlet should return status 200") {
    get("/") {
      status should equal (200)
    }
  }

}
