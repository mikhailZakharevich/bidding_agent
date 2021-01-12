package com.michael.rtb.actors

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import com.michael.rtb.domain._
import com.michael.rtb.repository.CampaignsRepository
import com.michael.rtb.repository.impl.DefaultCampaignsRepository
import com.michael.rtb.services.impl.{DefaultAuctionService, DefaultStatisticsService, DefaultValidationService}
import com.michael.rtb.services.{AuctionService, StatisticsService, ValidationService}
import com.michael.rtb.utils.AppUtils._
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

class BiddingAgentActorSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with LazyLogging {

  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  lazy val campaignsStorage: CampaignsRepository = mock[DefaultCampaignsRepository]

  lazy val auctionService: AuctionService = mock[DefaultAuctionService]

  lazy val validationService: ValidationService = mock[DefaultValidationService]

  lazy val statisticsService: StatisticsService = mock[DefaultStatisticsService]

  lazy val biddingAgent: BiddingAgentActor = new BiddingAgentActor(statisticsService, campaignsStorage, auctionService, validationService)

  "BiddingAgentActor" should {

    "return bid request if auction yielded value" in {

      val domain = "https://random.com/1"

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
      val site = Site(1, domain)
      val impressions = Some(List(imp))
      val segmentIds = List(1)

      val banner = Banner(1, domain, 1, 1)
      val cmp = Campaign(1, "USA", Targeting(Set(), Set()), List(banner), 1.0)

      val bidRequest = BidRequest(uuid, impressions, site, Some(user), Some(device))

      when(statisticsService.getOrInsert(any[Site], any[List[String]])).thenReturn(Task.pure(site))
      when(statisticsService.getSegmentIdsBySiteId(any[Int]())).thenReturn(Task.pure(segmentIds))
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

      val expectedBehavior = Behaviors.receiveMessage[BiddingAgentActor.BiddingAgentRequest] { msg =>
        msg.replyTo ! BidResponse(uuid, bidRequest.id, 1.0, Some(cmp.id.toString), Some(banner))
        Behaviors.same
      }

      val probe = testKit.createTestProbe[BiddingAgentActor.BiddingAgentRequest]()
      val responseProbe = testKit.createTestProbe[BiddingAgentResponse]()
      val agent = testKit.spawn(Behaviors.monitor(probe.ref, expectedBehavior))

      biddingAgent.createBid(bidRequest, List(imp.tagId)).futureValue should be(Some((cmp, 1.0)))

      agent ! BiddingAgentActor.BiddingAgentRequest(bidRequest, responseProbe.ref)

      responseProbe.expectMessageType[BidResponse]

    }

    "return empty result if auction didn't find any winner" in {

      val domain = "https://random.com/1"

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
      val site = Site(1, domain)
      val impressions = Some(List(imp))
      val segmentIds = List(1)

      val banner = Banner(1, domain, 1, 1)
      val cmp = Campaign(1, "USA", Targeting(Set(), Set()), List(banner), 1.0)

      val bidRequest = BidRequest(uuid, impressions, site, Some(user), Some(device))

      when(statisticsService.getOrInsert(any[Site], any[List[String]])).thenReturn(Task.pure(site))
      when(statisticsService.getSegmentIdsBySiteId(any[Int]())).thenReturn(Task.pure(segmentIds))
      when(campaignsStorage.getCampaigns).thenReturn(List(cmp))
      when(validationService.getMatchingCampaigns(
        any[Site],
        any[Option[List[Impression]]],
        any[List[Int]],
        any[Option[User]],
        any[Option[Device]],
        any[List[Campaign]]
      )).thenReturn(List(cmp))
      when(auctionService.startAuction(any[List[Campaign]])).thenReturn(None)

      val expectedBehavior = Behaviors.receiveMessage[BiddingAgentActor.BiddingAgentRequest] { msg =>
        msg.replyTo ! BidEmptyResponse
        Behaviors.same
      }

      val probe = testKit.createTestProbe[BiddingAgentActor.BiddingAgentRequest]()
      val responseProbe = testKit.createTestProbe[BiddingAgentResponse]()
      val agent = testKit.spawn(Behaviors.monitor(probe.ref, expectedBehavior))

      biddingAgent.createBid(bidRequest, List(imp.tagId)).futureValue should be(None)

      agent ! BiddingAgentActor.BiddingAgentRequest(bidRequest, responseProbe.ref)

      responseProbe.expectMessageType[BidEmptyResponse.type]

    }

    "return error response if agent failed to create bid request" in {

      val error = new RuntimeException("something went wrong during bidding process")
      val domain = "https://random.com/1"

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
      val site = Site(1, domain)
      val impressions = Some(List(imp))
      val segmentIds = List(1)

      val banner = Banner(1, domain, 1, 1)
      val cmp = Campaign(1, "USA", Targeting(Set(), Set()), List(banner), 1.0)

      val bidRequest = BidRequest(uuid, impressions, site, Some(user), Some(device))

      when(statisticsService.getOrInsert(any[Site], any[List[String]])).thenReturn(Task.pure(site))
      when(statisticsService.getSegmentIdsBySiteId(any[Int]())).thenReturn(Task.pure(segmentIds))
      when(campaignsStorage.getCampaigns).thenReturn(List(cmp))
      when(validationService.getMatchingCampaigns(
        any[Site],
        any[Option[List[Impression]]],
        any[List[Int]],
        any[Option[User]],
        any[Option[Device]],
        any[List[Campaign]]
      )).thenReturn(List(cmp))
      when(auctionService.startAuction(any[List[Campaign]])).thenThrow(error)

      val expectedBehavior = Behaviors.receiveMessage[BiddingAgentActor.BiddingAgentRequest] { msg =>
        msg.replyTo ! BidErrorResponse(error.getMessage)
        Behaviors.same
      }

      val probe = testKit.createTestProbe[BiddingAgentActor.BiddingAgentRequest]()
      val responseProbe = testKit.createTestProbe[BiddingAgentResponse]()
      val agent = testKit.spawn(Behaviors.monitor(probe.ref, expectedBehavior))

      agent ! BiddingAgentActor.BiddingAgentRequest(bidRequest, responseProbe.ref)

      responseProbe.expectMessageType[BidErrorResponse]

    }
  }
}
