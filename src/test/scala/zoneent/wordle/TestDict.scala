package zoneent.wordle

import zio.{ZIO, Layer}
import zio.Console.{printLine}
import zio.test.*
import zio.stream.*
import zio.stream.compression.CompressionException
import java.nio.charset.MalformedInputException
import java.io.UnsupportedEncodingException

val dictDataStream: ZStream[Any, Exception, String] = 
  ZStream.fromResource("words.txt.gz")
    .via(ZPipeline.gunzip(64 * 1024))
    .via(ZPipeline.utfDecode)
    .via(ZPipeline.splitLines)
    .refineOrDie {
      case e => new Exception("Unexpected read error, possible utf8 decoding? " + e.getMessage)
    }

val wordsLayer: Layer[Exception, Dict] = Dict.makeWordsLayer(dictDataStream)

val countWords: ZIO[Dict, Nothing, Int] = for {
    words <- ZIO.service[Dict]
  } yield(words.words.size)

def applyRules(rules: Iterable[Rule]): ZIO[Dict, Nothing, Dict] = for {
  initialWords <- ZIO.service[Dict]
  newWords = initialWords.applyRules(rules)
} yield (newWords)

val TestDict = suite("dict") (
  test("Can Load Dictionary") {
    assertZIO(countWords.provideLayer(wordsLayer))(Assertion.isGreaterThan(1000))
  },
  test("Reduce based on a rule") {
    val rules = Omit("corf")  ++ 
      List(Exact('a', 1), Exact('y', 4), Known('n', 2, 4))

    val available = applyRules(rules).provideLayer(wordsLayer)
    val size = for {
      myDict <- applyRules(rules)
      _ <- ZIO.debug(myDict.printall)
    } yield(myDict.words.size)
      
    assertZIO(size.provideLayer(wordsLayer))(Assertion.isGreaterThan(0))
  },
  test("Word score") {
    val dictScore = for {
      d <- ZIO.service[Dict]
      score = d.score("monal")
      _ <- ZIO.debug(s"monal score is ${score}")
    } yield(score)
    assertZIO(dictScore.provideLayer(wordsLayer))(Assertion.isGreaterThan(28.0))
  }
)
