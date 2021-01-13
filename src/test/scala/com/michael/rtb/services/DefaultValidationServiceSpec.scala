package com.michael.rtb.services

import com.michael.rtb.domain._
import com.michael.rtb.services.impl.DefaultValidationService
import com.michael.rtb.utils.AppUtils._
import com.softwaremill.macwire.wire
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DefaultValidationServiceSpec extends AnyWordSpec with Matchers {

  val validationService: DefaultValidationService = wire[DefaultValidationService]

  "DefaultValidationService" should {

    "validate that banners are present" in {

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

      val result = validationService.validateBanners(campaign, impressions)

      result should be(true)

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

      val result = validationService.validateSite(campaign, Site(1, "https://random.com/1"))

      result should be(true)

    }

    "return true if campaign's site targeting contains site's id" in {

      val campaign = Campaign(1, "USA", Targeting(Set(1), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val result = validationService.validateSite(campaign, Site(1, "https://random.com/1"))

      result should be(true)

    }

    "return true if campaign's segment targeting is empty" in {

      val campaign = Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val result = validationService.validateSegments(campaign, Set(1, 2))

      result should be(true)

    }

    "return true if campaign's segment targeting contains segments' ids" in {

      val campaign = Campaign(1, "USA", Targeting(Set(), Set(1)), List(Banner(1, "https://random.com/1", 1, 1)), 1.0)

      val result = validationService.validateSegments(campaign, Set(1, 2))

      result should be(true)

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
