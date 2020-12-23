package lib

import com.gu.mediaservice.lib.config.{CommonConfig, GridConfigResources}
import com.gu.permissions.PermissionDefinition

case class ScriptToLoad(
  host: String,
  path: String,
  async: Option[Boolean],
  permission: Option[PermissionDefinition]
)

class KahunaConfig(resources: GridConfigResources) extends CommonConfig(resources.configuration) {
  val rootUri: String = services.kahunaBaseUri
  val mediaApiUri: String = services.apiBaseUri
  val authUri: String = services.authBaseUri

  val sentryDsn: Option[String] = stringOpt("sentry.dsn").filterNot(_.isEmpty)

  val thumbOrigin: String = string("origin.thumb")
  val fullOrigin: String = string("origin.full")
  val cropOrigin: String = string("origin.crops")
  val imageOrigin: String = string("origin.images")
  val googleTrackingId: Option[String] = stringOpt("google.tracking.id").filterNot(_.isEmpty)

  val feedbackFormLink: Option[String]= stringOpt("links.feedbackForm").filterNot(_.isEmpty)
  val usageRightsHelpLink: Option[String]= stringOpt("links.usageRightsHelp").filterNot(_.isEmpty)
  val invalidSessionHelpLink: Option[String]= stringOpt("links.invalidSessionHelp").filterNot(_.isEmpty)
  val supportEmail: Option[String]= stringOpt("links.supportEmail").filterNot(_.isEmpty)

  val frameAncestors: Set[String] = getStringSet("security.frameAncestors")
  val connectSources: Set[String] = getStringSet("security.connectSources")

  val scriptsToLoad: List[ScriptToLoad] = getConfigList("scriptsToLoad").map(entry => ScriptToLoad(
    host = entry.getString("host"),
    path = entry.getString("path"),
    async = if (entry.hasPath("async")) Some(entry.getBoolean("async")) else None,
    permission =
      if (entry.hasPath("permission")) Some(
        PermissionDefinition(
          name = entry.getString("permission.name"),
          app = entry.getString("permission.app")
        )
      )
      else None
  ))

}

