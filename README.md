# Forbidden Island, in Kotlin

I've codified
[the rules of Forbidden Island](http://www.gamewright.com/gamewright/pdfs/Rules/ForbiddenIslandTM-RULES.pdf)
in [Kotlin](https://kotlinlang.org/).

This isn't a playable game. It's just the rules of the game, written in code.

You could use it to:
* create a program that plays the game;
* enjoy the fun and challenge of implementing the rules yourself; or
* implement a game playable by humans. (Yawn)

## Run a sample player

If you want to see a program in action, you can either start it with Maven:
```bash
mvn exec:java -Dexec.mainClass="com.grahamlea.forbiddenisland.play.RandomGamePlayer"
```

Or compile and execute it using Kotlin's tools:
```bash
kotlinc src/main/kotlin/ -d forbidden_island.jar
kotlin -classpath forbidden_island.jar com.grahamlea.forbiddenisland.play.RandomGamePlayer
```

## Want to create a program that plays the game?

There is a `play` subpackge in `src/main` that contains a `GamePlayer` interface for 
developing automata which can 'play' games of Forbidden Island. The package also 
contains functions which can test the playing automaton and print the results of the
test as a Markdown table.

You can see examples of `GamePlayer` implementations and how to use the various
 testing functions in the `RandomGamePlayer` and `RulesBasedGamePlayer` classes.

### Submit your GamePlayer
If you've created an interesting `GamePlayer` and would like to share it with others,
I'm keen to collect implementations in this repository via pull requests.
Please follow these steps to submit:
* place your source files in a subpackage of `com.grahamlea.forbiddenisland.play` that
is your github handle in lowercase;
* test your `GamePlayer` using `testGamePlayer` with 100,000 games per category and 
print the results using `printGamePlayerTestResult`
(this will likely take a long time;
my implementation took 45 min. to run this many tests);
* create a README.md in your subpackage, describe the design and most interesting 
aspects of your `GamePlayer` implementation, and copy in your test results;
* submit a pull request.

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

## My tips for developing a game-playing automaton
From my experiecnce, creating a `GamePlayer` that gets any significant results seems to 
be a bit of a Herculean task, but also a little bit addictive, so be prepared to waste
a whole lotta time!

It's probably best to play the actual game a few times to understand it before trying 
to solve it with code.
The physical game is fun, quick, classy and collaborative, but there's also a 
[highly-rated iPad version](https://itunes.apple.com/au/app/forbidden-island/id427419772?mt=8)
if you want to play solitaire.

Extend the `ExplainingGamePlayer` rather than the base `GamePlayer` interface, 
as this will give you extra information when you test your solution that you can use 
to iterate and optimise. 

Iterate on your `GamePlayer`, running tests, printing the stats, and using them to guide 
your development.

In developing my `RulesBasedGamePlayer`, I started by trying to just make the game last 
longer, i.e. lose later, which meant focusing on increasing the number of actions per 
game.
After I thought I'd coded all the obvious things to stop the game being lost, I started 
adding rules which encoded simple strategies for moving towards good places to be and
doing good things.

Don't focus on win rates to start with, because it takes a lot of work before they will
start to move off 0%.
Instead, focus on one thing at a time.
For example, if your player is losing 89% of games because of 
"BothPickupLocationsSankBeforeCollectingTreasure", that's probably a good place to focus
in order to bring that number down.

For most development, you can just run tests against the 'Novice' starting flood level,
as the flood level has an obvious and simple impact on the game.
However, different numbers of players have a more complex impact on the game, so I'd
recommend always testing with categories of 2, 3 and 4 players, and checking that each
improvement you make to your solution makes games across all player numbers improve 
(or at least makes some far better than it makes the others worse).

Beware of the trade-off between the `gamesPerCategory` parameter to `testGamePlayer` 
and over-fitting of your solution.
A small `gamesPerCategory` allows for faster iteration, but it can be easy to start
creating a solution which is highly tuned to a small number of games but doesn't work 
as well on a larger sample.
You can use the `VarianceCheck` to gather information about the statistical variance
of your solution using different sample sizes of random games.

If you run out of ideas and want some inspiration for how to improve your solution,
try using the `stepThroughGame` function and looking for choices your program is 
making that could be improved.
As an example, I found a lot of my rules were competing against each other, so one rule
would move a player off a tile, then the next action another rule would move them 
straight back.
You can also use the `debugGame` function to run a game or many games quickly and print 
out the game state whenever a certain condition that you specify occurs.
