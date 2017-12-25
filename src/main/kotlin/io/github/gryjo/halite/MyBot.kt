package io.github.gryjo.halite

import io.github.gryjo.halite.core.DockingStatus
import io.github.gryjo.halite.core.Log
import io.github.gryjo.halite.core.Networking
import io.github.gryjo.halite.own.Commander
import io.github.gryjo.halite.own.Navigator


fun main(args: Array<String>) {
    val networking = Networking()
    val gameMap = networking.initialize("Gryjo V2.1")

    val commander = Commander(gameMap)
    val navigator = Navigator(gameMap)
    var turn = 0


    while (true) {
        gameMap.updateMap(Networking.readLineIntoMetadata())

        Log.log("Turn: $turn")
        val freeShips = gameMap.myPlayer.ships.values.filter { it.dockingStatus == DockingStatus.Undocked }
        val targets = commander.doTurn(freeShips)
        navigator.addMovementTargets(targets)
        navigator.update()
        turn++
    }
}



