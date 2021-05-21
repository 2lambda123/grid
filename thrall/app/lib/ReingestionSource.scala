package lib

import java.time.Instant
import akka.{Done, NotUsed}
import akka.stream.alpakka.elasticsearch.ReadResult
import akka.stream.alpakka.elasticsearch.scaladsl.ElasticsearchSource
import akka.stream.scaladsl.Source
import com.gu.mediaservice.lib.aws.UpdateMessage
import com.gu.mediaservice.model.Image
import com.gu.mediaservice.model.Image.ImageReads
import lib.elasticsearch.ElasticSearch
import org.elasticsearch.client.RestClient
import play.api.libs.json.Json
import spray.json.DefaultJsonProtocol.jsonFormat1
import spray.json.{JsObject, JsonFormat}

import scala.concurrent.Future

case class ReingestionRecord(payload: Array[Byte], approximateArrivalTimestamp: Instant)

object ReingestionSource {
  def apply(implicit es: RestClient): Source[(UpdateMessage, Instant), Future[Done]] = {
    implicit val format: JsonFormat[Image] = ???
    val x = ElasticsearchSource
      .typed[Image](
        indexName = "source",
        typeName = "_doc",
        query = """{"match_all": {}}"""
      )
    val y: Source[(UpdateMessage, Instant), NotUsed] = x.map { imageResult: ReadResult[Image] =>
      (UpdateMessage("reproject-image", Some(imageResult.source)), java.time.Instant.now())
    }
    y.mapMaterializedValue(_ => Future.successful(Done))
  }
}
