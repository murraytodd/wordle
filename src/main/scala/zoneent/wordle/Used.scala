package zoneent.wordle

import java.net.URI
import zio.*
import zio.stream.*
import java.io.IOException
import java.io.InputStream
import zio.stream.compression.CompressionException

trait UsedLoader {
  def prescanningProcess(s: ZStream[Any, Exception, String]) = s // default: start immediately
  def scanningEnded(line: String): Boolean = false // default: scan every line
  def transformLine(line: String): String = line // no transformation needed
  def getStream: ZIO[Any, IOException, InputStream] // safely get data source

  def decompressor: ZPipeline[Any, CompressionException, Byte, Byte] = ZPipeline.identity // ZPipeline.gunzip(64 * 1048)
  def source: ZStream[Any, Exception, String] = ZStream.fromInputStreamZIO(getStream) 
    >>> decompressor >>> ZPipeline.utf8Decode >>> ZPipeline.splitLines

  def zLoader: ZIO[Any, Exception, Set[String]] = {
    val prestaged = prescanningProcess(source) // prescan if needed
    val processSink = ZSink.collectAllUntil[String](scanningEnded)
    prestaged.run(processSink).map(_.dropRight(1).map(transformLine).toSet)
  }
}

case class UsedBackupLoader(file: String = "/omits.txt.gz") extends UsedLoader:
  override def getStream: ZIO[Any, IOException, InputStream] = 
    val backup = Option(this.getClass().getResourceAsStream(file))
    backup match
      case None => ZIO.fail(new IOException(s"File ${file} could not be found in the resource path."))
      case Some(value) => ZIO.succeed(value)
    
  override def decompressor: ZPipeline[Any, CompressionException, Byte, Byte] = ZPipeline.gunzip(64 * 1048)

case class RPSGLoader(uri: URI = new URI("https://www.rockpapershotgun.com/wordle-past-answers")) extends UsedLoader {
  override def getStream: ZIO[Any, IOException, InputStream] = ZIO.attempt(uri.toURL.openStream()).refineToOrDie[IOException]
  override def prescanningProcess(s: ZStream[Any, Exception, String]): ZStream[Any, Exception, String] =
    s.dropUntil(_.startsWith("<h2>All Wordle answers</h2>")).drop(2)
  override def scanningEnded(line: String): Boolean = line.startsWith("</ul>")
  override def transformLine(line: String): String = line.drop(4).dropRight(5).toLowerCase()
}

case class FiveForksLoader(uri: URI = new URI("https://www.fiveforks.com/wordle/")) extends UsedLoader {
  override def getStream: ZIO[Any, IOException, InputStream] = ZIO.attempt(uri.toURL.openStream()).refineToOrDie[IOException]
  override def prescanningProcess(s: ZStream[Any, Exception, String]): ZStream[Any, Exception, String] = 
    s.dropUntil(line => line.startsWith("<div") && line.contains("<strong>Alphabetical</strong>")).drop(1)
  override def scanningEnded(line: String): Boolean = line.startsWith("<div")
  override def transformLine(line: String): String = line.take(5).toLowerCase()
}