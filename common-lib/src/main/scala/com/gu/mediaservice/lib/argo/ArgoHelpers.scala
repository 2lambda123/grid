package com.gu.mediaservice.lib.argo

import java.net.URI
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Cookie, Result, Results}
import com.gu.mediaservice.lib.argo.model._
import com.gu.mediaservice.lib.logging.GridLogging
import com.typesafe.scalalogging.Logger


trait ArgoHelpers extends Results with GridLogging {

  val ArgoMediaType = "application/vnd.argo+json"

  // FIXME: DSL to append links and actions?
  def respond[T](data: T, links: List[Link] = Nil, actions: List[Action] = Nil, uri: Option[URI] = None)
                (implicit writes: Writes[T]): Result = {
    val response = EntityResponse(
      uri     = uri,
      data    = data,
      links   = links,
      actions = actions
    )

    serializeAndWrap(response, Ok)
  }

  def respondCollection[T](data: Seq[T], offset: Option[Long] = None, total: Option[Long] = None, maybeOrgOwnedCount: Option[Long] = None,
                           links: List[Link] = Nil, uri: Option[URI] = None)
                          (implicit writes: Writes[T]): Result = {
    val response = CollectionResponse(
      uri    = uri,
      offset = offset,
      length = Some(data.size),
      total  = total,
      data   = data,
      links  = links,
      //FIXME using actions below is a hack to get the org owned count into the response (until such time as we move theseus into the grid, so client can safely parse the response)
      actions = maybeOrgOwnedCount
    )

    serializeAndWrap(response, Ok)
  }

// TODO: bring back once useful (causes Scala compiler tears)
//  def respondError[T](errorStatus: Status, errorKey: String, errorMessage: String, data: Option[T], links: List[Link] = Nil)
//                     (implicit writes: Writes[T]): Result = {
//    val response = ErrorResponse(
//      errorKey     = errorKey,
//      errorMessage = errorMessage,
//      data         = data,
//      links        = links
//    )
//
//    serializeAndWrap(response, errorStatus)
//  }

  // TODO: find a nicer way to serialise ErrorResponse[Nothing] without this hack
  def respondError(errorStatus: Status, errorKey: String, errorMessage: String, links: List[Link] = Nil): Result = {
    logger.warn(s"[$errorKey] Responding with error status ${errorStatus.header.status}, $errorMessage")
    val response = ErrorResponse[Int](
      errorKey     = errorKey,
      errorMessage = errorMessage,
      data         = None,
      links        = links
    )

    serializeAndWrap(response, errorStatus)
  }

  def respondNotFound(errorMessage: String): Result = {
    val response = ErrorResponse[Int](
      errorKey     = "not-found",
      errorMessage = errorMessage,
      data         = None,
      links        = Nil
    )

    serializeAndWrap(response, Status(404))
  }


  protected def serializeAndWrap[T](response: T, status: Status)(implicit writes: Writes[T]): Result = {
    val json = Json.toJson(response)
    status(json).as(ArgoMediaType)
  }

}
