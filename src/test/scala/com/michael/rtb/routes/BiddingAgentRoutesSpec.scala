package com.michael.rtb.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.michael.rtb.actors.{BidRequest, BidResponse, BiddingAgentActor}
import com.michael.rtb.dao.StatisticsDao
import com.michael.rtb.domain._
import com.michael.rtb.repository.impl.DefaultCampaignsRepository
import com.michael.rtb.services.impl.{DefaultAuctionService, DefaultStatisticsService, DefaultValidationService}
import com.michael.rtb.utils.AppUtils._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce._
import io.circe.generic.auto._
import monix.eval.Task
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.model.headers.CacheDirectives.`no-cache`

class BiddingAgentRoutesSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with ScalatestRouteTest
  with BaseCirceSupport
  with FailFastCirceSupport
  with FailFastUnmarshaller
  with LazyLogging {

  lazy val campaignsStorage: DefaultCampaignsRepository = mock[DefaultCampaignsRepository]

  lazy val auctionService: DefaultAuctionService = mock[DefaultAuctionService]

  lazy val validationService: DefaultValidationService = mock[DefaultValidationService]

  lazy val statisticsDao: StatisticsDao = mock[StatisticsDao]

  lazy val statisticsService: DefaultStatisticsService = mock[DefaultStatisticsService]

  lazy val biddingAgent: BiddingAgentActor = new BiddingAgentActor(statisticsService, campaignsStorage, auctionService, validationService)

  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val agent = testKit.spawn(biddingAgent.start)
  lazy val routes = new BiddingAgentRoutes(agent, statisticsService, campaignsStorage).routes

  "BiddingAgentRoutes" should {

    "return empty body for bid request if `impressions` are empty" in {

      val domain = "https://random.com/1"
      val site = Site(1, domain)
      val geo = Geo(Some("USA"))
      val user = User(uuid, Some(geo))
      val device = Device(uuid, Some(geo))
      val emptyImpressions = Option.empty[List[Impression]]
      val cmp = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      when(statisticsService.getOrInsert(any[Site], any[List[String]])).thenReturn(Task.pure(site))
      when(statisticsService.getSegmentIdsBySiteId(any[Int]())).thenReturn(Task.pure(List(1)))
      when(campaignsStorage.getCampaigns).thenReturn(List(cmp))
      when(validationService.getMatchingCampaigns(
        any[Site],
        any[Option[List[Impression]]],
        any[List[Int]],
        any[Option[User]],
        any[Option[Device]],
        any[List[Campaign]]
      )).thenReturn(List.empty[Campaign]) // validation failed
      when(auctionService.startAuction(any[List[Campaign]])).thenReturn(None)

      val bidRequest = BidRequest(uuid, emptyImpressions, site, Some(user), Some(device))
      val bidRequestEntity: MessageEntity = Marshal(bidRequest).to[MessageEntity].futureValue

      val request = Post("/api/bid-request").withEntity(bidRequestEntity) ~> `Cache-Control`(`no-cache`)

      request ~> routes ~> check {
        status should ===(StatusCodes.NoContent)
      }
    }

    "return empty body for bid request if `user` and `device` data are empty" in {

      val domain = "https://random.com/1"
      val site = Site(1, domain)
      val imp = Impression(
        id = uuid,
        wMin = Some(1),
        wMax = Some(1),
        w = Some(1),
        hMin = Some(1),
        hMax = Some(1),
        h = Some(1),
        bidFloor = Some(1.0),
        tagId = uuid)

      val emptyUser = Option.empty[User]
      val emptyDevice = Option.empty[Device]
      val impressions = List(imp)
      val cmp = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      when(statisticsService.getOrInsert(any[Site], any[List[String]])).thenReturn(Task.pure(site))
      when(statisticsService.getSegmentIdsBySiteId(any[Int]())).thenReturn(Task.pure(List(1)))
      when(campaignsStorage.getCampaigns).thenReturn(List(cmp))
      when(validationService.getMatchingCampaigns(
        any[Site],
        any[Option[List[Impression]]],
        any[List[Int]],
        any[Option[User]],
        any[Option[Device]],
        any[List[Campaign]]
      )).thenReturn(List.empty[Campaign]) // validation failed
      when(auctionService.startAuction(any[List[Campaign]])).thenReturn(None)

      val bidRequest = BidRequest(uuid, Some(impressions), Site(1, domain), emptyUser, emptyDevice)
      val bidRequestEntity: MessageEntity = Marshal(bidRequest).to[MessageEntity].futureValue

      val request = Post("/api/bid-request").withEntity(bidRequestEntity) ~> `Cache-Control`(`no-cache`)

      request ~> routes ~> check {
        status should ===(StatusCodes.NoContent)
      }
    }

    "be able to create bids with impressions and at least user or device data present" in {

      val domain = "https://random.com/1"
      val site = Site(1, domain)
      val imp = Impression(
        id = uuid,
        wMin = Some(1),
        wMax = Some(1),
        w = Some(1),
        hMin = Some(1),
        hMax = Some(1),
        h = Some(1),
        bidFloor = Some(1.0),
        tagId = uuid)

      val geo = Geo(Some("USA"))
      val user = User(uuid, Some(geo))
      val device = Device(uuid, Some(geo))
      val impressions = List(imp)
      val cmp = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      when(statisticsService.getOrInsert(any[Site], any[List[String]])).thenReturn(Task.pure(site))
      when(statisticsService.getSegmentIdsBySiteId(any[Int]())).thenReturn(Task.pure(List(1)))
      when(campaignsStorage.getCampaigns).thenReturn(List(cmp))
      when(validationService.getMatchingCampaigns(
        any[Site],
        any[Option[List[Impression]]],
        any[List[Int]],
        any[Option[User]],
        any[Option[Device]],
        any[List[Campaign]]
      )).thenReturn(List(cmp))
      when(auctionService.startAuction(any[List[Campaign]])).thenReturn(Some((cmp, 1.0)))

      val bidRequest = BidRequest(uuid, Some(impressions), site, Some(user), Some(device))
      val bidRequestEntity: MessageEntity = Marshal(bidRequest).to[MessageEntity].futureValue

      val request = Post("/api/bid-request").withEntity(bidRequestEntity) ~> `Cache-Control`(`no-cache`)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        contentType should ===(ContentTypes.`application/json`)

        val responseEntity = entityAs[BidResponse]
        responseEntity.banner should ===(Some(Banner(1, domain, 1, 1)))
        responseEntity.adId should ===(Some(1.toString))
      }
    }

    "be able to fetch all campaigns" in {

      val request = Get("/api/campaigns")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[List[Campaign]].nonEmpty should be(true)
      }
    }

    "be able to fetch all sites" in {

      val domain = "https://random.com/1"
      val site = Site(1, domain)

      when(statisticsService.getSites).thenReturn(Task.pure(List(site)))

      val request = Get("/api/sites")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[List[Site]] should contain(site)
      }
    }
  }
}
