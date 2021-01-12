package com.michael.rtb.services

import com.michael.rtb.domain._

trait ValidationService {

  protected[services] def validateSegments(campaign: Campaign, segmentIds: Set[Int]): Boolean

  protected[services] def validateUser(campaign: Campaign, user: Option[User], device: Option[Device]): Boolean

  protected[services] def validateBids(campaign: Campaign, impressions: List[Impression]): Boolean

  protected[services] def validateSite(campaign: Campaign, site: Site): Boolean

  protected[services] def validateBanners(campaign: Campaign, banners: List[Impression]): Boolean

  def getMatchingCampaigns(site: Site, imp: Option[List[Impression]], segmentIds: List[Int], user: Option[User], device: Option[Device], campaigns: List[Campaign]): List[Campaign] =
    campaigns
      .filter(campaign => validateUser(campaign, user, device))
      .filter(campaign => validateBanners(campaign, imp.getOrElse(Nil)))
      .filter(campaign => validateBids(campaign, imp.getOrElse(Nil)))
      .filter(campaign => validateSite(campaign, site))
      .filter(campaign => validateSegments(campaign, segmentIds.toSet))

}
