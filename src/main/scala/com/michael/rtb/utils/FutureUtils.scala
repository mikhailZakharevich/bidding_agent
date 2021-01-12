package com.michael.rtb.utils

import com.michael.rtb.database.DatabaseProvider.DbTask

import scala.concurrent.Future

trait FutureUtils {

  implicit class DbTaskToFuture[A](task: DbTask[A]) {

    import monix.execution.Scheduler.Implicits.global

    def toFuture: Future[A] = task.runToFuture
  }

}
