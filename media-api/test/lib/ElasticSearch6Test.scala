package lib

import com.gu.mediaservice.lib.auth.Syndication
import com.gu.mediaservice.model.Image
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http._
import lib.elasticsearch.{ElasticSearch6, SearchFilters, SearchParams}
import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers.appendEntries
import org.joda.time.{DateTime, DateTimeUtils}
import play.api.libs.json.Json
import play.api.{Configuration, Logger, MarkerContext}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class ElasticSearch6Test extends ElasticSearchTestBase {

  private val index = "images"
  private val client = ElasticClient(ElasticProperties("http://" + "localhost" + ":" + 9206)) // TODO obtain from ES6 instance

  private val mediaApiConfig = new MediaApiConfig(Configuration.from(Map(
    "es.cluster" -> "media-service-test",
    "es.port" -> "9206",
    "persistence.identifier" -> "picdarUrn",
    "es.index.aliases.read" -> "readAlias")))

  private val mediaApiMetrics = new MediaApiMetrics(mediaApiConfig)
  private val searchFilters = new SearchFilters(mediaApiConfig)

  val ES = new ElasticSearch6(mediaApiConfig, searchFilters, mediaApiMetrics)

  override def beforeAll {
    ES.ensureAliasAssigned()
    Await.ready(saveImages(images), 1.minute)

    // allow the cluster to distribute documents... eventual consistency!
    Thread.sleep(5000)

    // mocks `DateTime.now`
    val startDate = DateTime.parse("2018-03-01")
    DateTimeUtils.setCurrentMillisFixed(startDate.getMillis)
  }

  override def afterAll  {
    Await.ready(deleteImages(), 5.seconds)
    DateTimeUtils.setCurrentMillisSystem()
  }

  describe("Tiered API access") {
    it("ES should return only rights acquired pictures with an allow syndication lease for a syndication tier search") {
      val searchParams = SearchParams(tier = Syndication)

      val searchResult = ES.search(searchParams)

      whenReady(searchResult, timeout, interval) { result =>
        result.total shouldBe 3

        val imageIds = result.hits.map(_._1)
        imageIds.size shouldBe 3
        imageIds.contains("test-image-1") shouldBe true
        imageIds.contains("test-image-2") shouldBe true
        imageIds.contains("test-image-4") shouldBe true
      }
    }
  }

  private def saveImages(images: Seq[Image]) = {
    Future.sequence(images.map { i =>
      println(i)
      executeAndLog(indexInto(index, "_doc") id i.id source Json.stringify(Json.toJson(i)), s"Indexing test image")
    })
  }

  private def deleteImages() = {
    // client.prepareDelete().setIndex("images").executeAndLog(s"Deleting index")
    val request = deleteIndex(index)
    executeAndLog(request, s"Deleting index")
  }

  def executeAndLog[T, U](request: T, message: String)(implicit
                                                       functor: Functor[Future],
                                                       executor: Executor[Future],
                                                       handler: Handler[T, U],
                                                       manifest: Manifest[U]): Future[Response[U]] = {
    val start = System.currentTimeMillis()

    val result = client.execute(request).transform {
      case Success(r) =>
        r.isSuccess match {
          case true => Success(r)
          case false => Failure(new RuntimeException("query response was not successful: " + r.error.reason))
        }
      case Failure(f) => Failure(f)
    }

    result.foreach { r =>
      val elapsed = System.currentTimeMillis() - start
      val markers = MarkerContext(durationMarker(elapsed))
      Logger.info(s"$message - query returned successfully in $elapsed ms")(markers)
    }

    result.failed.foreach { e =>
      val elapsed = System.currentTimeMillis() - start
      val markers = MarkerContext(durationMarker(elapsed))
      Logger.error(s"$message - query failed after $elapsed ms: ${e.getMessage} cs: ${e.getCause}")(markers)
    }

    result
  }

  private def durationMarker(elapsed: Long): LogstashMarker = appendEntries(Map("duration" -> elapsed).asJava)

}
