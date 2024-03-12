package zoneent.wordle

import zio.test._ 
import zio._

object AllTests extends ZIOSpecDefault:
  def spec = suite("All tests")(TestDict, TestRules, TestUsed)

def extract[T](z: ZIO[Any, Any, T]) = Unsafe.unsafe { implicit unsafe =>
  Runtime.default.unsafe.run(z).getOrThrowFiberFailure()
}