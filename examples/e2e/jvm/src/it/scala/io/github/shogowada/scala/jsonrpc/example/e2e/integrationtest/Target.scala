package io.github.shogowada.scala.jsonrpc.example.e2e.integrationtest

import io.github.shogowada.scala.jsonrpc.example.test.utils.BaseTarget

object Target extends BaseTarget {
  override def healthCheckUrl = url + "/logs"
}
