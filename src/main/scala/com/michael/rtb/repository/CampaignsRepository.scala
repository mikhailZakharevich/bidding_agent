package com.michael.rtb.repository

import com.michael.rtb.domain.Campaign

trait CampaignsRepository {

  def getCampaigns: List[Campaign]

}
