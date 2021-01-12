package com.michael.rtb.services.impl

import com.michael.rtb.domain._
import com.michael.rtb.services.ValidationService
import com.typesafe.scalalogging.LazyLogging

class DefaultValidationService extends ValidationService with LazyLogging {

  override def validateSegments(campaign: Campaign, segmentIds: Set[Int]): Boolean =
    campaign.targeting.targetedSegmentIds.isEmpty || campaign.targeting.targetedSegmentIds.intersect(segmentIds).nonEmpty

  override def validateUser(campaign: Campaign, user: Option[User], device: Option[Device]): Boolean = {
    val campaignCountry = campaign.country.trim.toLowerCase
    getUserCountry(user).contains(campaignCountry) || getDeviceCountry(device).contains(campaignCountry)
  }

  // true for empty bids, otherwise compare with campaign bid
  override def validateBids(campaign: Campaign, impressions: List[Impression]): Boolean =
    impressions.map(_.bidFloor).exists(_.fold(true)(_ <= campaign.bid))

  override def validateSite(campaign: Campaign, site: Site): Boolean =
    campaign.targeting.targetedSiteIds.isEmpty || campaign.targeting.targetedSiteIds.contains(site.id)

  override def validateBanners(campaign: Campaign, impressions: List[Impression]): Boolean =
    impressions.flatMap {
      case Impression(_, _, _, Some(w), _, _, Some(h), _, _)                         => campaign.banners.filter(banner => banner.height == h && banner.width == w)
      case Impression(_, Some(wmin), Some(wmax), _, Some(hmin), Some(hmax), _, _, _) => campaign.banners.filter(banner => banner.height <= hmax && banner.width <= wmax && banner.height >= hmin && banner.width >= wmin)
      case Impression(_, Some(wmin), _, _, Some(hmin), _, _, _, _)                   => campaign.banners.filter(banner => banner.height >= hmin && banner.width >= wmin)
      case Impression(_, _, Some(wmax), _, _, Some(hmax), _, _, _)                   => campaign.banners.filter(banner => banner.height <= hmax && banner.width <= wmax)
      case _                                                                         => Nil
    }.nonEmpty

  private def getUserCountry(user: Option[User]): Option[String] = for {
    u       <- user
    geo     <- u.geo
    country <- geo.country
  } yield country.trim.toLowerCase

  private def getDeviceCountry(device: Option[Device]): Option[String] = for {
    d       <- device
    geo     <- d.geo
    country <- geo.country
  } yield country.trim.toLowerCase
}
