package com.michael.rtb

import java.util.UUID

import com.michael.rtb.dao.Database.DbTask

import scala.concurrent.Future

object Utils {

  def uuid: String = UUID.randomUUID().toString.replaceAll("-", "")

  implicit class DbTaskToFuture[A](task: DbTask[A]) {

    import monix.execution.Scheduler.Implicits.global

    def toFuture: Future[A] = task.runToFuture
  }

}
