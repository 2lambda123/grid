package lib

import com.gu.mediaservice.lib.config.CommonConfig
import play.api.Configuration

class KahunaConfig(override val configuration: Configuration) extends CommonConfig {

  final override lazy val appName = "kahuna"

  val rootUri: String = services.kahunaBaseUri
  val mediaApiUri: String = services.apiBaseUri
  val authUri: String = services.authBaseUri

  val sentryDsn: Option[String] = properties.get("sentry.dsn").filterNot(_.isEmpty)
  val watUri: Option[String] = properties.get("wat.uri").filterNot(_.isEmpty)

  val thumbOrigin: String = properties("origin.thumb")
  val fullOrigin: String = properties("origin.full")
  val cropOrigin: Option[String] = properties.get("origin.crops")
}
