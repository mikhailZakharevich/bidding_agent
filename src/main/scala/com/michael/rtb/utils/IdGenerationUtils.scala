package com.michael.rtb.utils

import java.util.UUID

trait IdGenerationUtils {

  def uuid: String = UUID.randomUUID().toString.replaceAll("-", "")

}
