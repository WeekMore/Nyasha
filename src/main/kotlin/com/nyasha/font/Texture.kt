package com.nyasha.font

import net.minecraft.util.Identifier
import java.util.*

class Texture {
    val id: Identifier

    constructor(path: String) {
        id = Identifier.of("nyasha", validatePath(path))
    }

    constructor(i: Identifier) {
        id = Identifier.of(i.namespace, i.path)
    }

    private fun validatePath(path: String): String {
        if (Identifier.isPathValid(path)) {
            return path
        }
        val ret = StringBuilder()
        for (c in path.lowercase(Locale.getDefault()).toCharArray()) {
            if (Identifier.isPathCharacterValid(c)) {
                ret.append(c)
            }
        }
        return ret.toString()
    }
}