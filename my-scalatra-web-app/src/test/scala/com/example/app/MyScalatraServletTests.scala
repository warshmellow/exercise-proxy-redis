package com.example.app

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.redis.RedisClient
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest._
import com.example.app.MyScalatraServlet._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{read, write}

class MyScalatraServletTests extends ScalatraFunSuite with MockFactory {
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  val redisMock: GetAndSettable = mock[GetAndSettable]

  val cache: LoadingCache[String, String] = CacheBuilder.newBuilder()
    .maximumSize(1)
    .expireAfterWrite(1000, TimeUnit.SECONDS)
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

  test("GET /key/:id on MyScalatraServlet should cache return status 200 if key is found") {
    val validKey = "validKey"
    val validValue = "validValue"
    // The noMoreThanOnce call ensures value is grabbed from cache the second time
    (redisMock.get _).expects(validKey).returns(Some("validValue")).noMoreThanOnce()

    get(s"/keys/$validKey") {
      assertResult(200)(status)
      assertResult(Pair(Some(validKey), validValue)) {
        parse(body).extract[Pair]
      }
    }

    get(s"/keys/$validKey") {
      assertResult(200)(status)
      assertResult(Pair(Some(validKey), validValue)) {
        parse(body).extract[Pair]
      }
    }
  }

  test("GET /key/:id on MyScalatraServlet should return status 404 if key is not found") {
    val invalidKey = "invalidKey"
    (redisMock.get _).expects(invalidKey).returns(None)

    get(s"/keys/$invalidKey") {
      assertResult(404)(status)
      assert(body.isBlank)
    }
  }

  test("PUT /key/:id on MyScalatraServlet should return status 200 if key is set") {
    val validKey = "validKey"
    val validValue = "validValue"
    (redisMock.set _).expects(validKey, validValue)
    val json = write(Pair(None, validValue))

    put(uri = s"/keys/$validKey", body = json.getBytes()) {
      assertResult(200)(status)
      assertResult(Pair(Some(validKey), validValue))(parse(body).extract[Pair])
    }
  }

  test("PUT /key/:id on MyScalatraServlet should return status 200 and cast to string if value is not string") {
    val validKey = "validKey"
    val invalidValue = 500L
    val json = s"""{"value":$invalidValue}"""
    (redisMock.set _).expects(validKey, invalidValue.toString)

    put(uri = s"/keys/$validKey", body = json.getBytes()) {
      assertResult(200)(status)
      assertResult(Pair(Some(validKey), invalidValue.toString))(parse(body).extract[Pair])
    }
  }

  test("LRU eviction and fixed key size") {
    val validKey1 = "validKey1"
    val validValue1 = "validValue1"
    val validKey2 = "validKey2"
    val validValue2 = "validValue2"
    (redisMock.set _).expects(*, *).repeat(2)
    val json1 = write(Pair(None, validValue1))
    val json2 = write(Pair(None, validValue2))

    put(uri = s"/keys/$validKey1", body = json1.getBytes()) {
      assertResult(200)(status)
      assertResult(Pair(Some(validKey1), validValue1))(parse(body).extract[Pair])
    }

    put(uri = s"/keys/$validKey2", body = json2.getBytes()) {
      assertResult(200)(status)
      assertResult(Pair(Some(validKey2), validValue2))(parse(body).extract[Pair])
    }

    assertResult(null)(cache.getIfPresent(validKey1))
  }
}
