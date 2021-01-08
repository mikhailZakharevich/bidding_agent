package com.michael.rtb.mocks

import com.michael.rtb.dao.CampaignsStorage
import com.michael.rtb.domain._

class TestCampaignsStorage extends CampaignsStorage {
  override def getCampaigns: List[Campaign] = List(
    Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)
  )
}
