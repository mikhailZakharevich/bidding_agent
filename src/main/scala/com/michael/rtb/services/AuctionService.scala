package com.michael.rtb.services

import com.michael.rtb.domain.Campaign

trait AuctionService {

  type Price = Double

  def startAuction(campaigns: List[Campaign]): Option[(Campaign, Price)]

}
