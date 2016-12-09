package io.github.shogowada.scala.jsonrpc.api

import scala.annotation.StaticAnnotation

class JsonRpcMethod(val name: String = "") extends StaticAnnotation
