package lib

import com.gu.mediaservice.lib.metrics.CloudWatchMetrics

class ThrallMetrics(config: ThrallConfig) extends CloudWatchMetrics(s"${config.stage}/Thrall", config) {

  val indexedImages = new CountMetric("IndexedImages")

  val deletedImages = new CountMetric("DeletedImages")

  val softReaped = new CountMetric("SoftReaped")
  val hardReaped = new CountMetric("HardReaped")
  val softReapable = new CountMetric("SoftReapable")
  val hardReapable = new CountMetric("HardReapable")

  val failedDeletedImages = new CountMetric("FailedDeletedImages")

  val failedMetadataUpdates = new CountMetric("FailedMetadataUpdates")

  val failedCollectionsUpdates = new CountMetric("FailedCollectionsUpdates")

  val failedExportsUpdates = new CountMetric("FailedExportsUpdates")

  val failedUsagesUpdates = new CountMetric("FailedUsagesUpdates")

  val failedSyndicationRightsUpdates = new CountMetric("FailedSyndicationRightsUpdates")

  val failedQueryUpdates = new CountMetric("FailedQueryUpdates")

  val failedDeletedAllUsages = new CountMetric("FailedDeletedAllUsages")

  val processingLatency = new TimeMetric("ProcessingLatency")

  val snsMessage = new CountMetric("SNSMessage")

}
