package zoneent.wordle

import zio.test.DefaultRunnableSpec

object AllTests extends DefaultRunnableSpec {
  def spec = suite("All tests")(TestDict, TestRules)
}
