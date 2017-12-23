package io.github.gryjo.halite.core

import java.util.Collections

class Player(val id: Int, ships: Map<Int, Ship>) {
    val ships: Map<Int, Ship> = Collections.unmodifiableMap(ships)
}
