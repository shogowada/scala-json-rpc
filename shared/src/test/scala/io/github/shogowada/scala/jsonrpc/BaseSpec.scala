package io.github.shogowada.scala.jsonrpc

import org.scalatest._

abstract class BaseSpec extends AsyncFreeSpec
    with OneInstancePerTest
    with Matchers
