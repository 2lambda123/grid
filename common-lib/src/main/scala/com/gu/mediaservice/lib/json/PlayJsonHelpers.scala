package com.gu.mediaservice.lib.json

import scala.PartialFunction.condOpt

import play.api.libs.json._
import play.api.libs.json.JsString

trait PlayJsonHelpers {

  def logParseErrors[A](parseResult: JsResult[A]): Either[Seq[(JsPath, Seq[JsonValidationError])], A] = parseResult.asEither

  def string(v: JsValue): Option[String] =
    condOpt(v) { case JsString(s) => s }

  def array(v: JsValue): Option[List[JsValue]] =
    condOpt(v) { case JsArray(vs) => vs.toList }

}

object PlayJsonHelpers extends PlayJsonHelpers
