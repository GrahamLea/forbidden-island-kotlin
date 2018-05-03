package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.LocationFloodState.Flooded
import com.grahamlea.forbiddenisland.LocationFloodState.Sunken

object GamePrinter {
    fun toString(game: Game) = toString(game.gameSetup.map, game.gameState.locationFloodStates, game.gameState.playerPositions)

    fun toString(
            map: GameMap,
            locationFloodStates: ImmutableMap<Location, LocationFloodState>,
            playerPositions: ImmutableMap<Adventurer, Position>
    ): String {
        val playerSites = playerPositions.mapValues { map.mapSiteAt(it.value) }

        val maxLocationNameLengthPerColumn =
            map.mapSites.groupBy { it.position.x }.mapValues { it.value.map { it.location.toString().length }.max()!! }

        val maxPlayersOnSingleSitePerColumn =
            map.mapSites.groupBy { it.position.x }.mapValues { it.value.map { site -> playerSites.count { it.value == site } }.max() ?: 0 }

        val mapSiteStrings = map.mapSites.associate { site ->
            site to
                    toString(
                            site,
                            playerSites.filterValues { it == site }.keys,
                            maxPlayersOnSingleSitePerColumn.getValue(site.position.x),
                            maxLocationNameLengthPerColumn[site.position.x],
                            locationFloodStates[site.location]
                    )
        }.toSortedMap()
        val columnWidths = mapSiteStrings.filterKeys { it.position.y == 3 }.values.map { it.length }
        return buildString {
            for (y in 1..6) {
                if (y !in 3..4) append(" ".repeat(columnWidths[0] + 1))
                if (y !in 2..5) append(" ".repeat(columnWidths[1] + 1))
                append(mapSiteStrings.filterKeys { it.position.y == y }.values.joinToString(" "))
                append("\n")
            }
        }
    }

    fun toString(
            mapSite: MapSite,
            players: Set<Adventurer>? = null,
            maxPlayersOnSingleSiteInColumn: Int = 0,
            padToAccommodateLocationNameLength: Int? = null,
            floodState: LocationFloodState? = null
    ): String {
        val playersString = when {
            players == null -> ""
            maxPlayersOnSingleSiteInColumn == 0 -> ""
            players.none() -> " ".repeat(maxPlayersOnSingleSiteInColumn + 1)
            else -> "%${maxPlayersOnSingleSiteInColumn}s>"
                        .format(players.map { if (it == Adventurer.Explorer) 'X' else it.name[0] }.sorted().joinToString(""))
        }
        val floodStateString = floodState?.let { when (it) { Sunken -> "**"; Flooded -> "* "; else -> "  "} } ?: ""
        return playersString +
            ((if (padToAccommodateLocationNameLength == null) "%s~%s" else "%s:%-${padToAccommodateLocationNameLength + 2}s")
                    .format(mapSite.position, mapSite.location.toString() + floodStateString))
    }

}