package com.nyasha.font

import com.mojang.blaze3d.systems.RenderSystem
import com.nyasha.inject.accessor.INativeImage
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import org.lwjgl.system.MemoryUtil
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

internal class GlyphMap(
    val fromIncl: Char,
    val toExcl: Char,
    val font: Font,
    val bindToTexture: Identifier,
    val pixelPadding: Int
) {
    private val glyphs = Char2ObjectArrayMap<Glyph>()
    var width: Int = 0
    var height: Int = 0

    var generated: Boolean = false

    fun getGlyph(c: Char): Glyph {
        if (!generated) {
            generate()
        }
        return glyphs[c]
    }

    fun destroy() {
        MinecraftClient.getInstance().textureManager.destroyTexture(this.bindToTexture)
        glyphs.clear()
        this.width = -1
        this.height = -1
        generated = false
    }

    fun contains(c: Char): Boolean {
        return c >= fromIncl && c < toExcl
    }

    private fun getFontForGlyph(c: Char): Font {
        if (font.canDisplay(c)) {
            return font
        }
        return this.font // no font can display it, so it doesn't matter which one we pick; it'll always be missing
    }

    fun generate() {
        if (generated) {
            return
        }
        val range = toExcl.code - fromIncl.code - 1
        val charsVert = (ceil(sqrt(range.toDouble())) * 1.5).toInt() // double as many chars wide as high
        glyphs.clear()
        var generatedChars = 0
        var charNX = 0
        var maxX = 0
        var maxY = 0
        var currentX = 0
        var currentY = 0
        var currentRowMaxY = 0
        val glyphs1: MutableList<Glyph> = ArrayList()
        val af = AffineTransform()
        val frc = FontRenderContext(af, true, false)
        while (generatedChars <= range) {
            val currentChar = (fromIncl.code + generatedChars).toChar()
            val font = getFontForGlyph(currentChar)
            val stringBounds = font.getStringBounds(currentChar.toString(), frc)

            val width = ceil(stringBounds.width).toInt()
            val height = ceil(stringBounds.height).toInt()
            generatedChars++
            maxX = max(maxX.toDouble(), (currentX + width).toDouble()).toInt()
            maxY = max(maxY.toDouble(), (currentY + height).toDouble()).toInt()
            if (charNX >= charsVert) {
                currentX = 0
                currentY += currentRowMaxY + pixelPadding // add height of highest glyph, and reset
                charNX = 0
                currentRowMaxY = 0
            }
            currentRowMaxY =
                max(currentRowMaxY.toDouble(), height.toDouble()).toInt() // calculate the highest glyph in this row
            glyphs1.add(Glyph(currentX, currentY, width, height, currentChar, this))
            currentX += width + pixelPadding
            charNX++
        }
        val bi = BufferedImage(
            max((maxX + pixelPadding).toDouble(), 1.0).toInt(), max((maxY + pixelPadding).toDouble(), 1.0)
                .toInt(),
            BufferedImage.TYPE_INT_ARGB
        )
        width = bi.width
        height = bi.height
        val g2d = bi.createGraphics()
        g2d.color = Color(255, 255, 255, 0)
        g2d.fillRect(0, 0, width, height)
        g2d.color = Color.WHITE

        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        for (glyph in glyphs1) {
            g2d.font = getFontForGlyph(glyph.value)
            val fontMetrics = g2d.fontMetrics
            g2d.drawString(glyph.value.toString(), glyph.u, glyph.v + fontMetrics.ascent)
            glyphs.put(glyph.value, glyph)
        }
        registerBufferedImageTexture(bindToTexture, bi)
        generated = true
    }

    companion object {
        fun registerBufferedImageTexture(i: Identifier?, bi: BufferedImage) {
            try {
                // argb from BufferedImage is little endian, alpha is actually where the `a` is in the label
                // rgba from NativeImage (and by extension opengl) is big endian, alpha is on the other side (abgr)
                // thank you opengl
                val ow = bi.width
                val oh = bi.height
                val image = NativeImage(NativeImage.Format.RGBA, ow, oh, false)
                val ptr = (image as INativeImage).pointer
                val backingBuffer = MemoryUtil.memIntBuffer(ptr, image.width * image.height)
                val off = 0
                val _d: Any
                val _ra = bi.raster
                val _cm = bi.colorModel
                val nbands = _ra.numBands
                val dataType = _ra.dataBuffer.dataType
                _d = when (dataType) {
                    DataBuffer.TYPE_BYTE -> ByteArray(nbands)
                    DataBuffer.TYPE_USHORT -> ShortArray(nbands)
                    DataBuffer.TYPE_INT -> IntArray(nbands)
                    DataBuffer.TYPE_FLOAT -> FloatArray(nbands)
                    DataBuffer.TYPE_DOUBLE -> DoubleArray(nbands)
                    else -> throw IllegalArgumentException(
                        "Unknown data buffer type: " +
                                dataType
                    )
                }

                for (y in 0 until oh) {
                    for (x in 0 until ow) {
                        _ra.getDataElements(x, y, _d)
                        val a = _cm.getAlpha(_d)
                        val r = _cm.getRed(_d)
                        val g = _cm.getGreen(_d)
                        val b = _cm.getBlue(_d)
                        val abgr = a shl 24 or (b shl 16) or (g shl 8) or r
                        backingBuffer.put(abgr)
                    }
                }
                val tex = NativeImageBackedTexture(image)
                tex.upload()
                if (RenderSystem.isOnRenderThread()) {
                    MinecraftClient.getInstance().textureManager.registerTexture(i, tex)
                } else {
                    RenderSystem.recordRenderCall {
                        MinecraftClient.getInstance().textureManager.registerTexture(
                            i,
                            tex
                        )
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}