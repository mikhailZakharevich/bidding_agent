package com.michael.rtb.repository.impl

import com.michael.rtb.domain.{Banner, Campaign, Targeting}
import com.michael.rtb.repository.CampaignsRepository

/** list of hard-coded campaigns
 *  This may also be a stateful actor storing campaigns
 * */
class DefaultCampaignsRepository extends CampaignsRepository {

  override def getCampaigns = List(
    Campaign(1,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(671, "https://example.com/671", 320, 240), Banner(871, "https://example.com/671", 640, 480)), 20),
    Campaign(2,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(672, "https://example.com/672", 200, 175), Banner(872, "https://example.com/672", 320, 240)), 15),
    Campaign(3,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(673, "https://example.com/673", 160, 600), Banner(873, "https://example.com/673", 640, 480)), 17),
    Campaign(4,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(674, "https://example.com/674", 800, 600), Banner(874, "https://example.com/674", 160, 600)), 22),
    Campaign(5,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(675, "https://example.com/675", 320, 240), Banner(875, "https://example.com/675", 800, 600)), 14),
    Campaign(6,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(676, "https://example.com/676", 640, 480), Banner(876, "https://example.com/676", 640, 480)), 16),
    Campaign(7,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(677, "https://example.com/677", 320, 240), Banner(877, "https://example.com/677", 160, 600)), 11),
    Campaign(8,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(678, "https://example.com/678", 800, 600), Banner(878, "https://example.com/678", 800, 600)), 23),
    Campaign(9,  "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(679, "https://example.com/679", 200, 175), Banner(879, "https://example.com/679", 200, 175)), 19),
    Campaign(10, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(710, "https://example.com/710", 800, 600), Banner(910, "https://example.com/710", 160, 600)), 18),
    Campaign(11, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(711, "https://example.com/711", 320, 240), Banner(911, "https://example.com/711", 800, 600)), 19),
    Campaign(12, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(712, "https://example.com/712", 800, 600), Banner(912, "https://example.com/712", 200, 175)), 21),
    Campaign(13, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(713, "https://example.com/713", 160, 600), Banner(913, "https://example.com/713", 320, 240)), 12),
    Campaign(14, "USA", Targeting(Set(), Set()),            List(Banner(714, "https://example.com/714", 160, 600), Banner(914, "https://example.com/714", 320, 240)), 13),
    Campaign(15, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(715, "https://example.com/715", 200, 175), Banner(915, "https://example.com/715", 640, 480)), 24),
    Campaign(16, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(716, "https://example.com/716", 200, 175), Banner(916, "https://example.com/716", 320, 240)), 15),
    Campaign(17, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(717, "https://example.com/717", 640, 480), Banner(917, "https://example.com/717", 200, 175)), 25),
    Campaign(18, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(718, "https://example.com/718", 160, 600), Banner(918, "https://example.com/718", 200, 175)), 12),
    Campaign(19, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(719, "https://example.com/719", 800, 600), Banner(919, "https://example.com/719", 160, 600)), 18),
    Campaign(20, "USA", Targeting(Set(1, 2, 3), Set(1, 2)), List(Banner(720, "https://example.com/720", 640, 480), Banner(920, "https://example.com/720", 640, 480)), 19),
  )
}
