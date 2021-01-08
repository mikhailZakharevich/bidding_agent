package com.michael.rtb.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.michael.rtb.domain.{Campaign, Site}
import com.michael.rtb.services.{BidRequest, BidResponse, BiddingAgentActor, BiddingAgentResponse, EmptyResponse}
import com.michael.rtb.services.BiddingAgentActor.{BidRequestCommand, GetCampaignsCommand, GetSitesCommand}
import com.typesafe.scalalogging.LazyLogging
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter

import scala.concurrent.Future
import scala.util.control.NonFatal

case class ErrorResponse(message: String)

class BiddingAgentRoutes(biddingAgentActor: ActorRef[BiddingAgentActor.Command])(implicit val system: ActorSystem[_]) extends LazyLogging {

  import Endpoints._
  private implicit val ex = system.executionContext

  private implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getSites: Future[Either[ErrorResponse, List[Site]]] =
    biddingAgentActor.ask(GetSitesCommand).map(c => Right(c.items)).recover {
      case NonFatal(e) =>
        logger.error(s"failed to fetch sites: [${e.getMessage}]", e)
        Left(ErrorResponse(e.getMessage))
    }

  def getCampaigns: Future[Either[ErrorResponse, List[Campaign]]] =
    biddingAgentActor.ask(GetCampaignsCommand).map(c => Right(c.items)).recover {
      case NonFatal(e) =>
        logger.error(s"failed to fetch campaigns: [${e.getMessage}]", e)
        Left(ErrorResponse(e.getMessage))
    }

  def createBidRequest(bidRequest: BidRequest): Future[Either[ErrorResponse, BiddingAgentResponse]] =
    biddingAgentActor.ask(BidRequestCommand(bidRequest, _)).map(resp => Right(resp)).recover {
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
          statusMapping(StatusCode.NoContent, emptyOutput.map(_ => EmptyResponse)(_ => ()))
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

    import akka.http.scaladsl.server.Directives._

    concat(
      AkkaHttpServerInterpreter.toRoute(createBidRequestEndpoint),
      AkkaHttpServerInterpreter.toRoute(campaignsListEndpoint),
      AkkaHttpServerInterpreter.toRoute(sitesListEndpoint)
    )

  }

}


