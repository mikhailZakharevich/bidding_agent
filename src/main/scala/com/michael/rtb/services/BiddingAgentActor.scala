package com.michael.rtb.services

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import cats.implicits._
import com.michael.rtb.Utils._
import com.michael.rtb.dao.CampaignsStorage
import com.michael.rtb.domain.{Banner, Campaign, Device, Impression, Site, User}
import com.michael.rtb.services.BiddingAgentActor._
import com.typesafe.scalalogging.LazyLogging
import monix.eval._

import scala.concurrent.duration._

case class Campaigns(items: List[Campaign])
case class Sites(items: List[Site])
case class BidRequest(id: String, imp: Option[List[Impression]], site: Site, user: Option[User], device: Option[Device])

sealed trait BiddingAgentResponse
case class BidResponse(id: String, bidRequestId: String, price: Double, adId: Option[String], banner: Option[Banner]) extends BiddingAgentResponse
case object EmptyResponse extends BiddingAgentResponse

class BiddingAgentActor(statisticsService: StatisticsService, campaignsStorage: CampaignsStorage, auctionService: AuctionService, validationService: ValidationService) extends LazyLogging {

  private val onFailureStrategy = SupervisorStrategy.restartWithBackoff(
    minBackoff = 200.millis, maxBackoff = 10.seconds, randomFactor = 0.1)

  def start: Behavior[Command] =
    Behaviors.supervise {
      Behaviors.receiveMessage[Command] {
        case BidRequestCommand(br, replyTo) =>
          val tags = br.imp.getOrElse(Nil).map(_.tagId)

//          val tags = List("tag1", "tag2")
          (for {
            site <- statisticsService.getOrInsert(br.site, tags)
            _ = logger.debug(s"agent -> site found [${site.id}]")

            segmentIds <- statisticsService.getSegmentIdsBySiteId(site.id)
            _ = logger.debug(s"agent -> segments found [${segmentIds.mkString(",")}]")

            campaigns <- validationService.getMatchingCampaigns(site, br.imp, segmentIds, br.user, br.device, campaignsStorage.getCampaigns).traverse(Task.pure)
            _ = logger.debug(s"agent -> campaigns found [${campaigns.mkString(",")}]")

            _ = auctionService.start(campaigns).fold(replyTo ! EmptyResponse) { case (campaign, price) =>
              replyTo ! BidResponse(uuid, br.id, price, Some(campaign.id.toString), campaign.banners.headOption)
            }
          } yield ()).toFuture
          Behaviors.same

        case GetCampaignsCommand(replyTo) =>
          replyTo ! Campaigns(campaignsStorage.getCampaigns)
          Behaviors.same

        case GetSitesCommand(replyTo) =>
          statisticsService.getSites.map { sites =>
            replyTo ! Sites(sites)
          }.toFuture
          Behaviors.same
      }
    }.onFailure(onFailureStrategy)

}

object BiddingAgentActor {

  sealed trait Command

  case class GetSitesCommand(replyTo: ActorRef[Sites]) extends Command
  case class GetCampaignsCommand(replyTo: ActorRef[Campaigns]) extends Command
  case class BidRequestCommand(bidRequest: BidRequest, replyTo: ActorRef[BiddingAgentResponse]) extends Command

}
