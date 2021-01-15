package com.michael.rtb.services

import com.michael.rtb.domain.Campaign
import com.michael.rtb.services.AuctionService.AuctionResult

trait AuctionService {

  def findWinnerCampaign(campaigns: List[Campaign]): Option[AuctionResult]

}

object AuctionService {

  case class AuctionResult(campaign: Campaign, price: Double)

}
