package com.michael.rtb.dao

import com.michael.rtb.dao.Database._
import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.context.monix.Runner
import io.getquill.{MysqlMonixJdbcContext, SnakeCase}
import monix.eval.Task
import monix.execution.Scheduler

class Database() {

  lazy val ctx = new MysqlMonixJdbcContext(SnakeCase, config, Runner.using(Scheduler.io()))

  def runT[T](block: DbContext => DbTask[T]): DbTask[T] =
    ctx.transaction(block(ctx))

}

object Database {
  type DbContext = MysqlMonixJdbcContext[SnakeCase.type]
  type DbTask[T] = Task[T]

  val config: Config = ConfigFactory.load().getObject("db.main").toConfig
}
