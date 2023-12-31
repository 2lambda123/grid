package com.gu.mediaservice.lib.logging

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MarkerUtilsTest extends AnyFunSpec with Matchers {
  it("should combine markers") {
    val marker1 = MarkerMap("a" -> "b")
    val marker2 = MarkerMap("c" -> "d")
    val combined = combineMarkers(marker1, marker2)
    combined.markerContents.toSeq should contain("a" -> "b")
    combined.markerContents.toSeq should contain("c" -> "d")

    val markerString = combined.toLogMarker.toString

    markerString should be("{a=b, c=d}")
  }
}
