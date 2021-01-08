package com.michael.rtb.dao

import com.michael.rtb.domain._

trait CampaignsStorage {

  def getCampaigns: List[Campaign]

}
