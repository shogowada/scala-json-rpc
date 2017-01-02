package io.github.shogowada.scala.jsonrpc.api

import scala.annotation.StaticAnnotation

case class JsonRpcMethod
(
    name: String
) extends StaticAnnotation
