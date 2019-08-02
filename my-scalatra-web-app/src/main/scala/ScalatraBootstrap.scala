import java.util.concurrent.TimeUnit

import com.example.app._
import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.scalatra._
import javax.servlet.ServletContext
import com.redis._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val r = new RedisClient("localhost", 7777)
    val cache = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(1, TimeUnit.SECONDS)
      .build[String, String](
        new CacheLoader[String, String]() {
          def load(key: String): String = {
            r.get(key) match {
              case Some(value) => value
              case None => throw new Exception(s"Cannot find $key")
            }
          }
        })
    
    context.mount(new MyScalatraServlet(new RedisClientAsGetAndSettable(r), cache), "/*")
  }
}
