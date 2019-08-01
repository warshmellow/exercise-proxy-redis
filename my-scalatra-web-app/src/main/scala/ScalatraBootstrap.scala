import com.example.app._
import org.scalatra._
import javax.servlet.ServletContext
import com.redis._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val r = new RedisClient("localhost", 7777)
    
    context.mount(new MyScalatraServlet(r), "/*")
  }
}
