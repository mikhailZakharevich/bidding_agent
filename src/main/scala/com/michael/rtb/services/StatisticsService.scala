package com.michael.rtb.services

import com.michael.rtb.domain.Site
import monix.eval.Task

trait StatisticsService {

  def getSites: Task[List[Site]]

  def getSiteById(siteId: Int): Task[Int]

  def getSegmentIdsBySiteId(siteId: Int): Task[List[Int]]

  def getOrInsert(site: Site, tags: List[String]): Task[Site]

}
