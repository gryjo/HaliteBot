package io.github.gryjo.halite.ext

import io.github.gryjo.halite.core.Log
import io.github.gryjo.halite.core.Planet
import io.github.gryjo.halite.core.Ship

/**
 * Created on 23.12.17.
 */

fun findBestMinePlanet(playerId: Int, ship: Ship, planets: Map<Int, Planet>) : Planet? {
    val nonEnemy = planets.values.filter { it.owner == -1 || it.owner == playerId }
    var distMap = nonEnemy
            .filter { it.dockedShips.isEmpty() }
            .associateBy { ship.getDistanceTo(it) }.toSortedMap()
    if (distMap.size == 0) {
        distMap = nonEnemy
                .filter { !it.isFull }
                .associateBy { ship.getDistanceTo(it) }.toSortedMap()
    }
    return distMap.values.firstOrNull()
}

fun findNearestEnemyShip(playerId: Int, ship: Ship, ships: List<Ship>) : Ship? {
    val enemy = ships.filter { !(it.owner == -1 || it.owner == playerId) }
    val distMap = enemy.associateBy { ship.getDistanceTo(it) }.toSortedMap()
    return distMap.values.firstOrNull()
}