package zoneent.wordle

import scala.io.Source
import zio.*
import zio.Console.{printLine, readLine}
import zio.stream.*

import java.io.IOException
import java.net.URL
import java.net.URI
import RuleSet.*
import zoneent.wordle.Rule.Issue

import scala.annotation.tailrec

object ConsoleApp extends ZIOAppDefault {

  // val dictURL = "https://www-personal.umich.edu/~jlawler/wordlist"

  val dictStream: Stream[IOException, String] = 
    ZStream.fromResource("words.txt").via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
  
  val dictLayer = Dict.makeWordsLayer(dictStream)

  def getAndApply(rules: Seq[Rule], dictionary: Dict): IO[Issue, (Seq[Rule], Dict)] = {

    for {
      _ <- printLine("Enter a command:").orDie
      line <- readLine.orDie
      command <- ZIO.fromEither(Rule.apply(line))
      newRules = rules.appendRules(command)
      newDict = dictionary.applyRules(newRules)
      _ <- printLine(newDict.printall).orDie
    } yield((newRules, newDict))
  }.catchSome { case Issue.ParseError => printLine("Try again").orDie *> getAndApply(rules: Seq[Rule], dictionary) }

  val run = ZIO.service[Dict].flatMap { fullDict =>

    def iterate(rules: Seq[Rule], dictionary: Dict): IO[Issue, (Seq[Rule], Dict)] =
      getAndApply(Seq.empty, dictionary)
        .catchSome { case Issue.Reset => printLine("Clear called").orDie *> iterate(Seq.empty, fullDict) }
        .flatMap { case (r, d) => iterate(r, d) }

    iterate(Seq.empty, fullDict)

  }.provideSomeLayer(dictLayer)
    .catchSome { case Issue.SafeExit => printLine("Thanks for playing!") *> ZIO.succeed(()) }
    

}
