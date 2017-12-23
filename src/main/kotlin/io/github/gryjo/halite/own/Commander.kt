package io.github.gryjo.halite.own

import io.github.gryjo.halite.core.Entity
import io.github.gryjo.halite.core.GameMap
import io.github.gryjo.halite.core.Planet
import io.github.gryjo.halite.core.Ship

/**
 * Created on 23.12.17.
 */

class Commander(val gameMap: GameMap) {

    val ownId: Int = gameMap.myPlayerId
    val playerCount: Int = gameMap.players.size


    inline fun Entity.isOwn(): Boolean = this.owner == ownId
    inline fun Entity.isFree(): Boolean = this.owner == -1

    fun findBestObject(ship: Ship) : Entity? {
        return findBestMinePlanet(ship) ?: findNearestEnemyShip(ship)
    }

    fun findBestMinePlanet(ship: Ship, maxShipsPerPlanet: Int = 1) : Planet? {
        val nonEnemy = gameMap.planets.values.filter { it.isOwn() || it.isFree()  }
        val distMap = nonEnemy
                    .filter { !it.isFull }
                    .associateBy { ship.getDistanceTo(it) }.toSortedMap()
        return distMap.values.firstOrNull()
    }

    fun findNearestEnemyShip(ship: Ship) : Ship? {
        val enemy = gameMap.allShips.filter { !it.isOwn() }
        val distMap = enemy.associateBy { ship.getDistanceTo(it) }.toSortedMap()
        return distMap.values.firstOrNull()
    }
}