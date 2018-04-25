package com.grahamlea.forbiddenisland

import com.grahamlea.forbiddenisland.LocationFloodState.*

object GamePrinter {
    fun toString(game: Game) = toString(game.gameSetup.map, game.gameState.locationFloodStates, game.gameState.playerPositions)

    fun toString(
            map: GameMap,
            locationFloodStates: ImmutableMap<Location, LocationFloodState>,
            playerPositions: ImmutableMap<Adventurer, MapSite>
    ): String {
        val maxLocationNameLengthPerColumn =
            map.mapSites.groupBy { it.position.x }.mapValues { it.value.map { it.location.toString().length }.max()!! }
        val maxAdventurersOnSingleSitePerColumn =
            map.mapSites.groupBy { it.position.x }.mapValues { it.value.map { site -> playerPositions.count { it.value == site } }.max() ?: 0 }

        val mapSiteStrings = map.mapSites.associate { site ->
            site to
                    toString(
                            site,
                            playerPositions.filterValues { it == site }.keys,
                            maxAdventurersOnSingleSitePerColumn.getValue(site.position.x),
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
            adventurers: Set<Adventurer>? = null,
            maxAdventurersOnSingleSiteInColumn: Int = 0,
            padToAccommodateLocationNameLength: Int? = null,
            floodState: LocationFloodState? = null
    ): String {
        val adventurerString = when {
            adventurers == null -> ""
            maxAdventurersOnSingleSiteInColumn == 0 -> ""
            adventurers.none() -> " ".repeat(maxAdventurersOnSingleSiteInColumn + 1)
            else -> "%${maxAdventurersOnSingleSiteInColumn}s>"
                        .format(adventurers.map { if (it == Adventurer.Explorer) 'X' else it.name[0] }.sorted().joinToString(""))
        }
        val floodStateString = floodState?.let { when (it) { Sunken -> "**"; Flooded -> "* "; else -> "  "} } ?: ""
        return adventurerString +
            ((if (padToAccommodateLocationNameLength == null) "%s~%s" else "%s:%-${padToAccommodateLocationNameLength + 2}s")
                    .format(mapSite.position, mapSite.location.toString() + floodStateString))
    }

}