package com.michael.rtb

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.michael.rtb.domain._
import com.michael.rtb.modules.TestApplicationModule
import com.michael.rtb.routes.BiddingAgentRoutes
import com.michael.rtb.services.{BidRequest, BidResponse}
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce._
import io.circe.generic.auto._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BiddingAgentRoutesSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with ScalatestRouteTest
  with BaseCirceSupport
  with FailFastCirceSupport
  with FailFastUnmarshaller
  with LazyLogging
  with TestApplicationModule {

  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Here we need to implement all the abstract members of UserRoutes.
  // We use the real UserRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  val agent = testKit.spawn(biddingAgent.start)
  lazy val routes = new BiddingAgentRoutes(agent).agentRoutes

  def uuid: String = UUID.randomUUID().toString

  "BiddingAgentRoutes" should {

    "return empty body if `impressions` are empty (POST /api/v1/bid-request)" in {

      val domain = "https://random.com/1"

      val geo = Geo(Some("USA"))
      val user = User(uuid, Some(geo))
      val device = Device(uuid, Some(geo))
      val emptyImpressions = Option.empty[List[Impression]]

      val bidRequest = BidRequest(uuid, emptyImpressions, Site(1, domain), Some(user), Some(device))
      val bidRequestEntity: MessageEntity = Marshal(bidRequest).to[MessageEntity].futureValue

      val request = Post("/api/v1/bid-request").withEntity(bidRequestEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.NoContent)
      }
    }

    "return empty body if `user` and `device` data are empty (POST /api/v1/bid-request)" in {

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

      val emptyUser = Option.empty[User]
      val emptyDevice = Option.empty[Device]
      val impressions = List(imp)

      val bidRequest = BidRequest(uuid, Some(impressions), Site(1, domain), emptyUser, emptyDevice)
      val bidRequestEntity: MessageEntity = Marshal(bidRequest).to[MessageEntity].futureValue

      val request = Post("/api/v1/bid-request").withEntity(bidRequestEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.NoContent)
      }
    }

    "be able to create bids with impressions and at least user or device data present (POST /bid-request)" in {
      import io.circe.generic.auto._

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
      val impressions = List(imp)

      val bidRequest = BidRequest(uuid, Some(impressions), Site(1, domain), Some(user), Some(device))
      val bidRequestEntity: MessageEntity = Marshal(bidRequest).to[MessageEntity].futureValue

      val request = Post("/api/v1/bid-request").withEntity(bidRequestEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        contentType should ===(ContentTypes.`application/json`)

        val responseEntity = entityAs[BidResponse]
        responseEntity.banner should ===(Some(Banner(1, domain, 1, 1)))
        responseEntity.adId should ===(Some(1.toString))
      }
    }

    "be able to fetch all campaigns" in {
      import io.circe.generic.auto._

      val campaign = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val request = Get("/api/v1/campaigns")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[List[Campaign]] should contain(campaign)
      }
    }
  }
}
