package com.michael.rtb.services.impl

import com.michael.rtb.domain.Campaign
import com.michael.rtb.services.AuctionService
import com.michael.rtb.services.AuctionService.AuctionResult

class DefaultAuctionService extends AuctionService {

  val WinningCoefficient: Double = 0.7

  /** start auction for eligible campaigns */
  override def findWinnerCampaign(campaigns: List[Campaign]): Option[AuctionResult] =
    campaigns.sortWith(_.bid > _.bid).headOption.map(c => AuctionResult(c, c.bid * WinningCoefficient))

}
