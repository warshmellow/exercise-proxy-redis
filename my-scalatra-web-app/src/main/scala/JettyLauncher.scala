// remember this package in the sbt project definition
import com.example.app.MyScalatraServlet
import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object JettyLauncher { // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    val conf = ConfigFactory.load

    val port = conf.getInt("httpPort")

    val server = new Server(port)
    val context = new WebAppContext()
    context setContextPath "/"
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[MyScalatraServlet], "/")

    server.setHandler(context)

    server.start()
    server.join()
  }
}