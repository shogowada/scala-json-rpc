package io.github.shogowada.scala.jsonrpc.example.e2e.integrationtest

import java.net.ServerSocket

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

object TargetController {

  lazy val port = freePort()
  lazy val url = s"http://localhost:$port"
  lazy val jarLocation = System.getProperty("jarLocation")

  def freePort(): Int = {
    val server = new ServerSocket(0)
    val localPort = server.getLocalPort
    server.close()
    localPort
  }

  lazy val target = startProcess(jarLocation, port)

  private def startProcess(jarLocation: String, port: Int): Process = {
    val process = new ProcessBuilder(
      "java", s"-Dport=$port", "-jar", jarLocation
    ).start()

    waitUntilReady()

    process
  }

  private def waitUntilReady(): Unit = {
    Range(0, 10).toStream
        .map(_ => {
          Thread.sleep(1000)
          val client = HttpClientBuilder.create().build()
          val response = client.execute(new HttpGet(s"$url/logs"))
          val code = response.getStatusLine.getStatusCode
          response.close()
          client.close()
          code
        })
        .filter(code => code == 200)
        .head
  }

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      target.destroy()
    }
  })
}
