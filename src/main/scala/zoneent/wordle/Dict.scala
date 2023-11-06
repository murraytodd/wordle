package zoneent.wordle

import zio._
import zio.stream._

final case class Dict(words: Set[String], frequencies: Map[Char, Double]):

  def applyRules(rules: Iterable[Rule]) =
    val newWords = words.filter(word => rules.forall(rule => rule.test(word)))
    Dict(newWords, frequencies)

  def score(word: String): Double = word.toList.map(frequencies).sum

  def printWord(w: String, s: Double): String = f"${w}: ${s}%2.2f"

  lazy val printall: String = words.map(w => (w, score(w))).toList.sortBy(- _._2)
    .map(t => printWord(t._1, t._2)).mkString("; ")

object Dict:

  def apply(data: Iterable[String]) =
    val words = data
      .map(_.toLowerCase)
      .filter(w => (w.length == 5) && (w.forall(c => (c >= 'a') && (c <= 'z'))))
      .toSet

    val letterCounts: List[(Char, Int)] = words.toList.flatten
      .groupBy(identity).view.mapValues(_.length).toList

    val totalCount: Int = letterCounts.map(_._2).sum

    val frequencies: Map[Char, Double] = letterCounts.toMap
      .view.mapValues(_ * 100.0 / totalCount).toMap 

    new Dict(words, frequencies)   

  def makeWordsLayer(wordStream: Stream[Exception, String]): Layer[Exception, Dict] = ZLayer.fromZIO (
    {
      wordStream.map(_.toLowerCase).filter(_.matches("[a-z]{5}")) 
        >>> ZSink.collectAllToSet
    }.map{words => Dict(words)}
  )