package io.github.gryjo.halite.own

import io.github.gryjo.halite.core.*

/**
 * Created on 23.12.17.
 */

class Commander(val gameMap: GameMap) {

    enum class EntityType {
        PLANET, SHIP
    }

    val SLOT_WEIGHT = gameMap.width * 0.08
    val SHIP_PENALTY = gameMap.width * 0.05
    val DOCKED_WEIGHT = 5

    var turn: Int = 0

    val targetMap: MutableMap<Int, Pair<EntityType, Int>> = HashMap()

    init {
        Log.log("[STATS]")
        Log.log("#Players: ${gameMap.players.size}")
        Log.log("#Planets: ${gameMap.planets.size}")
        Log.log("w: ${gameMap.width} / h: ${gameMap.height}")
        Log.log("SLOT_WEIGHT: $SLOT_WEIGHT")
        Log.log("SHIP_PENALTY: $SHIP_PENALTY")
        Log.log("-".repeat(20))
    }

    fun doTurn(ships: List<Ship>) : Map<Ship, Entity> {
        ships
                .filter { !targetMap.contains(it.id) || !isValidTarget(targetMap[it.id]!!)}
                .forEach {
                    val entity = findBestObject(it)!!
                    Log.log("Ship (${it.id}) ==> Entity(${entity.id})")
                    targetMap.put(it.id, Pair(if (entity is Ship) EntityType.SHIP else EntityType.PLANET, entity.id))
                }


        val lostShips = HashSet(targetMap.keys)
        lostShips.removeAll(ships.map { it.id })
        lostShips.forEach { targetMap.remove(it) }

        turn++
        return targetMap.mapKeys {
            gameMap.myPlayer.ships[it.key]!!
        }.mapValues {
            entityForId(it.value)!!
        }
    }

    fun entityForId(pair: Pair<EntityType, Int>) : Entity? {
        return when(pair.first) {
            EntityType.PLANET -> gameMap.planets[pair.second]
            EntityType.SHIP -> gameMap.allShips.firstOrNull { it.id == pair.second }
        }
    }

    fun isValidTarget(pair: Pair<EntityType, Int>) : Boolean {
        val entity = entityForId(pair)
        return when(entity) {
            is Planet -> entity.isFree() || (entity.isOwn() && entity.freeSlots() > 0)
            else -> false
        }
    }

    fun findBestObject(ship: Ship) : Entity? {
        return scoredEntities(ship).maxBy { it.key }?.value!!
    }

    fun scoredEntities(ship: Ship) : MutableMap<Double, Entity> {
        val entities = gameMap.nearbyEntitiesByDistance(ship)
        val maxDist = entities.maxBy { it.key }?.key!!
        return entities.values.associateBy { scoreEntity(ship, it, maxDist) }.toSortedMap(Comparator<Double> { o1, o2 -> o2.compareTo(o1) })
    }

    fun scoreEntity(ship: Ship, other: Entity, maxDist: Double) : Double {
        return when (other) {
            is Ship -> scoreShip(ship, other, maxDist)
            is Planet -> scorePlanet(ship, other, maxDist)
            else -> Double.MIN_VALUE
        }
    }

    fun scorePlanet(ship: Ship, planet: Planet, maxDist: Double) : Double {
        val dist = ship.getDistanceTo(planet)
        val free = planet.isFree()
        val own = planet.isOwn()
        val freeSlots = planet.freeSlots() - targetMap.values.filter { it.first == EntityType.PLANET }.filter { it.second == planet.id }.size

        return when {
            free -> ((maxDist - dist) + (planet.dockingSpots * SLOT_WEIGHT))
            own -> if (freeSlots > 0) ((maxDist - dist) + (freeSlots * SLOT_WEIGHT)) else Double.MIN_VALUE
            else -> Double.MIN_VALUE
        }
    }

    fun scoreShip(ownShip: Ship, enemyShip: Ship, maxDist: Double) : Double {
        val dist = ownShip.getDistanceTo(enemyShip)
        val own = enemyShip.isOwn()

        return when {
            own -> Double.MIN_VALUE
            else -> ((maxDist - dist) - SHIP_PENALTY) * (Constants.BASE_SHIP_HEALTH / ownShip.health) + if (enemyShip.dockingStatus == DockingStatus.Docked) DOCKED_WEIGHT else 0
        }
    }

    val ownId: Int = gameMap.myPlayerId

    inline fun Entity.isOwn(): Boolean = this.owner == ownId
    inline fun Entity.isEnemy(): Boolean = !this.isOwn() && !this.isFree()
    inline fun Entity.isFree(): Boolean = this.owner == -1

    inline fun Planet.freeSlots(): Int = this.dockingSpots - this.dockedShips.size
}