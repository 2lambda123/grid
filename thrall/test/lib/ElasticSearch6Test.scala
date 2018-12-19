package lib

import play.api.Configuration

class ElasticSearch6Test extends ElasticSearchTestBase {

  val thrallConfig = new ThrallConfig(Configuration.from(Map(
    "es.cluster" -> "media-service-test",
    "es.port" -> "9206",
    "es.index.aliases.write" -> "writeAlias"
  )))

  val thrallMetrics = new ThrallMetrics(thrallConfig)

  val ES = new ElasticSearch6(thrallConfig, thrallMetrics)

}