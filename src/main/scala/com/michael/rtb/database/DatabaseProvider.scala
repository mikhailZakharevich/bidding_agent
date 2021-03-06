package com.michael.rtb.database

import com.michael.rtb.database.DatabaseProvider._
import io.getquill.NamingStrategy
import io.getquill.context.monix.MonixJdbcContext
import io.getquill.context.sql.idiom.SqlIdiom
import monix.eval.Task

/** quill jdbc provider */
trait DatabaseProvider[D <: SqlIdiom, N <: NamingStrategy] {

  type DbContext = MonixJdbcContext[D, N]

  def ctx: DbContext

  /** helper method for running transactions */
  def runT[T](block: DbContext => DbTask[T]): DbTask[T] =
    ctx.transaction(block(ctx))
}

object DatabaseProvider {
  type DbTask[T] = Task[T]
}
