# RuleBasedGamePlayer
###by Graham Lea

##Description

This `GamePlayer` uses a list of rules to determine what to do at each point in the game.
The list of rules are in priority order and evaluated sequentially, and the first one to 
decide it wants to take an action given the current state gets to do so.

I developed the rules by first focusing on lengthening the time until games were lost,
by implementing rules that aimed to stop the game from ending and increase the 
"Avg. Actions".
Then, I focused on rules that would work towards collecting treasures, working on 
increasing the "Avg. Treasures" stat.
Finally, once the number of average treasures was getting up around 1.0, I wrote rules to 
finish the game if all treasures were collected.
Considering how hard it was to get to the point where even 0.5 treasures were collected
on average, I'm quite proud that I stuck with it and got to the point where I was
collecting 2.44 treasures on average and winning over 15% of Novice games.

Some of the rules are very simple.
For example: if it's possible to capture a treasure, then do it!
Others are more complex both in whether they activate and which of the available 
actions they choose.
So the rule `useSandbagRatherThanDiscardOneOfFourTreasureCards` only 
takes an action if a player needs to discard a card _and_ all of their treasure cards
are of the same treasure.
Then, which location to sandbag is selected using a preferential list of
locations based on their importance in winning (or not losing) the game.

All of the rules, though, take a fairly simplistic approach in that they operate almost 
entirely based on the current state and don't make much attempt to look forward in time
nor to maintain a notion of a consistent objective from one action to the next.
Consequently, one of the challenges late in the development was to stop
the rules from fighting with each other, for instance by one rule moving a player from 
A to B, and then another rule moving them back on the very next action.

Once I'd written most of these rules I started to tweak some of them, and implement some 
new ones, based on what I could see the `GamePlayer` choosing when I debugged it 
with `debugGame`. 
Often this meant observing small things about a game, then changing small things about 
the code and seeing what effect this had on the results.
Sometimes this resulted in an obvious improvement.
At other times, a seemingly logical change would make the results measurably worse.

I was originally testing with 1,000 games per category, as the `VarianceCheck` 
had shown that the Avg. Actions result of the `RandomGamePlayer` converged at around 
that point.
However, after running the variance check on the `RuleBasedGamePlayer`, I found that its 
win ratio results weren't converging satisfactorily until around 32,000 games.
Once I started testing with 32,000 games per category, I found that some of the changes
which previously appeared to make the results worse now made it better by small amounts.

Eventually I started trying really small tweaks on many different rules hunting for
really small gains.
For example, some rules now activate when there's 3 or 4 players, but not when there's 2.
At this point, essentially trying random little things to get random little improvements,
I started to feel like I was becoming a slow, meat-based neural network,
so I decided to call it a day with this rule-based approach.

##Results

Run with 100,000 games per category:

| |Novice|Normal|Elite|Legendary|
|---|---:|---:|---:|---:|
|2 players win rate|20.19%|8.91%|2.30%|0.78%|
|3 players win rate|18.47%|8.46%|2.72%|0.81%|
|4 players win rate|8.69%|3.93%|1.24%|0.21%|
|AdventurersWon|15.8%|7.1%|2.1%|0.6%|
|BothPickupLocationsSankBeforeCollectingTreasure|34.6%|42.0%|49.3%|41.8%|
|FoolsLandingSank|20.1%|21.0%|21.3%|17.2%|
|MaximumWaterLevelReached|16.2%|16.8%|14.6%|32.3%|
|PlayerDrowned|13.3%|13.1%|12.6%|8.0%|
|Avg. Treasures|2.44|2.05|1.65|1.37|
|Avg. Actions|58|53|48|43|

Time: 2763.7s
