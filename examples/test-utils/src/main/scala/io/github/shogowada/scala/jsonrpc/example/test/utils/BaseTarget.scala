package io.github.shogowada.scala.jsonrpc.example.test.utils

import java.net.ServerSocket

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

import scala.util.Try

trait BaseTarget {

  lazy val port = freePort()
  lazy val url = s"http://localhost:$port"
  lazy val jarLocation = System.getProperty("jarLocation")

  def healthCheckUrl = url

  def freePort(): Int = {
    val server = new ServerSocket(0)
    val localPort = server.getLocalPort
    server.close()
    localPort
  }

  private def startProcess(jarLocation: String, port: Int): Process = {
    new ProcessBuilder(
      "java", s"-Dport=$port", "-jar", jarLocation
    ).start()
  }

  private def waitUntilReady(): Unit = {
    Range(0, 10).toStream
        .map(_ => {
          Thread.sleep(1000)
          val client = HttpClientBuilder.create().build()
          val maybeCode = Try {
            val response = client.execute(new HttpGet(healthCheckUrl))
            val code = response.getStatusLine.getStatusCode
            response.close()
            code
          }.toOption
          client.close()
          maybeCode
        })
        .filter(code => code.contains(200))
        .head
  }

  val target = startProcess(jarLocation, port)

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      target.destroy()
    }
  })

  waitUntilReady()
}
