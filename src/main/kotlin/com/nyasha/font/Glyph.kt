package com.nyasha.font


internal data class Glyph(val u: Int, val v: Int, val width: Int, val height: Int, val value: Char, val owner: GlyphMap)