package model.upload

import java.io.File

import com.gu.mediaservice.lib.{ImageWrapper, StorableImage}
import com.gu.mediaservice.lib.logging.{LogMarker, Stopwatch}
import com.gu.mediaservice.model.{FileMetadata, MimeType, Png, Tiff}
import com.gu.mediaservice.lib.logging.MarkerMap

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._
import scala.collection.JavaConverters._

trait OptimiseOps {
  def toOptimisedFile(file: File, imageWrapper: ImageWrapper, tempDir: File)
                     (implicit ec: ExecutionContext, logMarker: LogMarker): Future[(File, MimeType)]
  def isTransformedFilePath(filePath: String): Boolean
  def shouldOptimise(mimeType: Option[MimeType], fileMetadata: FileMetadata): Boolean
  def optimiseMimeType: MimeType
}

object OptimiseWithPngQuant extends OptimiseOps {

  override def optimiseMimeType: MimeType = Png

  def toOptimisedFile(file: File, imageWrapper: ImageWrapper, tempDir: File)
                     (implicit ec: ExecutionContext, logMarker: LogMarker): Future[(File, MimeType)] = Future {

    val optimisedFilePath = tempDir.getAbsolutePath + "/optimisedpng - " + imageWrapper.id + optimiseMimeType.fileExtension
    val marker = MarkerMap(
      "fileName" -> file.getName()
    )

    Stopwatch("pngquant") {
      val result = Seq("pngquant", "--quality", "1-85", file.getAbsolutePath, "--output", optimisedFilePath).!
      if (result>0)
        throw new Exception(s"pngquant failed to convert to optimised png file (rc = $result)")
    }(marker)

    val optimisedFile = new File(optimisedFilePath)
    if (optimisedFile.exists()) {
      (optimisedFile, optimiseMimeType)
    } else {
      throw new Exception(s"Attempted to optimise PNG file ${optimisedFile.getPath}")
    }
  }

  def isTransformedFilePath(filePath: String): Boolean = filePath.contains("transformed-")

  def shouldOptimise(mimeType: Option[MimeType], fileMetadata: FileMetadata): Boolean =
    mimeType match {
      case Some(Png) =>
        fileMetadata.colourModelInformation.get("colorType") match {
          case Some("True Color") => true
          case Some("True Color with Alpha") => true
          case _ => false
        }
      // imagemagick seems to populate the png metadata when we bring in tiffs
      // otherwise ExtraSamples on the tifftags is what we want*
      // *it is needless to say this is not available here.
      case Some(Tiff) => fileMetadata.colourModelInformation.get("colorType") match {
        case Some("True Color") => false
        case Some("True Color with Alpha") => true
        case _ => false
      }
      case _ => false
    }
}
