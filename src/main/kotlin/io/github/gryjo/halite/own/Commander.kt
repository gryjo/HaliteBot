package io.github.gryjo.halite.own

import io.github.gryjo.halite.core.*

/**
 * Created on 23.12.17.
 */

class Commander(val gameMap: GameMap) {

    val SLOT_WEIGHT = 20
    val OWN_WEIGHT = 20

    var turn: Int = 0

    val targetMap: MutableMap<Int, Entity> = HashMap()

    fun doTurn(ships: List<Ship>) : Map<Ship, Entity> {
        ships
                .filter { !targetMap.contains(it.id) || !isValidTarget(targetMap[it.id]!!)}
                .forEach {
                    val entity = findBestObject(it)!!
                    Log.log("Ship (${it.id}) ==> Entity(${entity.id})")
                    targetMap.put(it.id, entity)
                }


        val lostShips = HashSet(targetMap.keys)
        lostShips.removeAll(ships.map { it.id })
        lostShips.forEach { targetMap.remove(it) }

        turn++
        return targetMap.mapKeys {
            gameMap.myPlayer.ships[it.key]!!
        }
    }

    fun isValidTarget(entity: Entity) : Boolean {
        return when(entity) {
            is Ship -> gameMap.allShips.any { it.id == entity.id }
            is Planet -> {
                val planet = gameMap.planets[entity.id]!!
                planet.isFree() || (planet.isOwn() && planet.freeSlots() > 0)
            }
            else -> false
        }
    }

    fun findBestObject(ship: Ship) : Entity? {
        return findBestPlanet(ship) ?: findNearestEnemyShip(ship)
    }

    fun findBestPlanet(ship: Ship) : Planet? {
        val planets = scoredPlanets(ship)
        if (planets.isEmpty()) return null

        val targets = planets
                .filter { it.key > 0 }
                .values
                    .filter {
                        val planet = it
                        (it.freeSlots() - targetMap.values.filter { it is Planet }.filter { it.id == planet.id }.size) > 0
                    }

        return targets.firstOrNull()

    }

    fun scoredPlanets(entity: Entity) : MutableMap<Double, Planet> {
        val planets = gameMap.allPlanets.values.filter { !(it.isOwn() && it.isFull) && !it.isEnemy()}
        val maxDist = gameMap.nearbyEntitiesByDistance(entity).maxBy { it.key }?.key
        maxDist?.let {
            return planets.associateBy { scorePlanet(entity, it, maxDist) }.toSortedMap(Comparator<Double> { o1, o2 -> o2.compareTo(o1) })
        }
        return HashMap()
    }

    fun scorePlanet(entity: Entity, planet: Planet, maxDist: Double) : Double {
        val dist = entity.getDistanceTo(planet)
        val free = planet.isFree()
        val own = planet.isOwn()
        val slots = planet.dockingSpots

        return when {
            free -> (maxDist - dist) + (slots * SLOT_WEIGHT)
            own -> ((maxDist - dist) + (slots * SLOT_WEIGHT) + OWN_WEIGHT)
            else -> -dist
        }
    }

    fun findNearestEnemyShip(ship: Ship) : Ship? {
        val enemy = gameMap.allShips.filter { !it.isOwn() }
        val distMap = enemy.associateBy { ship.getDistanceTo(it) }.toSortedMap()
        return distMap.values.firstOrNull()
    }



    val ownId: Int = gameMap.myPlayerId

    inline fun Entity.isOwn(): Boolean = this.owner == ownId
    inline fun Entity.isEnemy(): Boolean = !this.isOwn() && !this.isFree()
    inline fun Entity.isFree(): Boolean = this.owner == -1

    inline fun Planet.freeSlots(): Int = this.dockingSpots - this.dockedShips.size
}