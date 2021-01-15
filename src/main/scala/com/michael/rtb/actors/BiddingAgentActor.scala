package com.michael.rtb.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import com.michael.rtb.actors.BiddingAgentActor._
import com.michael.rtb.domain._
import com.michael.rtb.repository.CampaignsRepository
import com.michael.rtb.services.AuctionService.AuctionResult
import com.michael.rtb.services.{AuctionService, StatisticsService, ValidationService}
import com.michael.rtb.utils.AppUtils._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import monix.eval.Task

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class BidRequest(id: String,
                      imp: Option[List[Impression]],
                      site: Site,
                      user: Option[User],
                      device: Option[Device])

sealed trait BiddingAgentResponse

case class BidResponse(id: String,
                       bidRequestId: String,
                       price: Double,
                       adId: Option[String],
                       banner: Option[Banner]) extends BiddingAgentResponse

case class BidErrorResponse(message: String) extends BiddingAgentResponse

case object BidEmptyResponse extends BiddingAgentResponse

class BiddingAgentActor(statisticsService: StatisticsService,
                        campaignsStorage: CampaignsRepository,
                        auctionService: AuctionService,
                        validationService: ValidationService) extends LazyLogging {

  private val conf: Config = ConfigFactory.load()

  private val cache: Cache[Int, (Site, List[Int])] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(conf.getInt("cache.ttl").hour)
      .maximumSize(conf.getInt("cache.max-capacity"))
      .build[Int, (Site, List[Int])]

  private val onFailureStrategy = SupervisorStrategy.restartWithBackoff(
    minBackoff = 200.millis, maxBackoff = 10.seconds, randomFactor = 0.1)

  def start: Behavior[Command] =
    Behaviors.supervise {
      Behaviors.receive[Command] { case (context, command) => command match {
        case BiddingAgentRequest(br, replyTo) =>
          val tags = br.imp.getOrElse(Nil).map(_.tagId)

          context.pipeToSelf(createBid(br, tags)) {
            case Success(Some(AuctionResult(campaign, price))) =>
              NonEmptyBidResponse(
                BidResponse(uuid, br.id, price, Some(campaign.id.toString), campaign.banners.headOption),
                replyTo
              )
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
      }
      }
    }.onFailure(onFailureStrategy)

  private[actors] def createBid(br: BidRequest, tags: List[String]): Future[Option[AuctionResult]] =
    (for {
      siteAndSegments <- getCacheOrDbValue(br.site, tags)

      (site, segmentIds) = siteAndSegments

      campaigns = validationService
        .getMatchingCampaigns(site, br.imp, segmentIds, br.user, br.device, campaignsStorage.getCampaigns)
      _ = logger.debug(s"agent -> campaigns found [${campaigns.mkString(",")}]")

      result = auctionService.findWinnerCampaign(campaigns)
    } yield result).toFuture

  private[actors] def getCacheOrDbValue(site: Site, tags: => List[String]): Task[(Site, List[Int])] =
    cache.getIfPresent(site.id) match {
      case Some((site, segments)) =>
        Task.pure((site, segments))
      case None =>
        for {
          s <- statisticsService.getOrInsert(site, tags)
          _ = logger.debug(s"agent -> site found [${s.id}]")

          segmentIds <- statisticsService.getSegmentIdsBySiteId(site.id)
          _ = logger.debug(s"agent -> segments found [${segmentIds.mkString(",")}]")

          _ = cache.put(site.id, (s, segmentIds))
        } yield (s, segmentIds)
    }

}

object BiddingAgentActor {

  sealed trait Command

  case class BiddingAgentRequest(bidRequest: BidRequest, replyTo: ActorRef[BiddingAgentResponse]) extends Command

  case class NonEmptyBidResponse(bidResponse: BidResponse, replyTo: ActorRef[BiddingAgentResponse]) extends Command

  case class EmptyBidResponse(bidRequest: BidRequest, replyTo: ActorRef[BiddingAgentResponse]) extends Command

  case class ErrorBidResponse(error: Throwable, replyTo: ActorRef[BiddingAgentResponse]) extends Command

}
