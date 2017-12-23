package io.github.gryjo.halite

import io.github.gryjo.halite.core.*
import io.github.gryjo.halite.ext.findBestMinePlanet
import io.github.gryjo.halite.ext.findNearestEnemyShip


fun main(args: Array<String>) {
    val networking = Networking()
    val gameMap = networking.initialize("Gryjo V1.4")
    var turn = 0

    val moveList = mutableListOf<Move>()
    while (true) {
        moveList.clear()
        gameMap.updateMap(Networking.readLineIntoMetadata())

        Log.log("Turn: $turn")

        gameMap.myPlayer.ships.values
                .forEach { ship ->
                    val planet = findBestMinePlanet(gameMap.myPlayerId, ship, gameMap.allPlanets)

                    val move = if (planet != null) {
                        mine(gameMap, ship, planet)
                    } else {
                        attack(gameMap, ship)
                    }

                    move?.let {
                        moveList.add(it)
                    }
                }
        Networking.sendMoves(moveList)
        turn++
    }
}

fun mine(gameMap: GameMap, ship: Ship, planet: Planet) : Move? {
    Log.log("Ship (${ship.id}) -m> Planet (${planet.id}, ${planet.dockedShips.size})")
    return if (ship.canDock(planet)) {
        DockMove(ship, planet)
    } else {
        Navigation(ship, planet).navigateToDock(gameMap, Constants.MAX_SPEED / 2)
    }
}

fun attack(gameMap: GameMap, ship: Ship) : Move? {
    val enemyShip = findNearestEnemyShip(gameMap.myPlayerId, ship, gameMap.allShips)
    return if (enemyShip != null) {
        Log.log("Own Ship (${ship.id}) -a> Enemy Ship (${ship.id}, ${ship.dockedPlanet})")
        Navigation(ship, enemyShip).navigateToDock(gameMap, Constants.MAX_SPEED / 2)
    } else {
        null
    }
}
