# Forbidden Island, in Kotlin

I'm codifying
[the rules of Forbidden Island](http://www.gamewright.com/gamewright/pdfs/Rules/ForbiddenIslandTM-RULES.pdf)
in [Kotlin](https://kotlinlang.org/).

This isn't a playable game. It's just the rules of the game, written in code.

You could use it to:
* create a program that plays the game;
* enjoy the fun and challenge of implementing the rules yourself; or
* implement a game playable by humans. (Yawn)

## Want to create a program that plays the game?

There is a `play` subpackge in `src/main` that contains a `GamePlayer` interface for 
developing automata which can 'play' games of Forbidden Island. The package also 
contains functions which can test the playing automaton and print the results of the
test as a Markdown table.

## Want to implement the rules yourself?

The code was developed almost completely using test-driven design.
This means that the repository contains a large suite of fairly comprehensive tests.
That in turns means that, if you'd like to implement the rules yourself, you can avoid 
the drudgery of imagining and writing all the tests and can instead just hoe in 
coding the solution.

There is a branch named `tests-only` which contains all of the tests, but with all the
non-trivial functions of the implementation replaced with `TODO()`.   

It's recommended that you get the tests passing in the following order, as each of these
can be implemented without depending on any implementation from the following tests.
* CollectionsTest
* ModelTest
* TreasureDeckTest
* GameMapTest
* GameSetupTest
* GameInitialisationTest
* GamePrinterTest
* GameStateResultTest
* GamePhaseTest
* GameStateProgressionTest
* GameStateAvailableActionsTest
* FullGameTest 