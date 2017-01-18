package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import io.github.shogowada.scala.jsonrpc.JsonRpcFunction1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RandomNumberSubject extends RandomNumberSubjectApi {
  private var registeredObservers: Set[JsonRpcFunction1[Int, Future[Unit]]] = Set()

  def start(): Unit = {
    // Once started, it will generate and notify random numbers to registered observers every second.
    val threadPoolExecutor = new ScheduledThreadPoolExecutor(1)
    val executor = new Runnable {
      override def run() = {
        val randomNumber = (Math.random() * 100.0).toInt
        notifyObservers(randomNumber)
      }
    }
    threadPoolExecutor.scheduleAtFixedRate(executor, 1, 1, TimeUnit.SECONDS)
  }

  private def notifyObservers(randomNumber: Int): Unit = {
    registeredObservers.foreach(observer => {
      println(s"Sending $randomNumber to ${observer.hashCode()}")
      observer(randomNumber)
          .failed // Probably the connection is lost
          .foreach(_ => unregister(observer))
      println(s"Sent $randomNumber to ${observer.hashCode()}")
    })
  }

  override def register(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit = {
    println(s"Registering observer ${observer.hashCode()}")
    this.synchronized(registeredObservers = registeredObservers + observer)
  }

  override def unregister(observer: JsonRpcFunction1[Int, Future[Unit]]): Unit = {
    println(s"Unregistering observer ${observer.hashCode()}")
    this.synchronized(registeredObservers = registeredObservers - observer)
    observer.dispose()
  }
}
