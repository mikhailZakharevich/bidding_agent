package com.michael.rtb

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.michael.rtb.modules.ApplicationModule
import com.michael.rtb.routes.BiddingAgentRoutes
import com.michael.rtb.actors.BiddingAgentActor.Command
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object ApplicationMain extends ApplicationModule {

  lazy val cfg: Config = ConfigFactory.load()
  lazy val host: String = cfg.getString("app.host")
  lazy val port: Int = cfg.getInt("app.port")

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding: Future[Http.ServerBinding] = Http().newServerAt(host, port).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
        sys.addShutdownHook {
          system.log.info("Shutting down the server...")
          binding.unbind().map(_ => system.terminate())
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }

  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      implicit val ex: ExecutionContextExecutor = context.system.executionContext

      val biddingAgentActor: ActorRef[Command] = context.spawn(biddingAgent.start, "BiddingAgentActor")
      context.watch(biddingAgentActor)

      val routes = new BiddingAgentRoutes(biddingAgentActor, statisticsService, campaignsProvider)(context.system)
      startHttpServer(routes.agentRoutes)(context.system)

      Behaviors.empty
    }

    val _ = ActorSystem[Nothing](rootBehavior, "BiddingServer")
  }
}
