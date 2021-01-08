package com.michael.rtb

package object domain {

  case class Campaign(id: Int, country: String, targeting: Targeting, banners: List[Banner], bid: Double)

  case class Targeting(targetedSiteIds: Set[Int], targetedSegmentIds: Set[Int])

  case class Banner(id: Int, src: String, width: Int, height: Int)


  case class Impression(id: String, wMin: Option[Int], wMax: Option[Int], w: Option[Int], hMin: Option[Int], hMax: Option[Int], h: Option[Int], bidFloor: Option[Double], tagId: String)

  case class Site(id: Int, domain: String)

  case class User(id: String, geo: Option[Geo])

  case class Device(id: String, geo: Option[Geo])

  case class Geo(country: Option[String])

}
