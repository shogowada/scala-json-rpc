package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.util.UUID
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RandomNumberSubject(
    observerApiRepository: RandomNumberObserverApiRepository
) extends RandomNumberSubjectApi {
  private var registeredObserverIds: Set[String] = Set()

  def start(): Unit = {
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
    val registeredObserverApis = observerApiRepository.getIdToApiMap
        .filterKeys(observerApi => registeredObserverIds.contains(observerApi))
        .values
    registeredObserverApis.foreach(api => api.notify(randomNumber))
  }

  override def createObserverId(): Future[String] = {
    val observerId = UUID.randomUUID().toString
    Future(observerId)
  }

  override def register(observerId: String): Unit = {
    println(s"Registering observer with ID $observerId")
    this.synchronized(registeredObserverIds = registeredObserverIds + observerId)
  }

  override def unregister(observerId: String): Unit = {
    println(s"Unregistering observer with ID $observerId")
    this.synchronized(registeredObserverIds = registeredObserverIds - observerId)
  }
}
