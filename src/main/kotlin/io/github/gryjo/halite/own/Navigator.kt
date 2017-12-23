package io.github.gryjo.halite.own

import io.github.gryjo.halite.core.*

/**
 * Created on 23.12.17.
 */

class Navigator(val gameMap: GameMap) {

    val angularStepRad = Math.PI / 180.0

    val moves: MutableList<Move> = mutableListOf()

    fun navigateToEntity(ship: Ship, entity: Entity) {
        val move: Move?

        when (entity) {
            is Ship -> {
                Log.log("Own Ship (${ship.id}) -a> Enemy Ship (${entity.id}, ${entity.dockedPlanet})")
                move = Navigation(ship, entity)
                        .navigateTowards(
                                gameMap,
                                Position(entity.xPos - (Constants.WEAPON_RADIUS / 2), entity.yPos - (Constants.WEAPON_RADIUS / 2)),
                                Constants.MAX_SPEED,
                                true,
                                Constants.MAX_NAVIGATION_CORRECTIONS,
                                angularStepRad
                        )
            }
            is Planet -> {
                Log.log("Ship (${ship.id}) -m> Planet (${entity.id}, ${entity.dockedShips.size})")
                move = if (ship.canDock(entity)) {
                    DockMove(ship, entity)
                } else {
                    Navigation(ship, entity).navigateToDock(gameMap, Constants.MAX_SPEED)
                }
            }
            else -> move = null
        }

        move?.let { moves.add(it) }
    }

    fun update() {
        Networking.sendMoves(moves)
        moves.clear()
    }
}