package zoneent.wordle

import zio.test.*
import zio.test.Assertion.equalTo
import RuleSet.*

val TestRules = suite("rules") (
  test("RuleSet replacement") {


    val rules = List(Omit('a'), Known('b', 1), Exact('c', 2))

    val r1 = rules.appendRule(Exact('b', 4))
    val r2 = rules.appendRule(Omit('z'))
   
    assert(r1)(equalTo(List(Exact('b', 4), Omit('a'), Exact('c', 2)))) &&
      assert(r2)(equalTo(Omit('z') :: List(Omit('a'), Known('b', 1), Exact('c', 2))))
    
  },
  test("Multiple appends") {

    val initial = List(Omit('a'), Known('b', 1))
    val newRules = List(Exact('b', 4), Omit('z'))

    val appendedRules = initial.appendRules(newRules)

    val expected = List(Omit('z'), Exact('b', 4), Omit('a'))

    assert(appendedRules)(equalTo(expected))
  },
  test("Consolidate Knowns") {
    val initialWithKnown = List(Omit('a'), Known('b', 2, 4))
    val initialWithoutKnown = List(Omit('a'), Known('c', 2, 3))

    val appendedWithKnown = initialWithKnown.appendRule(Known('b', 1))
    val appendedWithoutKnown = initialWithoutKnown.appendRule(Known('b', 1))

    val expectedWithKnown = List(Omit('a'), Known('b', 1, 2, 4))
    val expectedWithoutKnown = List(Known('b', 1), Omit('a'), Known('c', 2, 3))
    
    assert(appendedWithKnown)(equalTo(expectedWithKnown)) &&
      assert(appendedWithoutKnown)(equalTo(expectedWithoutKnown))
  }
)