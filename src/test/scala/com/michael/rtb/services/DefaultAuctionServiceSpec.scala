package com.michael.rtb.services

import com.michael.rtb.domain.{Banner, Campaign, Targeting}
import com.michael.rtb.services.AuctionService.AuctionResult
import com.michael.rtb.services.impl.DefaultAuctionService
import com.softwaremill.macwire.wire
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DefaultAuctionServiceSpec extends AnyWordSpec with Matchers {

  val auctionService: DefaultAuctionService = wire[DefaultAuctionService]

  "Auction service" should {

    "return the highest bidder" in {

      val cmps = List(
        Campaign(1, "USA", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 1.0),
        Campaign(2, "Poland", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 5.0),
        Campaign(3, "UK", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 10.0)
      )

      auctionService.findWinnerCampaign(cmps) should be(Some(AuctionResult(Campaign(3, "UK", Targeting(Set(), Set()), List(Banner(1, "https://random.com/1", 1, 1)), 10.0), 7.0)))

    }

  }

}
