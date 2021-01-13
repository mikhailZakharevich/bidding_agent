package com.michael.rtb.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.michael.rtb.ApplicationMain.auctionService.Price
import com.michael.rtb.actors.BiddingAgentActor._
import com.michael.rtb.domain._
import com.michael.rtb.repository.CampaignsRepository
import com.michael.rtb.services.{AuctionService, StatisticsService, ValidationService}
import com.michael.rtb.utils.AppUtils._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class BidRequest(id: String, imp: Option[List[Impression]], site: Site, user: Option[User], device: Option[Device])

sealed trait BiddingAgentResponse

case class BidResponse(id: String, bidRequestId: String, price: Double, adId: Option[String], banner: Option[Banner]) extends BiddingAgentResponse

case class BidErrorResponse(message: String) extends BiddingAgentResponse

case object BidEmptyResponse extends BiddingAgentResponse

class BiddingAgentActor(statisticsService: StatisticsService, campaignsStorage: CampaignsRepository, auctionService: AuctionService, validationService: ValidationService) extends LazyLogging {

  private val onFailureStrategy = SupervisorStrategy.restartWithBackoff(
    minBackoff = 200.millis, maxBackoff = 10.seconds, randomFactor = 0.1)

  def start: Behavior[Command] =
    Behaviors.supervise {
      Behaviors.receive[Command] { case (context, command) => command match {
        case BiddingAgentRequest(br, replyTo) =>
          val tags = br.imp.getOrElse(Nil).map(_.tagId)

          context.pipeToSelf(createBid(br, tags)) {
            case Success(Some((campaign, price))) => NonEmptyBidResponse(BidResponse(uuid, br.id, price, Some(campaign.id.toString), campaign.banners.headOption), replyTo)
            case Success(None) => EmptyBidResponse(br, replyTo)
            case Failure(err) => ErrorBidResponse(err, replyTo)
          }
          Behaviors.same

        case NonEmptyBidResponse(bidResponse, replyTo) =>
          context.log.debug(s"successful response: $bidResponse")
          replyTo ! bidResponse
          Behaviors.same

        case EmptyBidResponse(br, replyTo) =>
          context.log.debug(s"empty response for bid: $br")
          replyTo ! BidEmptyResponse
          Behaviors.same

        case ErrorBidResponse(err, replyTo) =>
          context.log.error(s"bidding failed: ${err.getMessage}", err)
          replyTo ! BidErrorResponse(err.getMessage)
          Behaviors.same
      }}
    }.onFailure(onFailureStrategy)

  private[actors] def createBid(br: BidRequest, tags: List[String]): Future[Option[(Campaign, Price)]] =
    (for {
      site <- statisticsService.getOrInsert(br.site, tags)
      _ = logger.debug(s"agent -> site found [${site.id}]")

      segmentIds <- statisticsService.getSegmentIdsBySiteId(site.id)
      _ = logger.debug(s"agent -> segments found [${segmentIds.mkString(",")}]")

      campaigns = validationService.getMatchingCampaigns(site, br.imp, segmentIds, br.user, br.device, campaignsStorage.getCampaigns)
      _ = logger.debug(s"agent -> campaigns found [${campaigns.mkString(",")}]")

      result = auctionService.startAuction(campaigns)
    } yield result).toFuture

}

object BiddingAgentActor {

  sealed trait Command

  case class BiddingAgentRequest(bidRequest: BidRequest, replyTo: ActorRef[BiddingAgentResponse]) extends Command

  case class NonEmptyBidResponse(bidResponse: BidResponse, replyTo: ActorRef[BiddingAgentResponse]) extends Command

  case class EmptyBidResponse(bidRequest: BidRequest, replyTo: ActorRef[BiddingAgentResponse]) extends Command

  case class ErrorBidResponse(error: Throwable, replyTo: ActorRef[BiddingAgentResponse]) extends Command

}
