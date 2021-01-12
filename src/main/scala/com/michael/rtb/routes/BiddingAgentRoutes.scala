package com.michael.rtb.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.michael.rtb.actors.BiddingAgentActor._
import com.michael.rtb.actors._
import com.michael.rtb.domain.{Campaign, Site}
import com.michael.rtb.repository.CampaignsRepository
import com.michael.rtb.services.StatisticsService
import com.michael.rtb.utils.AppUtils._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter

import scala.concurrent.Future
import scala.util.control.NonFatal

case class ErrorResponse(message: String)

class BiddingAgentRoutes(biddingAgentActor: ActorRef[BiddingAgentActor.Command],
                         statisticsService: StatisticsService,
                         campaignsProvider: CampaignsRepository)
                        (implicit val system: ActorSystem[_]) extends LazyLogging {

  import Endpoints._
  private implicit val ex = system.executionContext

  private implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getSites: Future[Either[ErrorResponse, List[Site]]] =
    statisticsService.getSites.map(Right(_)).toFuture.recover {
      case NonFatal(e) =>
        logger.error(s"failed to fetch sites: [${e.getMessage}]", e)
        Left(ErrorResponse(e.getMessage))
    }

  def getCampaigns: Future[Either[ErrorResponse, List[Campaign]]] =
    Future.successful(campaignsProvider.getCampaigns).map(Right(_))

  def createBidRequest(bidRequest: BidRequest): Future[Either[ErrorResponse, BiddingAgentResponse]] =
    biddingAgentActor.ask(BiddingAgentRequest(bidRequest, _)).map {
      case BidErrorResponse(msg) => Left(ErrorResponse(msg))
      case response => Right(response)
    }.recover {
      case NonFatal(e) =>
        logger.error(s"failed to create bid request: [${e.getMessage}]", e)
        Left(ErrorResponse(e.getMessage))
    }

  private object Endpoints {
    val createBidRequestEndpoint: ServerEndpoint[BidRequest, ErrorResponse, BiddingAgentResponse, Any, Future] =
      endpoint
        .post
        .in("api" / "v1" / "bid-request")
        .in(jsonBody[BidRequest])
        .errorOut(jsonBody[ErrorResponse])
        .out(oneOf[BiddingAgentResponse](
          statusMapping(StatusCode.Created, jsonBody[BidResponse]),
          statusMapping(StatusCode.NoContent, emptyOutput.map(_ => BidEmptyResponse)(_ => ()))
        ))
        .serverLogic(createBidRequest)

    val campaignsListEndpoint: ServerEndpoint[Unit, ErrorResponse, List[Campaign], Any, Future] =
      endpoint
        .get
        .in("api" / "v1" / "campaigns")
        .errorOut(jsonBody[ErrorResponse])
        .out(jsonBody[List[Campaign]])
        .serverLogic(_ => getCampaigns)

    val sitesListEndpoint: ServerEndpoint[Unit, ErrorResponse, List[Site], Any, Future] =
      endpoint
        .get
        .in("api" / "v1" / "sites")
        .errorOut(jsonBody[ErrorResponse])
        .out(jsonBody[List[Site]])
        .serverLogic(_ => getSites)

  }

  def agentRoutes: Route = {

    case class LogEntry(msg: String)

    import akka.http.scaladsl.server.Directives._

    concat(
      AkkaHttpServerInterpreter.toRoute(createBidRequestEndpoint),
      AkkaHttpServerInterpreter.toRoute(campaignsListEndpoint),
      AkkaHttpServerInterpreter.toRoute(sitesListEndpoint)
    )
  }

}


