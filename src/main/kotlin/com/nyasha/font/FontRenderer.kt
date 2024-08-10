package com.nyasha.font

import com.mojang.blaze3d.systems.RenderSystem
import com.nyasha.util.IMinecraft
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap
import it.unimi.dsi.fastutil.chars.Char2ObjectFunction
import it.unimi.dsi.fastutil.objects.*
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.jetbrains.annotations.Contract
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Font
import java.io.Closeable
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.floor
import kotlin.math.max

class FontRenderer @JvmOverloads constructor(
    font: Font?,
    private val originalSize: Float,
    private val charsPerPage: Int = 256,
    private val padding: Int = 5,
    private val prebakeGlyphs: String? = null
) : Closeable, IMinecraft {
    private val GLYPH_PAGE_CACHE: Object2ObjectMap<Identifier, ObjectList<DrawEntry>> = Object2ObjectOpenHashMap()
    private val maps: ObjectList<GlyphMap> = ObjectArrayList()
    private val allGlyphs = Char2ObjectArrayMap<Glyph>()
    private var scaleMul = 0
    private var font: Font? = null
    private var previousGameScale = -1
    private var prebakeGlyphsFuture: Future<Void?>? = null
    private var initialized = false

    init {
        init(font, originalSize)
    }

    private fun sizeCheck() {
        val gs = mc.window.scaleFactor.toInt()
        if (gs != this.previousGameScale) {
            close()
            init(this.font, this.originalSize)
        }
    }

    private fun init(font: Font?, sizePx: Float) {
        check(!initialized) { "Double call to init()" }
        initialized = true
        this.previousGameScale = mc.window.scaleFactor.toInt()
        this.scaleMul = this.previousGameScale
        this.font = font!!.deriveFont(sizePx * this.scaleMul)
        if (prebakeGlyphs != null && !prebakeGlyphs.isEmpty()) {
            prebakeGlyphsFuture = this.prebake()
        }
    }

    private fun prebake(): Future<Void?> {
        return ASYNC_WORKER.submit<Void?> {
            for (c in prebakeGlyphs!!.toCharArray()) {
                if (Thread.interrupted()) break
                locateGlyph1(c)
            }
            null
        }
    }

    private fun generateMap(from: Char, to: Char): GlyphMap {
        val gm = GlyphMap(
            from, to,
            font!!, randomIdentifier(), padding
        )
        maps.add(gm)
        return gm
    }

    private fun locateGlyph0(glyph: Char): Glyph {
        for (map in maps) {
            if (map.contains(glyph)) {
                return map.getGlyph(glyph)
            }
        }
        val base = floorNearestMulN(glyph.code, charsPerPage)
        val glyphMap = generateMap(base.toChar(), (base + charsPerPage).toChar())
        return glyphMap.getGlyph(glyph)
    }

    private fun locateGlyph1(glyph: Char): Glyph? {
        return allGlyphs.computeIfAbsent(glyph, Char2ObjectFunction { glyph: Char ->
            this.locateGlyph0(
                glyph.toChar()
            )
        })
    }

    fun drawString(stack: MatrixStack, s: String, x: Double, y: Double, color: Int) {
        val r = ((color shr 16) and 0xff) / 255f
        val g = ((color shr 8) and 0xff) / 255f
        val b = ((color) and 0xff) / 255f
        val a = ((color shr 24) and 0xff) / 255f
        drawString(stack, s, x.toFloat(), y.toFloat(), r, g, b, a)
    }

    fun drawString(stack: MatrixStack, s: String, x: Double, y: Double, color: Color) {
        drawString(
            stack,
            s,
            x.toFloat(),
            y.toFloat(),
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )
    }

    fun drawString(stack: MatrixStack, s: String, x: Float, y: Float, color: Color) {
        drawString(stack, s, x, y, color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
    }

    fun drawString(
        stack: MatrixStack,
        s: String,
        x: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        gradientColor: Color,
        offset: Int
    ) {
        var y = y
        var a = a
        if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture!!.isDone) {
            try {
                prebakeGlyphsFuture!!.get()
            } catch (ignored: InterruptedException) {
            } catch (ignored: ExecutionException) {
            }
        }
        sizeCheck()
        var r2 = r
        var g2 = g
        var b2 = b
        stack.push()
        y -= 3f
        stack.translate(x, y, 0f)
        stack.scale(1f / this.scaleMul, 1f / this.scaleMul, 1f)

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        //RenderSystem.disableCull();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        RenderSystem.setShader { GameRenderer.getPositionTexColorProgram() }
        val mat = stack.peek().positionMatrix
        val chars = s.toCharArray()
        var xOffset = 0f
        var yOffset = 0f
        var inSel = false
        var lineStart = 0
        synchronized(GLYPH_PAGE_CACHE) {
            for (i in chars.indices) {
                val c = chars[i]
                if (inSel) {
                    inSel = false
                    val c1 = c.uppercaseChar()
                    if (colorCodes.containsKey(c1)) {
                        val ii = colorCodes[c1]
                        val col = RGBIntToRGB(ii)
                        r2 = col[0] / 255f
                        g2 = col[1] / 255f
                        b2 = col[2] / 255f
                    } else if (c1 == 'R') {
                        r2 = r
                        g2 = g
                        b2 = b
                    }
                    continue
                }



                r2 = gradientColor.red / 255f
                g2 = gradientColor.green / 255f
                b2 = gradientColor.blue / 255f
                a = gradientColor.alpha / 255f


                if (c == '§') {
                    inSel = true
                    continue
                } else if (c == '\n') {
                    yOffset += getStringHeight(s.substring(lineStart, i)) * scaleMul
                    xOffset = 0f
                    lineStart = i + 1
                    continue
                }
                val glyph = locateGlyph1(c)
                if (glyph != null) {
                    if (glyph.value != ' ') {
                        val i1 = glyph.owner.bindToTexture
                        val entry = DrawEntry(xOffset, yOffset, r2, g2, b2, glyph)
                        GLYPH_PAGE_CACHE.computeIfAbsent(
                            i1,
                            Object2ObjectFunction<Identifier, ObjectList<DrawEntry>> { integer: Any? -> ObjectArrayList() })
                            .add(entry)
                    }
                    xOffset += glyph.width.toFloat()
                }
            }
            for (identifier in GLYPH_PAGE_CACHE.keys) {
                RenderSystem.setShaderTexture(0, identifier)
                val objects: List<DrawEntry> = GLYPH_PAGE_CACHE[identifier]!!

                if (!objects.isEmpty()) {
                    val bb = Tessellator.getInstance()
                        .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
                    for ((xo, yo, cr, cg, cb, glyph) in objects) {
                        val owner = glyph.owner
                        val w = glyph.width.toFloat()
                        val h = glyph.height.toFloat()
                        val u1 = glyph.u.toFloat() / owner.width
                        val v1 = glyph.v.toFloat() / owner.height
                        val u2 = (glyph.u + glyph.width).toFloat() / owner.width
                        val v2 = (glyph.u + glyph.height).toFloat() / owner.height

                        bb.vertex(mat, xo + 0, yo + h, 0f).texture(u1, v2).color(cr, cg, cb, a)
                        bb.vertex(mat, xo + w, yo + h, 0f).texture(u2, v2).color(cr, cg, cb, a)
                        bb.vertex(mat, xo + w, yo + 0, 0f).texture(u2, v1).color(cr, cg, cb, a)
                        bb.vertex(mat, xo + 0, yo + 0, 0f).texture(u1, v1).color(cr, cg, cb, a)
                    }
                    BufferRenderer.drawWithGlobalProgram(bb.end())
                }
            }
            GLYPH_PAGE_CACHE.clear()
        }
        stack.pop()
        RenderSystem.disableBlend()
    }

    @JvmOverloads
    fun drawString(
        stack: MatrixStack,
        s: String,
        x: Float,
        y: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        offset: Int = 0
    ) {
        var y = y
        if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture!!.isDone) {
            try {
                prebakeGlyphsFuture!!.get()
            } catch (ignored: InterruptedException) {
            } catch (ignored: ExecutionException) {
            }
        }
        sizeCheck()
        var r2 = r
        var g2 = g
        var b2 = b
        stack.push()
        y -= 3f
        stack.translate(x, y, 0f)
        stack.scale(1f / this.scaleMul, 1f / this.scaleMul, 1f)

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        RenderSystem.setShader { GameRenderer.getPositionTexColorProgram() }
        val mat = stack.peek().positionMatrix
        val chars = s.toCharArray()
        var xOffset = 0f
        var yOffset = 0f
        var inSel = false
        var lineStart = 0
        synchronized(GLYPH_PAGE_CACHE) {
            for (i in chars.indices) {
                val c = chars[i]
                if (inSel) {
                    inSel = false
                    val c1 = c.uppercaseChar()
                    if (colorCodes.containsKey(c1)) {
                        val ii = colorCodes[c1]
                        val col = RGBIntToRGB(ii)
                        r2 = col[0] / 255f
                        g2 = col[1] / 255f
                        b2 = col[2] / 255f
                    } else if (c1 == 'R') {
                        r2 = r
                        g2 = g
                        b2 = b
                    }
                    continue
                }
                if (c == '§') {
                    inSel = true
                    continue
                } else if (c == '\n') {
                    yOffset += getStringHeight(s.substring(lineStart, i)) * scaleMul
                    xOffset = 0f
                    lineStart = i + 1
                    continue
                }
                val glyph = locateGlyph1(c)
                if (glyph != null) {
                    if (glyph.value != ' ') {
                        val i1 = glyph.owner.bindToTexture
                        val entry = DrawEntry(xOffset, yOffset, r2, g2, b2, glyph)
                        GLYPH_PAGE_CACHE.computeIfAbsent(
                            i1,
                            Object2ObjectFunction<Identifier, ObjectList<DrawEntry>> { integer: Any? -> ObjectArrayList() })
                            .add(entry)
                    }
                    xOffset += glyph.width.toFloat()
                }
            }
            for (identifier in GLYPH_PAGE_CACHE.keys) {
                RenderSystem.setShaderTexture(0, identifier)
                val objects: List<DrawEntry> = GLYPH_PAGE_CACHE[identifier]!!

                if (!objects.isEmpty()) {
                    val bb = Tessellator.getInstance()
                        .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
                    for ((xo, yo, cr, cg, cb, glyph) in objects) {
                        val owner = glyph.owner
                        val w = glyph.width.toFloat()
                        val h = glyph.height.toFloat()
                        val u1 = glyph.u.toFloat() / owner.width
                        val v1 = glyph.v.toFloat() / owner.height
                        val u2 = (glyph.u + glyph.width).toFloat() / owner.width
                        val v2 = (glyph.v + glyph.height).toFloat() / owner.height

                        bb.vertex(mat, xo + 0, yo + h, 0f).texture(u1, v2).color(cr, cg, cb, a)
                        bb.vertex(mat, xo + w, yo + h, 0f).texture(u2, v2).color(cr, cg, cb, a)
                        bb.vertex(mat, xo + w, yo + 0, 0f).texture(u2, v1).color(cr, cg, cb, a)
                        bb.vertex(mat, xo + 0, yo + 0, 0f).texture(u1, v1).color(cr, cg, cb, a)
                    }
                    BufferRenderer.drawWithGlobalProgram(bb.end())
                }
            }
            GLYPH_PAGE_CACHE.clear()
        }

        stack.pop()
        RenderSystem.disableBlend()
    }

    fun drawCenteredString(stack: MatrixStack, s: String, x: Double, y: Double, color: Int) {
        val r = ((color shr 16) and 0xff) / 255f
        val g = ((color shr 8) and 0xff) / 255f
        val b = ((color) and 0xff) / 255f
        val a = ((color shr 24) and 0xff) / 255f
        drawString(stack, s, (x - getStringWidth(s) / 2f).toFloat(), y.toFloat(), r, g, b, a)
    }

    fun drawCenteredString(stack: MatrixStack, s: String, x: Double, y: Double, color: Color) {
        drawString(
            stack,
            s,
            (x - getStringWidth(s) / 2f).toFloat(),
            y.toFloat(),
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )
    }

    fun drawCenteredString(stack: MatrixStack, s: String, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float) {
        drawString(stack, s, x - getStringWidth(s) / 2f, y, r, g, b, a)
    }

    fun getStringWidth(text: String): Float {
        val c = stripControlCodes(text).toCharArray()
        var currentLine = 0f
        var maxPreviousLines = 0f
        for (c1 in c) {
            if (c1 == '\n') {
                maxPreviousLines = max(currentLine.toDouble(), maxPreviousLines.toDouble()).toFloat()
                currentLine = 0f
                continue
            }
            val glyph = locateGlyph1(c1)
            currentLine += if (glyph == null) 0f else (glyph.width / scaleMul.toFloat())
        }
        return max(currentLine.toDouble(), maxPreviousLines.toDouble()).toFloat()
    }

    fun getStringHeight(text: String): Float {
        var c = stripControlCodes(text).toCharArray()
        if (c.size == 0) {
            c = charArrayOf(' ')
        }
        var currentLine = 0f
        var previous = 0f
        for (c1 in c) {
            if (c1 == '\n') {
                if (currentLine == 0f) {
                    currentLine =
                        (if (locateGlyph1(' ') == null) 0f else (Objects.requireNonNull(locateGlyph1(' '))!!.height / scaleMul.toFloat()))
                }
                previous += currentLine
                currentLine = 0f
                continue
            }
            val glyph = locateGlyph1(c1)
            currentLine = max(
                (if (glyph == null) 0f else (glyph.height / scaleMul.toFloat())).toDouble(),

                currentLine.toDouble()
            ).toFloat()
        }
        return currentLine + previous
    }


    override fun close() {
        try {
            if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture!!.isDone && !prebakeGlyphsFuture!!.isCancelled) {
                prebakeGlyphsFuture!!.cancel(true)
                prebakeGlyphsFuture!!.get()
                prebakeGlyphsFuture = null
            }
            for (map in maps) {
                map.destroy()
            }
            maps.clear()
            allGlyphs.clear()
            initialized = false
        } catch (ignored: Exception) {
        }
    }

    fun getFontHeight(str: String): Float {
        return getStringHeight(str)
    }

    fun drawGradientString(
        stack: MatrixStack,
        s: String,
        x: Float,
        y: Float,
        color1: Color,
        color2: Color,
        offset: Int
    ) {
        drawString(
            stack,
            s,
            x,
            y,
            color1.red.toFloat(),
            color1.green.toFloat(),
            color1.blue.toFloat(),
            color1.alpha.toFloat(),
            color2,
            offset
        )
    }

    fun drawGradientCenteredString(
        matrices: MatrixStack,
        s: String,
        x: Float,
        y: Float,
        color1: Color,
        color2: Color,
        offset: Int
    ) {
        drawGradientString(matrices, s, x - getStringWidth(s) / 2f, y, color1, color2, offset)
    }

    @JvmRecord
    internal data class DrawEntry(
        val atX: Float,
        val atY: Float,
        val r: Float,
        val g: Float,
        val b: Float,
        val toDraw: Glyph
    )

    companion object {
        private val dcode = Char2IntArrayMap()





        private val colorCodes: Char2IntArrayMap = Char2IntArrayMap().also {
            it.put('1', 0x0000AA)
            it.put('2', 0x00AA00)
            it.put('3', 0x00AAAA)
            it.put('4', 0xAA0000)
            it.put('5', 0xAA00AA)
            it.put('6', 0xFFAA00)
            it.put('7', 0xAAAAAA)
            it.put('8', 0x555555)
            it.put('9', 0x5555FF)
            it.put('A', 0x55FF55)
            it.put('B', 0x55FFFF)
            it.put('C', 0xFF5555)
            it.put('D', 0xFF55FF)
            it.put('E', 0xFFFF55)
            it.put('F', 0xFFFFFF)
        }





        private val ASYNC_WORKER: ExecutorService = Executors.newCachedThreadPool()
        private fun floorNearestMulN(x: Int, n: Int): Int {
            return n * floor(x.toDouble() / n.toDouble()).toInt()
        }

        fun stripControlCodes(text: String): String {
            val chars = text.toCharArray()
            val f = StringBuilder()
            var i = 0
            while (i < chars.size) {
                val c = chars[i]
                if (c == '§') {
                    i++
                    i++
                    continue
                }
                f.append(c)
                i++
            }
            return f.toString()
        }

        @Contract(value = "-> new", pure = true)
        fun randomIdentifier(): Identifier {
            return Identifier.of("thunderhack", "temp/" + randomString())
        }

        private fun randomString(): String {
            return IntStream.range(0, 32)
                .mapToObj { operand: Int -> Random().nextInt('a'.code, 'z'.code + 1).toChar().toString() }
                .collect(Collectors.joining())
        }

        @Contract(value = "_ -> new", pure = true)
        fun RGBIntToRGB(`in`: Int): IntArray {
            val red = `in` shr 8 * 2 and 0xFF
            val green = `in` shr 8 and 0xFF
            val blue = `in` and 0xFF
            return intArrayOf(red, green, blue)
        }
    }
}