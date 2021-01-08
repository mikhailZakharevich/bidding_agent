package com.michael.rtb.modules

import com.michael.rtb.dao._
import com.michael.rtb.mocks.{TestCampaignsStorage, TestStatisticsService}
import com.michael.rtb.services.BiddingAgentActor
import com.michael.rtb.services.impl._

trait TestApplicationModule {

  import com.softwaremill.macwire._

  lazy val campaignsDao: TestCampaignsStorage = wire[TestCampaignsStorage]

  lazy val auctionService: DefaultAuctionService = wire[DefaultAuctionService]

  lazy val validationService: DefaultValidationService = wire[DefaultValidationService]

  lazy val statisticsService: TestStatisticsService = wire[TestStatisticsService]

  lazy val biddingAgent: BiddingAgentActor = wire[BiddingAgentActor]

}
