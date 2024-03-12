package zoneent.wordle

import zio.test.{test,*}
import zio.test.Assertion.*
import RuleSet.*
import java.io.IOException
import zio.*
import java.net.URI

val TestUsed = suite("used") (
  test("Loading backup data") {
    assertZIO(UsedBackupLoader().zLoader)(Assertion.isNonEmpty)
  },
  test("Backup loader with bad path should fail") {
    val bad = UsedBackupLoader("bad.path").zLoader.map(_.size)
    assertZIO(bad.isFailure)(isTrue)
  },
  test("New RPSG loader works") {
    val loader = RPSGLoader().zLoader
    assertZIO(loader)(isNonEmpty) && assertZIO(loader.map(_.size))(isGreaterThan(100))
  },
  test("Loader with bad URI fails safely") {
    val loader = RPSGLoader(new URI("https://nonexistiant.url.blah")).zLoader
    assertZIO(loader.isFailure)(isTrue)
  },
  test("Five Forks Loader works") {
    val loader = FiveForksLoader().zLoader
    assertZIO(loader)(isNonEmpty) && assertZIO(loader.map(_.size))(isGreaterThan(900))
  }
)