# ZIO Wordle Solver

The purpose of this application is twofold: to design an app to help figure out wordle solutions and to have 
a way to demonstrate some of the features of ZIO, including testing, streaming, and writing console apps.

I'm also proud to say that this now works with the most recent (currently 21.x) version of Java, the most recent 
(currently 3.4) version of Scala, and the most recent (currently 2.0.21) version of ZIO. During the emergence of
Scala 3.0 and ZIO 2.0, these was pretty much impossible. But damn! We're finally hit true greenfield mode!

## sbt project compiled with Scala 3

### Usage

Run the project and as you play wordle, enter your "clues" that you learn into the prompt. The "clues" come in the following forms:

* `exact e 4`  - You got a green E in the fourth space
* `known t 3`  - You know there is a T, but it is *not* in the third space (it was yellow)
* `omit foaz`  - You know that none of the letters "f", "o", "a", or "z" are in the solution. (They were dull grey)
* `multiple e` - You know that there are more than one letter "e" in the solution

You can also combine multiple clues by separating them by a semicolon.