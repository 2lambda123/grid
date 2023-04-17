package com.gu.mediaservice.lib.imaging.vips

import com.gu.mediaservice.model.Bounds

import java.io.File
import scala.util.Try


object Vips {
  // this should only be run once per process - please keep it inside a singleton `object`!
  LibVips.INSTANCE.vips_init("")

  //noinspection AccessorLikeMethodIsEmptyParen
  private def getErrors(): String = LibVips.INSTANCE.vips_error_buffer_copy()

  def openFile(file: File): Try[VipsImage] = Try {
    val image = LibVips.INSTANCE.vips_image_new_from_file(file.getAbsolutePath)
    if (image == null) {
      throw new Error(s"Failed to open image file ${file.getName} - libvips returned error(s) ${getErrors()}")
    }
    image
  }

  def extractArea(image: VipsImage, bounds: Bounds): Try[VipsImage] = Try {
    val cropOutput = new VipsImageByReference()
    if (LibVips.INSTANCE.vips_extract_area(image, cropOutput, bounds.x, bounds.y, bounds.width, bounds.height) != 0) {
      throw new Error(s"Failed to crop image - libvips return error(s) ${getErrors()}")
    }
    cropOutput.getValue
  }

  def resize(image: VipsImage, scale: Double): Try[VipsImage] = Try {
    val resizeOutput = new VipsImageByReference()
    if (LibVips.INSTANCE.vips_resize(image, resizeOutput, scale) != 0) {
      throw new Error(s"Failed to resize image - libvips returned error(s) ${getErrors()}")
    }
    resizeOutput.getValue
  }

  def saveJpeg(image: VipsImage, outputFile: File, quality: Int, profile: Option[String]): Try[Unit] = Try {
    val args = Seq("Q", quality.asInstanceOf[Integer], "strip", 1.asInstanceOf[Integer]) ++ profile.map(Seq("profile", _)).getOrElse(Seq.empty)

    if (LibVips.INSTANCE.vips_jpegsave(image, outputFile.getAbsolutePath, args:_*) != 0) {
      throw new Error(s"Failed to save file to Jpeg - libvips returned error ${getErrors()}")
    }
  }

  def savePng(image: VipsImage, outputFile: File, quality: Int, profile: Option[String]): Try[Unit] = Try {
    val args = Seq("Q", quality.asInstanceOf[Integer], "strip", 1.asInstanceOf[Integer]) ++ profile.map(Seq("profile", _)).getOrElse(Seq.empty)

    if (LibVips.INSTANCE.vips_pngsave(image, outputFile.getAbsolutePath, args:_*) != 0) {
      throw new Error(s"Failed to save file to Png - libvips returned error ${getErrors()}")
    }
  }
}
