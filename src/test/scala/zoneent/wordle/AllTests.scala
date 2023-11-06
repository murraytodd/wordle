package zoneent.wordle

import zio.test._ 

object AllTests extends ZIOSpecDefault:
  def spec = suite("All tests")(TestDict, TestRules)

