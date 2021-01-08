package com.michael.rtb.services

import com.michael.rtb.domain.{Banner, Campaign, Device, Geo, Impression, Site, Targeting, User}
import com.michael.rtb.modules.TestApplicationModule
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.michael.rtb.Utils._

class ValidationServiceSpec extends AnyWordSpec with Matchers with TestApplicationModule {

  "DefaultValidationService" should {

    "filter banners by size" in {

      val validImp = Impression(
        id = uuid,
        wMin = Some(1),
        wMax = Some(1),
        w = Some(1),
        hMin = Some(1),
        hMax = Some(1),
        h = Some(1),
        bidFloor = Some(1.0),
        tagId = uuid)

      val invalidImp = Impression(
        id = uuid,
        wMin = None,
        wMax = None,
        w = None,
        hMin = None,
        hMax = None,
        h = Some(1),
        bidFloor = Some(3.0),
        tagId = uuid)

      val campaign = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val impressions = List(validImp, invalidImp)

      val result: List[Banner] = validationService.filterBanners(campaign, impressions)

      result.nonEmpty should be(true)
      result.size should be(1)

    }

    "return true for impressions with bid floor less than campaign's bid" in {

      val campaign = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val imp = Impression(
        id = uuid,
        wMin = Some(1),
        wMax = Some(1),
        w = Some(1),
        hMin = Some(1),
        hMax = Some(1),
        h = Some(1),
        bidFloor = Some(1.0),
        tagId = uuid)

      val result = validationService.validateBids(campaign, List(imp))

      result should be(true)

    }

    "return true for impressions with empty bid floor amount" in {

      val campaign = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val imp = Impression(
        id = uuid,
        wMin = Some(1),
        wMax = Some(1),
        w = Some(1),
        hMin = Some(1),
        hMax = Some(1),
        h = Some(1),
        bidFloor = None,
        tagId = uuid)

      val result = validationService.validateBids(campaign, List(imp))

      result should be(true)

    }

    "return true if campaign's site targeting is empty" in {

      val campaign = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      validationService.validateSite(campaign, Site(1, "https://random.com/1"))

    }

    "return true if campaign's site targeting contains site's id" in {

      val campaign = Campaign(1, "USA", Targeting(Set(1), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      validationService.validateSite(campaign, Site(1, "https://random.com/1"))

    }

    "return true if campaign's site targeting contains segments' ids" in {

      val campaign = Campaign(1, "USA", Targeting(Set(), Set(1)), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      validationService.validateSegments(campaign, Set(1, 2))

    }

    "return true if campaign contains user or user's device country" in {

      val geo = Geo(Some("USA"))
      val user = User(uuid, Some(geo))
      val device = Device(uuid, Some(geo))
      val campaign = Campaign(1, "USA", Targeting(Set(), Set(1)), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val resultWithUser = validationService.validateUser(campaign, Some(user), None)

      resultWithUser should be(true)

      val resultWithDevice = validationService.validateUser(campaign, None, Some(device))

      resultWithDevice should be(true)

      val resultWithoutData = validationService.validateUser(campaign, None, None)

      resultWithoutData should be(false)

    }

  }

}
