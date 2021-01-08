package com.michael.rtb.services

import com.michael.rtb.domain.Campaign

trait AuctionService {

  type Price = Double

  def start(campaigns: List[Campaign]): Option[(Campaign, Price)]

}
