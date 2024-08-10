package com.nyasha.util.render.shaders

import com.mojang.blaze3d.systems.RenderSystem
import com.nyasha.util.IMinecraft
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import org.ladysnake.satin.api.managed.ManagedCoreShader
import org.ladysnake.satin.api.managed.ShaderEffectManager
import org.ladysnake.satin.api.managed.uniform.SamplerUniform
import org.ladysnake.satin.api.managed.uniform.Uniform1f
import org.ladysnake.satin.api.managed.uniform.Uniform2f
import org.ladysnake.satin.api.managed.uniform.Uniform4f
import org.lwjgl.opengl.GL30
import java.awt.Color

/**
 * @author yuxiangll
 * @since 2024/7/14 上午7:31
 * IntelliJ IDEA
 */
class BlurProgram : IMinecraft {
    private var uSize: Uniform2f? = null
    private var uLocation: Uniform2f? = null
    private var radius: Uniform1f? = null
    private var inputResolution: Uniform2f? = null
    private var brightness: Uniform1f? = null
    private var quality: Uniform1f? = null
    private var color1: Uniform4f? = null
    private var sampler: SamplerUniform? = null

    private var input: Framebuffer? = null

    init {
        setup()
    }

    fun setParameters(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        r: Float,
        c1: Color,
        blurStrenth: Float,
        blurOpacity: Float
    ) {
        val i = mc.window.scaleFactor.toFloat()
        radius!!.set(r * i)
        uLocation!![x * i] = -y * i + mc.window.scaledHeight * i - height * i
        uSize!![width * i] = height * i
        brightness!!.set(blurOpacity)
        quality!!.set(blurStrenth)
        color1!![c1.red / 255f, c1.green / 255f, c1.blue / 255f] = 1f

        if (input != null) input!!.resize(
            mc.window.framebufferWidth,
            mc.window.framebufferHeight,
            MinecraftClient.IS_SYSTEM_MAC
        )
        else input = SimpleFramebuffer(100, 100, false, MinecraftClient.IS_SYSTEM_MAC)

        sampler!!.set(input!!.colorAttachment)
    }

    fun use() {
        val buffer = MinecraftClient.getInstance().framebuffer
        input!!.beginWrite(false)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buffer.fbo)
        GL30.glBlitFramebuffer(
            0,
            0,
            buffer.textureWidth,
            buffer.textureHeight,
            0,
            0,
            buffer.textureWidth,
            buffer.textureHeight,
            GL30.GL_COLOR_BUFFER_BIT,
            GL30.GL_LINEAR
        )
        buffer.beginWrite(false)

        inputResolution!![buffer.textureWidth.toFloat()] = buffer.textureHeight.toFloat()
        sampler!!.set(input!!.colorAttachment)

        RenderSystem.setShader { BLUR.program }
    }

    protected fun setup() {
        this.inputResolution = BLUR.findUniform2f("InputResolution")
        this.brightness = BLUR.findUniform1f("Brightness")
        this.quality = BLUR.findUniform1f("Quality")
        this.color1 = BLUR.findUniform4f("color1")
        this.uSize = BLUR.findUniform2f("uSize")
        this.uLocation = BLUR.findUniform2f("uLocation")
        this.radius = BLUR.findUniform1f("radius")
        sampler = BLUR.findSampler("InputSampler")
    }

    companion object {
        val BLUR: ManagedCoreShader = ShaderEffectManager.getInstance()
            .manageCoreShader(Identifier.of("nyasha", "blur"), VertexFormats.POSITION)
    }
}