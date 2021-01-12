package com.michael.rtb.database

import com.typesafe.config.{Config, ConfigFactory}
import io.getquill.context.monix.Runner
import io.getquill.{MysqlMonixJdbcContext, SnakeCase}
import monix.execution.Scheduler

class Database() extends MysqlDatabaseProvider {
  override val config: Config = ConfigFactory.load().getObject("db.main").toConfig
}
