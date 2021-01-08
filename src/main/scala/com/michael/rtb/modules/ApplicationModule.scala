package com.michael.rtb.modules

import com.michael.rtb.dao._
import com.michael.rtb.services.BiddingAgentActor
import com.michael.rtb.services.impl._

trait ApplicationModule {

  import com.softwaremill.macwire._

  lazy val database: Database = wire[Database]

  lazy val statisticsDao: StatisticsDao = wire[StatisticsDao]

  lazy val campaignsDao: CampaignsDao = wire[CampaignsDao]

  lazy val auctionService: DefaultAuctionService = wire[DefaultAuctionService]

  lazy val validationService: DefaultValidationService = wire[DefaultValidationService]

  lazy val statisticsService: DefaultStatisticsService = wire[DefaultStatisticsService]

  lazy val biddingAgent: BiddingAgentActor = wire[BiddingAgentActor]

}
