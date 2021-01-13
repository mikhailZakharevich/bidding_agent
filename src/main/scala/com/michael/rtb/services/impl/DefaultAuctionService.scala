package com.michael.rtb.services.impl

import com.michael.rtb.domain.Campaign
import com.michael.rtb.services.AuctionService

class DefaultAuctionService extends AuctionService {

  val WinningCoefficient: Price = 0.7

  /** start auction for eligible campaigns */
  override def startAuction(campaigns: List[Campaign]): Option[(Campaign, Price)] =
    campaigns.sortWith(_.bid > _.bid).headOption.map(c => (c, c.bid * WinningCoefficient))

}
