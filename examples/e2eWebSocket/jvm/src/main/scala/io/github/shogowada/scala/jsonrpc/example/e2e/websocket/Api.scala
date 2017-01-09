package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import java.util.UUID
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClientIdFactoryApiImpl extends ClientIdFactoryApi {
  override def create(): Future[String] = {
    val clientId = UUID.randomUUID().toString
    Future(clientId)
  }
}

class RandomNumberSubject(
    observerApiRepository: RandomNumberObserverApiRepository
) extends RandomNumberSubjectApi {
  private var registeredClientIds: Set[String] = Set()

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
        .filterKeys(clientId => registeredClientIds.contains(clientId))
        .values
    registeredObserverApis.foreach(api => api.notify(randomNumber))
  }

  override def register(clientId: String): Unit = {
    println(s"Registering observer with client ID $clientId")
    this.synchronized(registeredClientIds = registeredClientIds + clientId)
  }

  override def unregister(clientId: String): Unit = {
    println(s"Unregistering observer with client ID $clientId")
    this.synchronized(registeredClientIds = registeredClientIds - clientId)
  }
}
