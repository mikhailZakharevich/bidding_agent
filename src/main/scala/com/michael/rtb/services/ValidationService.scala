package com.michael.rtb.services

import com.michael.rtb.domain._

trait ValidationService {

  def validateSegments(campaign: Campaign, segmentIds: Set[Int]): Boolean

  def validateUser(campaign: Campaign, user: Option[User], device: Option[Device]): Boolean

  def validateBids(campaign: Campaign, impressions: List[Impression]): Boolean

  def validateSite(campaign: Campaign, site: Site): Boolean

  def filterBanners(campaign: Campaign, banners: List[Impression]): List[Banner]

  def getMatchingCampaigns(site: Site, imp: Option[List[Impression]], segmentIds: List[Int], user: Option[User], device: Option[Device], campaigns: List[Campaign]): List[Campaign] =
    campaigns
      .filter(campaign => validateUser(campaign, user, device))
      .filter(campaign => filterBanners(campaign, imp.getOrElse(Nil)).nonEmpty)
      .filter(campaign => validateBids(campaign, imp.getOrElse(Nil)))
      .filter(campaign => validateSite(campaign, site))
      .filter(campaign => validateSegments(campaign, segmentIds.toSet))

}
