package io.github.shogowada.scala.jsonrpc.example.e2e.websocket

import io.github.shogowada.scala.jsonrpc.serializers.UpickleJsonSerializer

object JsonRpcModule {

  lazy val randomNumberSubject = new RandomNumberSubject

  lazy val jsonSerializer = UpickleJsonSerializer()
}
