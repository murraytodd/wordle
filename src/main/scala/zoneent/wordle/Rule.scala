package zoneent.wordle

import scala.util.{Try,Success}

sealed trait Rule {
  def test(word: String): Boolean
}

object RuleSet {
  extension (rules: Seq[Rule]) {
    def appendRule(r: Rule): Seq[Rule] = r match {
      case Exact(eChar, ePos) => 
        Exact(eChar, ePos) +: rules.collect {
          case k: Known if k.letter != eChar => k
          case e: Exact => e
          case o: Omit => o
        }
      case Known(kChar, kPos*) => {
        val existingKnown: Option[Known] = rules.collectFirst {
          case l: Known if l.letter == kChar => Known(l.letter, (kPos ++ l.antiPos)*)
        }
        existingKnown match {
          case Some(k: Known) => { // Known had gotten consolidated, now replace in the rules
            val swapped = rules.collect {
              case k2: Known => if (k2.letter == k.letter) k else k2
              case r => r
            }
            swapped
          }
          case None => Known(kChar, kPos*) +: rules // otherwise, simply append it
        }
      }
      case r => r +: rules
    }

    def appendRules(r: Iterable[Rule]): Seq[Rule] = 
      r.foldLeft(rules) { (aRules, rule) => aRules.appendRule(rule) }
  }
}

final case class Exact(letter: Char, position: Int) extends Rule {
  override def test(word: String): Boolean = Try { word.charAt(position) } match {
    case Success(l) if l == letter => true
    case _ => false
  }
}

final case class Known(letter: Char, antiPos: Int *) extends Rule {
  override def test(word: String): Boolean = { //word.find(_ == letter).isDefined 
    word.find(_ == letter).isDefined && antiPos.forall(i => word(i) != letter)
  }
}

final case class Omit(letter: Char) extends Rule {
  override def test(word: String): Boolean = word.find(_ == letter).isEmpty
}

object Omit {
  def apply(letters: String): List[Omit] = letters.toList.map(Omit(_))
}

object Rule {

  enum Issue:
    case ParseError, SafeExit, Reset

  val omitCommand = "omit ([a-z]+)".r
  val knownCommand = "known ([a-z]) ([1-5])".r
  val exactCommand = "exact ([a-z]) ([1-5])".r
  val exitCommand = "(exit|done|quit)".r

  def apply(command: String): Either[Issue, Seq[Rule]] = {
    command.toLowerCase match {
      case omitCommand(r) => Right(Omit(r))
      case knownCommand(l,p) => Right(List(Known(l.head, p.toInt - 1)))
      case exactCommand(l,p) => Right(List(Exact(l.head, p.toInt - 1)))
      case "clear" => Left(Issue.Reset)
      case exitCommand(_) => Left(Issue.SafeExit)
      case _ => Left(Issue.ParseError)
    }
  }
}

