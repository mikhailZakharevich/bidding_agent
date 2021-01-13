package com.michael.rtb.database

import com.typesafe.config.Config
import io.getquill.context.monix.Runner
import io.getquill.{MySQLDialect, MysqlMonixJdbcContext, SnakeCase}
import monix.execution.Scheduler

trait MysqlDatabaseProvider extends DatabaseProvider[MySQLDialect, SnakeCase.type] {

  val config: Config

  // use a dedicated thread pool for db interaction
  lazy val ctx = new MysqlMonixJdbcContext(SnakeCase, config, Runner.using(Scheduler.io()))

}
