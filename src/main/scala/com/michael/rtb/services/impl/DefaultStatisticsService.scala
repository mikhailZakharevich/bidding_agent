package com.michael.rtb.services.impl

import com.michael.rtb.dao.StatisticsDao
import com.michael.rtb.domain.Site
import com.michael.rtb.errors.ApiError.NotFoundError
import com.michael.rtb.services.StatisticsService
import monix.eval.Task

class DefaultStatisticsService(statisticsDao: StatisticsDao) extends StatisticsService {

  override def getSites: Task[List[Site]] = statisticsDao.getSites

  override def getSiteById(siteId: Int): Task[Int] =
    statisticsDao.getSiteById(siteId).map(_.id)

  override def getSegmentIdsBySiteId(siteId: Int): Task[List[Int]] =
    statisticsDao.getSegmentIdsBySiteId(siteId)

  override def getOrInsert(site: Site, tags: List[String]): Task[Site] =
    statisticsDao.getSiteByExchangeSiteId(site.id).redeemWith ({
        case NotFoundError(_) =>
          for {
            id <- statisticsDao.save(site, tags)
            site <- statisticsDao.getSiteById(id)
          } yield site
      }, s => Task.pure(s))

}
