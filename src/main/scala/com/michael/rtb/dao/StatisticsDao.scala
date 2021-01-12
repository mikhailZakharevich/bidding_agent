package com.michael.rtb.dao

import com.michael.rtb.database.DatabaseProvider.DbTask
import com.michael.rtb.dao.StatisticsDao._
import com.michael.rtb.database.MysqlDatabaseProvider
import com.michael.rtb.domain.Site
import com.michael.rtb.errors.ApiError._
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task

class StatisticsDao(db: MysqlDatabaseProvider) extends LazyLogging {

  def getSites: DbTask[List[Site]] = db.runT { implicit ctx =>
    import ctx._
    run(quote(query[Sites])).map(result => result.map(s => Site(s.id, s.domain)))
  }

  def getSiteById(siteId: Int): DbTask[Site] = db.runT { implicit ctx =>
    import ctx._
    run(quote(query[Sites].filter(_.id == lift(siteId))))
      .map(_.headOption.map(s => Site(s.id, s.domain))).flatMap {
      case Some(site) => Task.pure(site)
      case None => Task.raiseError(NotFoundError(s"there is no site with id $siteId"))
    }
  }

  def getSiteByExchangeSiteId(exchangeSiteId: Int): DbTask[Site] = db.runT { implicit ctx =>
    import ctx._
    run(quote(query[Sites].filter(_.exchangeSiteId == lift(exchangeSiteId))))
      .map(_.headOption.map(s => Site(s.id, s.domain))).flatMap {
      case Some(site) => Task.pure(site)
      case None => Task.raiseError(NotFoundError(s"there is no site with exchange site id $exchangeSiteId"))
    }
  }

  def getSegmentIdsBySiteId(siteId: Int): DbTask[List[Int]] = db.runT { implicit ctx =>
    import ctx._
    run(quote(query[Segments].filter(_.siteId == lift(siteId)))).map(_.map(_.siteId))
  }

  def save(site: Site, tags: List[String]): DbTask[Int] = db.runT { implicit ctx =>
    for {
      id     <- insertSite(site)
      plcIds <- insertPlacements(id, tags)
      _      <- insertSegments(id, plcIds)
    } yield id

  }

  def insertSite(site: Site): DbTask[Int] = db.runT { implicit ctx =>
    import ctx._
    run(
      quote(query[Sites].insert(
        _.domain -> lift(site.domain),
        _.exchangeSiteId -> lift(site.id),
        _.exchangeId -> lift(ExchangeId)
      ).onConflictIgnore.returningGenerated(_.id)))
  }

  def insertPlacements(siteId: Int, tags: List[String]): DbTask[List[Int]] = db.runT { implicit ctx =>
    import ctx._
    run(
      quote(
        liftQuery(tags).foreach(tag => query[SitesPlacements]
          .insert(
            _.siteId -> lift(siteId),
            _.tagId -> tag
          ).onConflictIgnore.returningGenerated(_.id)
        )
      )
    )
  }

  def insertSegments(siteId: Int, placementIds: List[Int]): DbTask[List[Int]] = db.runT { implicit ctx =>
    import ctx._
    run(
      quote(
        liftQuery(placementIds).foreach(plcId => query[Segments]
          .insert(
            _.siteId -> lift(siteId),
            _.placementId -> plcId
          ).onConflictIgnore.returningGenerated(_.id)
        )
      )
    )
  }

}

object StatisticsDao {

  val ExchangeId: Int = 1

  case class Sites(id: Int, exchangeId: Int, exchangeSiteId: Int, domain: String)

  case class SitesPlacements(id: Int, siteId: Int, tagId: String)

  case class Segments(id: Int, siteId: Int, placementId: Int)

}
