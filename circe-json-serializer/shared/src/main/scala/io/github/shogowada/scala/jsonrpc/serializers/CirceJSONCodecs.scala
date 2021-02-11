package io.github.shogowada.scala.jsonrpc.serializers

import io.circe.Decoder.decodeOption
import io.circe.Encoder.encodeOption
import io.circe.{Decoder, Encoder, Error, HCursor, Json}
import io.github.shogowada.scala.jsonrpc.Models._
import io.github.shogowada.scala.jsonrpc.Types.Id

object CirceJSONCodecs {

  // JSONRPCMethod
  implicit val encodeJsonRpcMethod: Encoder[JSONRPCMethod] = (jsonRpcMethod: JSONRPCMethod) =>
    Json.obj(
      ("jsonrpc", Json.fromString(jsonRpcMethod.jsonrpc)),
      ("method", Json.fromString(jsonRpcMethod.method)),
    )

  implicit val decodeJsonRpcMethod: Decoder[JSONRPCMethod] = (c: HCursor) => {
    for {
      jsonrpc <- c.downField("jsonrpc").as[String]
      method <- c.downField("method").as[String]
    } yield {
      JSONRPCMethod(jsonrpc, method)
    }
  }

  // Id
  implicit val encodeId: Encoder[Id] = (id: Id) => {
    id.fold(Json.fromString, Json.fromBigDecimal)
  }

  implicit val decodeId: Decoder[Id] = (c: HCursor) => {
    c.as[String] match {
      case Right(a) => Right(Left(a))
      case _ => c.as[BigDecimal].map(Right(_))
    }
  }

  // JSONRPCId
  implicit val encodeJsonRpcId: Encoder[JSONRPCId] = (jsonRpcId: JSONRPCId) =>
    Json.obj(
      ("jsonrpc", Json.fromString(jsonRpcId.jsonrpc)),
      ("id", encodeId(jsonRpcId.id)),
    )

  implicit val decodeJsonRpcId: Decoder[JSONRPCId] = (c: HCursor) => {
    for {
      jsonrpc <- c.downField("jsonrpc").as[String]
      id <- c.downField("id").as[Id]
    } yield {
      JSONRPCId(jsonrpc, id)
    }
  }

  // JSONRPCRequest
  implicit def encodeJsonRpcRequest[T](implicit encoder: Encoder[T]): Encoder[JSONRPCRequest[T]] = (jsonRpcRequest: JSONRPCRequest[T]) =>
    Json.obj(
      ("jsonrpc", Json.fromString(jsonRpcRequest.jsonrpc)),
      ("id", encodeId(jsonRpcRequest.id)),
      ("method", Json.fromString(jsonRpcRequest.method)),
      ("params", encoder(jsonRpcRequest.params))
    )

  implicit def decodeJsonRpcRequest[T](implicit decoder: Decoder[T]): Decoder[JSONRPCRequest[T]] = (c: HCursor) => {
    for {
      jsonrpc <- c.downField("jsonrpc").as[String]
      id <- c.downField("id").as[Id]
      method <- c.downField("method").as[String]
      params <- c.downField("params").as[T]
    } yield {
      JSONRPCRequest(jsonrpc, id, method, params)
    }
  }

  // JSONRPCNotification
  implicit def encodeJsonRpcNotification[T](implicit encoder: Encoder[T]): Encoder[JSONRPCNotification[T]] = (jsonRpcNotification: JSONRPCNotification[T]) =>
    Json.obj(
      ("jsonrpc", Json.fromString(jsonRpcNotification.jsonrpc)),
      ("method", Json.fromString(jsonRpcNotification.method)),
      ("params", encoder(jsonRpcNotification.params))
    )

  implicit def decodeJsonRpcNotification[T](implicit decoder: Decoder[T]): Decoder[JSONRPCNotification[T]] = (c: HCursor) => {
    for {
      jsonrpc <- c.downField("jsonrpc").as[String]
      method <- c.downField("method").as[String]
      params <- c.downField("params").as[T]
    } yield {
      JSONRPCNotification(jsonrpc, method, params)
    }
  }

  // JSONRPCResultResponse
  implicit def encodeJsonRpcResultResponse[T](implicit encoder: Encoder[T]): Encoder[JSONRPCResultResponse[T]] = (jsonRpcResultResponse: JSONRPCResultResponse[T]) =>
    Json.obj(
      ("jsonrpc", Json.fromString(jsonRpcResultResponse.jsonrpc)),
      ("id", encodeId(jsonRpcResultResponse.id)),
      ("result", encoder(jsonRpcResultResponse.result))
    )

  implicit def decodeJsonRpcResultResponse[T](implicit decoder: Decoder[T]): Decoder[JSONRPCResultResponse[T]] = (c: HCursor) => {
    for {
      jsonrpc <- c.downField("jsonrpc").as[String]
      id <- c.downField("id").as[Id]
      result <- c.downField("result").as[T]
    } yield {
      JSONRPCResultResponse(jsonrpc, id, result)
    }
  }

  // JSONRPCError
  implicit def encodeJsonRpcError[T](implicit encoder: Encoder[T]): Encoder[JSONRPCError[T]] = (jsonRpcError: JSONRPCError[T]) =>
    Json.obj(
      ("code", Json.fromInt(jsonRpcError.code)),
      ("message", Json.fromString(jsonRpcError.message)),
      ("data", encodeOption[T].apply(jsonRpcError.data))
    )

  implicit def decodeJsonRpcError[T](implicit decoder: Decoder[T]): Decoder[JSONRPCError[T]] = (c: HCursor) => {
    for {
      code <- c.downField("code").as[Int]
      message <- c.downField("message").as[String]
      data <- c.downField("data").as[Option[T]]
    } yield {
      JSONRPCError(code, message, data)
    }
  }

  // JSONRPCErrorResponse
  implicit def encodeJsonRpcErrorResponse[T](implicit encoder: Encoder[T]): Encoder[JSONRPCErrorResponse[T]] = (jsonRpcErrorResponse: JSONRPCErrorResponse[T]) =>
    Json.obj(
      ("jsonrpc", Json.fromString(jsonRpcErrorResponse.jsonrpc)),
      ("id", encodeId(jsonRpcErrorResponse.id)),
      ("error", encodeJsonRpcError[T].apply(jsonRpcErrorResponse.error))
    )

  implicit def decodeJsonRpcErrorResponse[T](implicit decoder: Decoder[T]): Decoder[JSONRPCErrorResponse[T]] = (c: HCursor) => {
    for {
      jsonrpc <- c.downField("jsonrpc").as[String]
      id <- c.downField("id").as[Id]
      error <- c.downField("error").as[JSONRPCError[T]]
    } yield {
      JSONRPCErrorResponse(jsonrpc, id, error)
    }
  }
}
