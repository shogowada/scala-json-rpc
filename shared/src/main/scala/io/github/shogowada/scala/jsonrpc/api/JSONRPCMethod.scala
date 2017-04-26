package io.github.shogowada.scala.jsonrpc.api

import scala.annotation.StaticAnnotation

case class JSONRPCMethod
(
    name: String
) extends StaticAnnotation
