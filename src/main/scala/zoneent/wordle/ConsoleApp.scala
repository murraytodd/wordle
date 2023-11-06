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

  val dictStream: Stream[Exception, String] = 
    ZStream.fromResource("words.txt.gz").via(ZPipeline.gunzip(64 * 1024) >>> ZPipeline.utf8Decode >>> ZPipeline.splitLines)
  val dictLayer = Dict.makeWordsLayer(dictStream)

  val instructions = """The available commands are:
    |  exact pos letter - Indicates you know the letter is confirmed to be in position pos
    |  known pos letter - Indicates you know the letter is in the word, but NOT in position pos
    |  omit letter(s)   - None of the provided letters are in the word.
    |  clear            - Reset and start a new search.
    |Note that multiple commands can be entered on the same line by separating them with a semicolon.
    |""".stripMargin

  def getAndApply(rules: Seq[Rule], dictionary: Dict): IO[Issue, (Seq[Rule], Dict)] = {

    for {
      _ <- printLine("Enter a command:").orDie
      line <- readLine.orDie
      command <- ZIO.fromEither(Rule.apply(line))
      newRules = rules.appendRules(command)
      newDict = dictionary.applyRules(newRules)
      _ <- printLine(newDict.printall).orDie
    } yield((newRules, newDict))
  }.catchSome { case Issue.ParseError => printLine("Command not understood. Try again. Type 'help' for instructions.").orDie *> getAndApply(rules: Seq[Rule], dictionary) }

  val run = ZIO.service[Dict].flatMap { fullDict =>

    def iterate(rules: Seq[Rule], dictionary: Dict): IO[Issue, (Seq[Rule], Dict)] =
      getAndApply(Seq.empty, dictionary)
        .catchSome { case Issue.Reset => printLine("Clear called").orDie *> iterate(Seq.empty, fullDict) }
        .catchSome { case Issue.Help => printLine(instructions).orDie *> iterate(Seq.empty, dictionary) }
        .flatMap { case (r, d) => iterate(r, d) }

    iterate(Seq.empty, fullDict)

  }.provideSomeLayer(dictLayer)
    .catchSome { case Issue.SafeExit => printLine("Thanks for playing!") *> ZIO.succeed(()) }
    

}
