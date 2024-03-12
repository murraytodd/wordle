package zoneent.wordle

import java.net.URI
import zio.*
import zio.stream.*
import java.io.IOException



object RockPaperShotgun:

  val usedURI = new URI("https://www.rockpapershotgun.com/wordle-past-answers")
  val usedURL = usedURI.toURL

  def parsePage: List[String] = 
    val iterator = scala.io.Source.fromURL(usedURL).getLines()

    iterator.takeWhile(! _.startsWith("<h2>All Wordle answers</h2>")).toList // empty lines leading to the list
    assert(iterator.next()=="<ul class=\"inline\">") // empty one more line

    iterator.takeWhile(l => l != "</ul>").map(_.drop(4).dropRight(5).toLowerCase()).toList

  /**
    * ZIO effect to load the previously-used words list from disk
    */  
  val parseBackup: ZIO[Any, Exception, Set[String]] = ZStream.fromResource("omits.txt.gz").via(ZPipeline.gunzip(64 * 1024) >>> ZPipeline.utf8Decode >>> ZPipeline.splitLines)
      >>> ZSink.collectAllToSet
  
  val parseZIO: ZIO[Any, Exception, Set[String]] = 
    val attemptURLStream = ZIO.attempt(usedURI.toURL.openStream())
    
    val source = (ZStream.fromInputStreamZIO(attemptURLStream.refineToOrDie[IOException]) >>> ZPipeline.utf8Decode >>> ZPipeline.splitLines)
      .dropUntil(_.startsWith("<h2>All Wordle answers</h2>")).drop(2)
    
    val processSink = ZSink.collectAllUntil[String](_.startsWith("</ul>"))
      .map(_.map(_.drop(4).dropRight(5).toLowerCase()).dropRight(1).toSet)
    source.run(processSink)