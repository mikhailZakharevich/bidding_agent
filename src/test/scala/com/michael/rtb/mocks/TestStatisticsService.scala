package com.michael.rtb.mocks

import com.michael.rtb.domain._
import com.michael.rtb.services.StatisticsService
import monix.eval.Task

class TestStatisticsService extends StatisticsService {

  override def getSites: Task[List[Site]] = Task.pure(List())

  override def getSiteById(siteId: Int): Task[Int] = Task.pure(1)

  override def getSegmentIdsBySiteId(siteId: Int): Task[List[Int]] = Task.pure(List(1))

  override def getOrInsert(site: Site, tags: List[String]): Task[Site] = Task.pure(site)

}
