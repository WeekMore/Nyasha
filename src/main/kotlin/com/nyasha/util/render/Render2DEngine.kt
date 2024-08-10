package com.nyasha.util.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.nyasha.font.Texture
import com.nyasha.util.IMinecraft
import com.nyasha.util.math.ColorUtility.darker
import com.nyasha.util.math.ColorUtility.injectAlpha
import com.nyasha.util.math.MathUtility.clamp
import com.nyasha.util.render.shaders.BlurProgram
import net.minecraft.client.render.*
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.MathHelper
import org.apache.commons.lang3.RandomStringUtils
import org.joml.Matrix4f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL40C
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object Render2DEngine : IMinecraft {
    private var BLUR: BlurProgram? = null

    var shadowCache: HashMap<Int, BlurredShadow> = HashMap()
    var shadowCache1: HashMap<Int, BlurredShadow> = HashMap()
    val clipStack: Stack<Rectangle> = Stack()

    fun initShaders() {
        BLUR = BlurProgram()
    }

    fun drawRoundedBlur(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        c1: Color?,
        blurStrenth: Float,
        blurOpacity: Float
    ) {
        val bb = preShaderDraw(matrices, x - 10, y - 10, width + 20, height + 20)
        BLUR!!.setParameters(x, y, width, height, radius, c1!!, blurStrenth, blurOpacity)
        BLUR!!.use()
        BufferRenderer.drawWithGlobalProgram(bb.end())
        endRender()
    }


    fun addWindow(stack: MatrixStack, r1: Rectangle) {
        val matrix = stack.peek().positionMatrix
        val coord = Vector4f(r1.x, r1.y, 0f, 1f)
        val end = Vector4f(r1.x1, r1.y1, 0f, 1f)
        coord.mulTranspose(matrix)
        end.mulTranspose(matrix)
        val x = coord.x()
        val y = coord.y()
        val endX = end.x()
        val endY = end.y()
        val r = Rectangle(x, y, endX, endY)
        if (clipStack.empty()) {
            clipStack.push(r)
            beginScissor(r.x.toDouble(), r.y.toDouble(), r.x1.toDouble(), r.y1.toDouble())
        } else {
            val lastClip = clipStack.peek()
            val lsx = lastClip.x
            val lsy = lastClip.y
            val lstx = lastClip.x1
            val lsty = lastClip.y1
            val nsx = MathHelper.clamp(r.x, lsx, lstx)
            val nsy = MathHelper.clamp(r.y, lsy, lsty)
            val nstx = MathHelper.clamp(r.x1, nsx, lstx)
            val nsty = MathHelper.clamp(r.y1, nsy, lsty)
            clipStack.push(Rectangle(nsx, nsy, nstx, nsty))
            beginScissor(nsx.toDouble(), nsy.toDouble(), nstx.toDouble(), nsty.toDouble())
        }
    }

    fun popWindow() {
        clipStack.pop()
        if (clipStack.empty()) {
            endScissor()
        } else {
            val r = clipStack.peek()
            beginScissor(r.x.toDouble(), r.y.toDouble(), r.x1.toDouble(), r.y1.toDouble())
        }
    }

    fun beginScissor(x: Double, y: Double, endX: Double, endY: Double) {
        var width = endX - x
        var height = endY - y
        width = max(0.0, width)
        height = max(0.0, height)
        val ay =
            (mc.window.scaledHeight - (y + height)).toInt()
        RenderSystem.enableScissor(x.toInt(), ay, width.toInt(), height.toInt())
    }

    fun endScissor() {
        RenderSystem.disableScissor()
    }

    fun addWindow(stack: MatrixStack, x: Float, y: Float, x1: Float, y1: Float, animation_factor: Double) {
        val h = y + y1
        val h2 = (h * (1.0 - clamp(animation_factor, 0.0, 1.0025))).toFloat()

        val x3 = x
        val y3 = y + h2
        var x4 = x1
        var y4 = y1 - h2

        if (x4 < x3) x4 = x3
        if (y4 < y3) y4 = y3
        addWindow(stack, Rectangle(x3, y3, x4, y4))
    }

    fun horizontalGradient(
        matrices: MatrixStack,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        startColor: Color,
        endColor: Color
    ) {
        val matrix = matrices.peek().positionMatrix
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        buffer.vertex(matrix, x1, y1, 0.0f).color(startColor.rgb)
        buffer.vertex(matrix, x1, y2, 0.0f).color(startColor.rgb)
        buffer.vertex(matrix, x2, y2, 0.0f).color(endColor.rgb)
        buffer.vertex(matrix, x2, y1, 0.0f).color(endColor.rgb)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
        endRender()
    }

    fun verticalGradient(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startColor: Color,
        endColor: Color
    ) {
        val matrix = matrices.peek().positionMatrix
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        buffer.vertex(matrix, left, top, 0.0f).color(startColor.rgb)
        buffer.vertex(matrix, left, bottom, 0.0f).color(endColor.rgb)
        buffer.vertex(matrix, right, bottom, 0.0f).color(endColor.rgb)
        buffer.vertex(matrix, right, top, 0.0f).color(startColor.rgb)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
        endRender()
    }

    fun drawRect(matrices: MatrixStack, x: Float, y: Float, width: Float, height: Float, c: Color) {
        val matrix = matrices.peek().positionMatrix
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        buffer.vertex(matrix, x, y + height, 0.0f).color(c.rgb)
        buffer.vertex(matrix, x + width, y + height, 0.0f).color(c.rgb)
        buffer.vertex(matrix, x + width, y, 0.0f).color(c.rgb)
        buffer.vertex(matrix, x, y, 0.0f).color(c.rgb)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
        endRender()
    }

    fun drawRectWithOutline(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        c: Color,
        c2: Color
    ) {
        val matrix = matrices.peek().positionMatrix
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        buffer.vertex(matrix, x, y + height, 0.0f).color(c.rgb)
        buffer.vertex(matrix, x + width, y + height, 0.0f).color(c.rgb)
        buffer.vertex(matrix, x + width, y, 0.0f).color(c.rgb)
        buffer.vertex(matrix, x, y, 0.0f).color(c.rgb)
        BufferRenderer.drawWithGlobalProgram(buffer.end())

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        buffer.vertex(matrix, x, y + height, 0.0f).color(c2.rgb)
        buffer.vertex(matrix, x + width, y + height, 0.0f).color(c2.rgb)
        buffer.vertex(matrix, x + width, y, 0.0f).color(c2.rgb)
        buffer.vertex(matrix, x, y, 0.0f).color(c2.rgb)
        buffer.vertex(matrix, x, y + height, 0.0f).color(c2.rgb)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
        endRender()
    }

    fun drawRectDumbWay(matrices: MatrixStack, x: Float, y: Float, x1: Float, y1: Float, c1: Color) {
        val matrix = matrices.peek().positionMatrix
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        buffer.vertex(matrix, x, y1, 0.0f).color(c1.rgb)
        buffer.vertex(matrix, x1, y1, 0.0f).color(c1.rgb)
        buffer.vertex(matrix, x1, y, 0.0f).color(c1.rgb)
        buffer.vertex(matrix, x, y, 0.0f).color(c1.rgb)
        BufferRenderer.drawWithGlobalProgram(buffer.end())
        endRender()
    }

    fun setRectPoints(
        bufferBuilder: BufferBuilder,
        matrix: Matrix4f?,
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        c1: Color,
        c2: Color,
        c3: Color,
        c4: Color
    ) {
        bufferBuilder.vertex(matrix, x, y1, 0.0f).color(c1.rgb)
        bufferBuilder.vertex(matrix, x1, y1, 0.0f).color(c2.rgb)
        bufferBuilder.vertex(matrix, x1, y, 0.0f).color(c3.rgb)
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(c4.rgb)
    }


    fun drawBlurredShadow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        color: Color
    ) {
        var x = x
        var y = y
        var width = width
        var height = height
        width = width + blurRadius * 2
        height = height + blurRadius * 2
        x = x - blurRadius
        y = y - blurRadius

        val identifier = (width * height + width * blurRadius).toInt()
        if (shadowCache.containsKey(identifier)) {
            shadowCache[identifier]!!.bind()
        } else {
            val original = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_ARGB)
            val g = original.graphics
            g.color = Color(-1)
            g.fillRect(blurRadius, blurRadius, (width - blurRadius * 2).toInt(), (height - blurRadius * 2).toInt())
            g.dispose()
            val op = GaussianFilter(blurRadius.toFloat())
            val blurred = op.filter(original, null)
            shadowCache[identifier] = BlurredShadow(blurred)
            return
        }

        setupRender()
        RenderSystem.setShaderColor(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        renderTexture(
            matrices,
            x.toDouble(),
            y.toDouble(),
            width.toDouble(),
            height.toDouble(),
            0f,
            0f,
            width.toDouble(),
            height.toDouble(),
            width.toDouble(),
            height.toDouble()
        )
        endRender()
    }

    fun drawGradientBlurredShadow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        color1: Color,
        color2: Color,
        color3: Color,
        color4: Color
    ) {
        var x = x
        var y = y
        var width = width
        var height = height
        width = width + blurRadius * 2
        height = height + blurRadius * 2
        x = x - blurRadius
        y = y - blurRadius

        val identifier = (width * height + width * blurRadius).toInt()
        if (shadowCache.containsKey(identifier)) {
            shadowCache[identifier]!!.bind()
        } else {
            val original = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_ARGB)
            val g = original.graphics
            g.color = Color(-1)
            g.fillRect(blurRadius, blurRadius, (width - blurRadius * 2).toInt(), (height - blurRadius * 2).toInt())
            g.dispose()
            val op = GaussianFilter(blurRadius.toFloat())
            val blurred = op.filter(original, null)
            shadowCache[identifier] = BlurredShadow(blurred)
            return
        }

        setupRender()
        renderGradientTexture(
            matrices,
            x.toDouble(),
            y.toDouble(),
            width.toDouble(),
            height.toDouble(),
            0f,
            0f,
            width.toDouble(),
            height.toDouble(),
            width.toDouble(),
            height.toDouble(),
            color1,
            color2,
            color3,
            color4
        )
        endRender()
    }

    fun drawGradientBlurredShadow1(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blurRadius: Int,
        color1: Color,
        color2: Color,
        color3: Color,
        color4: Color
    ) {
        var x = x
        var y = y
        var width = width
        var height = height
        width = width + blurRadius * 2
        height = height + blurRadius * 2
        x = x - blurRadius
        y = y - blurRadius

        val identifier = (width * height + width * blurRadius).toInt()
        if (shadowCache1.containsKey(identifier)) {
            shadowCache1[identifier]!!.bind()
        } else {
            val original = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_ARGB)
            val g = original.graphics
            g.color = Color(-1)
            g.fillRect(blurRadius, blurRadius, (width - blurRadius * 2).toInt(), (height - blurRadius * 2).toInt())
            g.dispose()
            val blurred = GaussianFilter(blurRadius.toFloat()).filter(original, null)

            val black = BufferedImage(
                width.toInt() + blurRadius * 2,
                height.toInt() + blurRadius * 2,
                BufferedImage.TYPE_INT_ARGB
            )
            val g2 = black.graphics
            g2.color = Color(0x000000)
            g2.fillRect(0, 0, width.toInt() + blurRadius * 2, height.toInt() + blurRadius * 2)
            g2.dispose()

            val combined = BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_ARGB)
            val g1 = combined.graphics
            g1.drawImage(black, -blurRadius, -blurRadius, null)
            g1.drawImage(blurred, 0, 0, null)
            g1.dispose()

            shadowCache1[identifier] = BlurredShadow(combined)
            return
        }

        setupRender()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE)
        renderGradientTexture(
            matrices,
            x.toDouble(),
            y.toDouble(),
            width.toDouble(),
            height.toDouble(),
            0f,
            0f,
            width.toDouble(),
            height.toDouble(),
            width.toDouble(),
            height.toDouble(),
            color1,
            color2,
            color3,
            color4
        )
        endRender()
    }


    fun renderTexture(
        matrices: MatrixStack,
        x0: Double,
        y0: Double,
        width: Double,
        height: Double,
        u: Float,
        v: Float,
        regionWidth: Double,
        regionHeight: Double,
        textureWidth: Double,
        textureHeight: Double
    ) {
        val x1 = x0 + width
        val y1 = y0 + height
        val z = 0.0
        val matrix = matrices.peek().positionMatrix
        RenderSystem.setShader { GameRenderer.getPositionTexProgram() }
        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
        buffer.vertex(matrix, x0.toFloat(), y1.toFloat(), z.toFloat())
            .texture((u) / textureWidth.toFloat(), (v + regionHeight.toFloat()) / textureHeight.toFloat())
        buffer.vertex(matrix, x1.toFloat(), y1.toFloat(), z.toFloat()).texture(
            (u + regionWidth.toFloat()) / textureWidth.toFloat(),
            (v + regionHeight.toFloat()) / textureHeight.toFloat()
        )
        buffer.vertex(matrix, x1.toFloat(), y0.toFloat(), z.toFloat())
            .texture((u + regionWidth.toFloat()) / textureWidth.toFloat(), (v) / textureHeight.toFloat())
        buffer.vertex(matrix, x0.toFloat(), y0.toFloat(), z.toFloat())
            .texture((u) / textureWidth.toFloat(), (v + 0.0f) / textureHeight.toFloat())
        BufferRenderer.drawWithGlobalProgram(buffer.end())
    }

    fun renderGradientTexture(
        matrices: MatrixStack,
        x0: Double,
        y0: Double,
        width: Double,
        height: Double,
        u: Float,
        v: Float,
        regionWidth: Double,
        regionHeight: Double,
        textureWidth: Double,
        textureHeight: Double,
        c1: Color,
        c2: Color,
        c3: Color,
        c4: Color
    ) {
        RenderSystem.setShader { GameRenderer.getPositionTexColorProgram() }
        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        renderGradientTextureInternal(
            buffer,
            matrices,
            x0,
            y0,
            width,
            height,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight,
            c1,
            c2,
            c3,
            c4
        )
        BufferRenderer.drawWithGlobalProgram(buffer.end())
    }

    fun renderGradientTextureInternal(
        buff: BufferBuilder,
        matrices: MatrixStack,
        x0: Double,
        y0: Double,
        width: Double,
        height: Double,
        u: Float,
        v: Float,
        regionWidth: Double,
        regionHeight: Double,
        textureWidth: Double,
        textureHeight: Double,
        c1: Color,
        c2: Color,
        c3: Color,
        c4: Color
    ) {
        val x1 = x0 + width
        val y1 = y0 + height
        val z = 0.0
        val matrix = matrices.peek().positionMatrix
        buff.vertex(matrix, x0.toFloat(), y1.toFloat(), z.toFloat())
            .texture((u) / textureWidth.toFloat(), (v + regionHeight.toFloat()) / textureHeight.toFloat()).color(c1.rgb)
        buff.vertex(matrix, x1.toFloat(), y1.toFloat(), z.toFloat()).texture(
            (u + regionWidth.toFloat()) / textureWidth.toFloat(),
            (v + regionHeight.toFloat()) / textureHeight.toFloat()
        ).color(c2.rgb)
        buff.vertex(matrix, x1.toFloat(), y0.toFloat(), z.toFloat())
            .texture((u + regionWidth.toFloat()) / textureWidth.toFloat(), (v) / textureHeight.toFloat()).color(c3.rgb)
        buff.vertex(matrix, x0.toFloat(), y0.toFloat(), z.toFloat())
            .texture((u) / textureWidth.toFloat(), (v + 0.0f) / textureHeight.toFloat()).color(c4.rgb)
    }

    fun renderRoundedGradientRect(
        matrices: MatrixStack,
        color1: Color,
        color2: Color,
        color3: Color,
        color4: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        Radius: Float
    ) {
        val matrix = matrices.peek().positionMatrix
        RenderSystem.colorMask(false, false, false, true)
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 0.0f)
        RenderSystem.clear(GL40C.GL_COLOR_BUFFER_BIT, false)
        RenderSystem.colorMask(true, true, true, true)

        drawRound(matrices, x, y, width, height, Radius, color1)
        setupRender()
        RenderSystem.blendFunc(GL40C.GL_DST_ALPHA, GL40C.GL_ONE_MINUS_DST_ALPHA)
        val bufferBuilder =
            Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(matrix, x, y + height, 0.0f).color(color1.rgb)
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0f).color(color2.rgb)
        bufferBuilder.vertex(matrix, x + width, y, 0.0f).color(color3.rgb)
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(color4.rgb)
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    fun drawRound(matrices: MatrixStack, x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color) {
        renderRoundedQuad(
            matrices,
            color,
            x.toDouble(),
            y.toDouble(),
            (width + x).toDouble(),
            (height + y).toDouble(),
            radius.toDouble(),
            4.0
        )
    }

    fun renderRoundedQuad(
        matrices: MatrixStack,
        c: Color,
        fromX: Double,
        fromY: Double,
        toX: Double,
        toY: Double,
        radius: Double,
        samples: Double
    ) {
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        renderRoundedQuadInternal(
            matrices.peek().positionMatrix,
            c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f,
            fromX,
            fromY,
            toX,
            toY,
            radius,
            samples
        )
        endRender()
    }

    fun renderRoundedQuad2(
        matrices: MatrixStack,
        c: Color,
        c2: Color,
        c3: Color,
        c4: Color,
        fromX: Double,
        fromY: Double,
        toX: Double,
        toY: Double,
        radius: Double
    ) {
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        renderRoundedQuadInternal2(
            matrices.peek().positionMatrix,
            c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f,
            c2.red / 255f,
            c2.green / 255f,
            c2.blue / 255f,
            c2.alpha / 255f,
            c3.red / 255f,
            c3.green / 255f,
            c3.blue / 255f,
            c3.alpha / 255f,
            c4.red / 255f,
            c4.green / 255f,
            c4.blue / 255f,
            c4.alpha / 255f,
            fromX,
            fromY,
            toX,
            toY,
            radius
        )
        endRender()
    }

    fun renderRoundedQuadInternal(
        matrix: Matrix4f?,
        cr: Float,
        cg: Float,
        cb: Float,
        ca: Float,
        fromX: Double,
        fromY: Double,
        toX: Double,
        toY: Double,
        radius: Double,
        samples: Double
    ) {
        val bufferBuilder =
            Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR)
        val map = arrayOf(
            doubleArrayOf(toX - radius, toY - radius, radius),
            doubleArrayOf(toX - radius, fromY + radius, radius),
            doubleArrayOf(fromX + radius, fromY + radius, radius),
            doubleArrayOf(fromX + radius, toY - radius, radius)
        )
        for (i in 0..3) {
            val current = map[i]
            val rad = current[2]
            var r = i * 90.0
            while (r < (360 / 4.0 + i * 90.0)) {
                val rad1 = Math.toRadians(r).toFloat()
                val sin = (sin(rad1.toDouble()) * rad).toFloat()
                val cos = (cos(rad1.toDouble()) * rad).toFloat()
                bufferBuilder.vertex(matrix, current[0].toFloat() + sin, current[1].toFloat() + cos, 0.0f)
                    .color(cr, cg, cb, ca)
                r += (90 / samples)
            }
            val rad1 = Math.toRadians((360 / 4.0 + i * 90.0)).toFloat()
            val sin = (sin(rad1.toDouble()) * rad).toFloat()
            val cos = (cos(rad1.toDouble()) * rad).toFloat()
            bufferBuilder.vertex(matrix, current[0].toFloat() + sin, current[1].toFloat() + cos, 0.0f)
                .color(cr, cg, cb, ca)
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
    }

    fun renderRoundedQuadInternal2(
        matrix: Matrix4f?,
        cr: Float,
        cg: Float,
        cb: Float,
        ca: Float,
        cr1: Float,
        cg1: Float,
        cb1: Float,
        ca1: Float,
        cr2: Float,
        cg2: Float,
        cb2: Float,
        ca2: Float,
        cr3: Float,
        cg3: Float,
        cb3: Float,
        ca3: Float,
        fromX: Double,
        fromY: Double,
        toX: Double,
        toY: Double,
        radC1: Double
    ) {
        val bufferBuilder =
            Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR)

        val map = arrayOf(
            doubleArrayOf(toX - radC1, toY - radC1, radC1),
            doubleArrayOf(toX - radC1, fromY + radC1, radC1),
            doubleArrayOf(fromX + radC1, fromY + radC1, radC1),
            doubleArrayOf(fromX + radC1, toY - radC1, radC1)
        )

        for (i in 0..3) {
            val current = map[i]
            val rad = current[2]
            var r = (i * 90).toDouble()
            while (r < (90 + i * 90)) {
                val rad1 = Math.toRadians(r).toFloat()
                val sin = (sin(rad1.toDouble()) * rad).toFloat()
                val cos = (cos(rad1.toDouble()) * rad).toFloat()
                when (i) {
                    0 -> bufferBuilder.vertex(matrix, current[0].toFloat() + sin, current[1].toFloat() + cos, 0.0f)
                        .color(cr1, cg1, cb1, ca1)

                    1 -> bufferBuilder.vertex(matrix, current[0].toFloat() + sin, current[1].toFloat() + cos, 0.0f)
                        .color(cr, cg, cb, ca)

                    2 -> bufferBuilder.vertex(matrix, current[0].toFloat() + sin, current[1].toFloat() + cos, 0.0f)
                        .color(cr2, cg2, cb2, ca2)

                    else -> bufferBuilder.vertex(matrix, current[0].toFloat() + sin, current[1].toFloat() + cos, 0.0f)
                        .color(cr3, cg3, cb3, ca3)
                }
                r += 10.0
            }
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
    }


    fun draw2DGradientRect(
        matrices: MatrixStack,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        leftBottomColor: Color,
        leftTopColor: Color,
        rightBottomColor: Color,
        rightTopColor: Color
    ) {
        val matrix = matrices.peek().positionMatrix
        setupRender()
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(matrix, right, top, 0.0f).color(rightTopColor.rgb)
        bufferBuilder.vertex(matrix, left, top, 0.0f).color(leftTopColor.rgb)
        bufferBuilder.vertex(matrix, left, bottom, 0.0f).color(leftBottomColor.rgb)
        bufferBuilder.vertex(matrix, right, bottom, 0.0f).color(rightBottomColor.rgb)
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
    }

    private fun setupRender() {
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }


    fun drawDefaultArrow(
        matrices: MatrixStack,
        x: Float,
        y: Float,
        size: Float,
        tracerWidth: Float,
        downHeight: Float,
        down: Boolean,
        glow: Boolean,
        color: Int
    ) {
        var color = color
        if (glow) drawBlurredShadow(
            matrices,
            x - size * tracerWidth,
            y,
            (x + size * tracerWidth) - (x - size * tracerWidth),
            size,
            10,
            injectAlpha(
                Color(color), 140
            )
        )

        matrices.push()
        setupRender()
        val matrix = matrices.peek().positionMatrix

        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(color)
        bufferBuilder.vertex(matrix, (x - size * tracerWidth), (y + size), 0.0f).color(color)
        bufferBuilder.vertex(matrix, x, (y + size - downHeight), 0.0f).color(color)
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(color)
        color = darker(Color(color), 0.8f).rgb
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(color)
        bufferBuilder.vertex(matrix, x, (y + size - downHeight), 0.0f).color(color)
        bufferBuilder.vertex(matrix, (x + size * tracerWidth), (y + size), 0.0f).color(color)
        bufferBuilder.vertex(matrix, x, y, 0.0f).color(color)

        if (down) {
            color = darker(Color(color), 0.6f).rgb
            bufferBuilder.vertex(matrix, (x - size * tracerWidth), (y + size), 0.0f).color(color)
            bufferBuilder.vertex(matrix, (x + size * tracerWidth), (y + size), 0.0f).color(color)
            bufferBuilder.vertex(matrix, x, (y + size - downHeight), 0.0f).color(color)
            bufferBuilder.vertex(matrix, (x - size * tracerWidth), (y + size), 0.0f).color(color)
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        endRender()
        matrices.pop()
    }

    private fun endRender() {
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    fun drawGradientRound(
        ms: MatrixStack,
        v: Float,
        v1: Float,
        i: Float,
        i1: Float,
        v2: Float,
        darker: Color,
        darker1: Color,
        darker2: Color,
        darker3: Color
    ) {
        renderRoundedQuad2(
            ms,
            darker,
            darker1,
            darker2,
            darker3,
            v.toDouble(),
            v1.toDouble(),
            (v + i).toDouble(),
            (v1 + i1).toDouble(),
            v2.toDouble()
        )
    }

    private fun preShaderDraw(matrices: MatrixStack, x: Float, y: Float, width: Float, height: Float): BufferBuilder {
        setupRender()
        val matrix = matrices.peek().positionMatrix
        val buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
        setRectanglePoints(buffer, matrix, x, y, x + width, y + height)
        return buffer
    }

    private fun setRectanglePoints(buffer: BufferBuilder, matrix: Matrix4f, x: Float, y: Float, x1: Float, y1: Float) {
        buffer.vertex(matrix, x, y, 0f)
        buffer.vertex(matrix, x, y1, 0f)
        buffer.vertex(matrix, x1, y1, 0f)
        buffer.vertex(matrix, x1, y, 0f)
    }

    fun drawLine(x: Float, y: Float, x1: Float, y1: Float, color: Int) {
        RenderSystem.setShader { GameRenderer.getPositionColorProgram() }
        val bufferBuilder =
            Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)
        bufferBuilder.vertex(x, y, 0f).color(color)
        bufferBuilder.vertex(x1, y1, 0f).color(color)
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
    }


    fun registerBufferedImageTexture(i: Texture, bi: BufferedImage?) {
        try {
            val baos = ByteArrayOutputStream()
            ImageIO.write(bi, "png", baos)
            val bytes = baos.toByteArray()
            registerTexture(i, bytes)
        } catch (ignored: Exception) {
        }
    }

    fun registerTexture(i: Texture, content: ByteArray) {
        try {
            val data = BufferUtils.createByteBuffer(content.size).put(content)
            data.flip()
            val tex = NativeImageBackedTexture(NativeImage.read(data))
            mc.execute { mc.textureManager.registerTexture(i.id, tex) }
        } catch (ignored: Exception) {
        }
    }

    class BlurredShadow(bufferedImage: BufferedImage?) {
        var id: Texture = Texture("texture/remote/" + RandomStringUtils.randomAlphanumeric(16))

        init {
            registerBufferedImageTexture(id, bufferedImage)
        }

        fun bind() {
            RenderSystem.setShaderTexture(0, id.id)
        }
    }

    @JvmRecord
    data class Rectangle(val x: Float, val y: Float, val x1: Float, val y1: Float) {
        fun contains(x: Double, y: Double): Boolean {
            return x >= this.x && x <= x1 && y >= this.y && y <= y1
        }
    }
}
