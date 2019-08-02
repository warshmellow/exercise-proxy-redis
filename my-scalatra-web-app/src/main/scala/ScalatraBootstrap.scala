import java.util.concurrent.{Semaphore, TimeUnit}

import com.example.app._
import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.scalatra._
import javax.servlet.ServletContext
import com.redis._
import com.typesafe.config.ConfigFactory

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val conf = ConfigFactory.load
    val redisHost = conf.getString("redisHost")
    val redisPort = conf.getInt("redisPort")
    val cacheExpiryTimeSeconds = conf.getLong("cacheExpiryTimeSeconds")
    val cacheCapacity = conf.getLong("cacheCapacity")
    val maximumConcurrentConnections = conf.getInt("maximumConcurrentConnections")

    val r = new RedisClient(redisHost, redisPort)
    val cache = CacheBuilder.newBuilder()
      .maximumSize(cacheCapacity)
      .expireAfterWrite(cacheExpiryTimeSeconds, TimeUnit.SECONDS)
      .build[String, String](
        new CacheLoader[String, String]() {
          def load(key: String): String = {
            r.get(key) match {
              case Some(value) => value
              case None => throw new Exception(s"Cannot find $key")
            }
          }
        })
    val requestSemaphore = new Semaphore(maximumConcurrentConnections)
    context.mount(new MyScalatraServlet(new RedisClientAsGetAndSettable(r), cache, requestSemaphore), "/*")
  }
}
