package zoneent.wordle

import zio.{ZIO, ZManaged, Layer}
import zio.Console.{printLine}
import zio.test.*
import zio.stream.*
import zio.stream.compression.CompressionException
import java.io.IOException
import java.nio.charset.MalformedInputException
import java.io.UnsupportedEncodingException

val dictDataStream: ZStream[Any, IOException, String] = 
  ZStream.fromInputStreamManaged(
    ZManaged.fromAutoCloseable(
      ZIO.attempt(TestDict.getClass.getResourceAsStream("/wordlist.txt.gz"))
        .refineToOrDie[IOException]
    )
  ).via(ZPipeline.gunzip(64 * 1024))
    .via(ZPipeline.utfDecode)
    .via(ZPipeline.splitLines)
    .refineOrDie {
      case e: IOException => e
      case _: CompressionException => new IOException("Gunzip problem with source data")
      case e => new IOException("Unexpected read error, possible utf8 decoding? " + e.getMessage)
    }

val wordsLayer: Layer[IOException, Dict] = Dict.makeWordsLayer(dictDataStream)

val countWords: ZIO[Dict, Nothing, Int] = for {
    words <- ZIO.service[Dict]
  } yield(words.words.size)

def applyRules(rules: Iterable[Rule]): ZIO[Dict, Nothing, Dict] = for {
  initialWords <- ZIO.service[Dict]
  newWords = initialWords.applyRules(rules)
} yield (newWords)

val TestDict = suite("dict") (
  test("Can Load Dictionary") {
    assertM(countWords.provideLayer(wordsLayer))(Assertion.isGreaterThan(1000))
  },
  test("Reduce based on a rule") {
    val rules = Omit("corf")  ++ 
      List(Exact('a', 1), Exact('y', 4), Known('n', 2, 4))

    val available = applyRules(rules).provideLayer(wordsLayer)
    val size = for {
      myDict <- applyRules(rules)
      _ <- ZIO.debug(myDict.printall)
    } yield(myDict.words.size)
      
    assertM(size.provideLayer(wordsLayer))(Assertion.isGreaterThan(0))
  },
  test("Word score") {
    val dictScore = for {
      d <- ZIO.service[Dict]
      score = d.score("monal")
      _ <- ZIO.debug(s"monal score is ${score}")
    } yield(score)
    assertM(dictScore.provideLayer(wordsLayer))(Assertion.isGreaterThan(30.0))
  }
)
