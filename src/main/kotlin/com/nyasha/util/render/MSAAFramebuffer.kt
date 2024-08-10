package com.nyasha.util.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.Framebuffer
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30C
import kotlin.math.min

class MSAAFramebuffer(private val samples: Int) : Framebuffer(true) {
    private var rboColor = 0
    private var rboDepth = 0

    init {
        setClearColor(1f, 1f, 1f, 0f)
    }

    override fun resize(width: Int, height: Int, getError: Boolean) {
        if (textureWidth != width || textureHeight != height) {
            super.resize(width, height, getError)
        }
    }

    override fun initFbo(width: Int, height: Int, getError: Boolean) {
        RenderSystem.assertOnRenderThreadOrInit()
        viewportWidth = width
        viewportHeight = height
        textureWidth = width
        textureHeight = height

        fbo = GlStateManager.glGenFramebuffers()
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo)

        rboColor = GlStateManager.glGenRenderbuffers()
        GlStateManager._glBindRenderbuffer(GL30C.GL_RENDERBUFFER, rboColor)
        GL30.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, samples, GL30C.GL_RGBA8, width, height)
        GlStateManager._glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0)

        rboDepth = GlStateManager.glGenRenderbuffers()
        GlStateManager._glBindRenderbuffer(GL30C.GL_RENDERBUFFER, rboDepth)
        GL30.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, samples, GL30C.GL_DEPTH_COMPONENT, width, height)
        GlStateManager._glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0)

        GL30.glFramebufferRenderbuffer(
            GL30C.GL_FRAMEBUFFER,
            GL30C.GL_COLOR_ATTACHMENT0,
            GL30C.GL_RENDERBUFFER,
            rboColor
        )
        GL30.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_RENDERBUFFER, rboDepth)

        colorAttachment = MinecraftClient.getInstance().framebuffer.colorAttachment
        depthAttachment = MinecraftClient.getInstance().framebuffer.depthAttachment

        checkFramebufferStatus()
        clear(getError)
        endRead()
    }

    override fun delete() {
        RenderSystem.assertOnRenderThreadOrInit()
        endRead()
        endWrite()

        if (fbo > -1) {
            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0)
            GlStateManager._glDeleteFramebuffers(fbo)
            fbo = -1
        }

        if (rboColor > -1) {
            GlStateManager._glDeleteRenderbuffers(rboColor)
            rboColor = -1
        }

        if (rboDepth > -1) {
            GlStateManager._glDeleteRenderbuffers(rboDepth)
            rboDepth = -1
        }

        colorAttachment = -1
        depthAttachment = -1
        textureWidth = -1
        textureHeight = -1
    }

    companion object {
        val MAX_SAMPLES: Int = GL30.glGetInteger(GL30C.GL_MAX_SAMPLES)
        private val INSTANCES: MutableMap<Int, MSAAFramebuffer> = HashMap()

        fun getInstance(samples: Int): MSAAFramebuffer {
            return INSTANCES.computeIfAbsent(samples) { x: Int? -> MSAAFramebuffer(samples) }
        }

        fun use(fancy: Boolean, drawAction: Runnable) {
            use(
                min((if (fancy) 16 else 4).toDouble(), MAX_SAMPLES.toDouble())
                    .toInt(), MinecraftClient.getInstance().framebuffer, drawAction
            )
        }

        fun use(samples: Int, mainBuffer: Framebuffer, drawAction: Runnable) {
            RenderSystem.assertOnRenderThreadOrInit()
            val msaaBuffer = getInstance(samples)
            msaaBuffer.resize(mainBuffer.textureWidth, mainBuffer.textureHeight, false)

            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, mainBuffer.fbo)
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, msaaBuffer.fbo)
            GlStateManager._glBlitFrameBuffer(
                0,
                0,
                msaaBuffer.textureWidth,
                msaaBuffer.textureHeight,
                0,
                0,
                msaaBuffer.textureWidth,
                msaaBuffer.textureHeight,
                GL30C.GL_COLOR_BUFFER_BIT,
                GL30C.GL_LINEAR
            )
            msaaBuffer.beginWrite(true)

            drawAction.run()
            msaaBuffer.endWrite()

            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, msaaBuffer.fbo)
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, mainBuffer.fbo)
            GlStateManager._glBlitFrameBuffer(
                0,
                0,
                msaaBuffer.textureWidth,
                msaaBuffer.textureHeight,
                0,
                0,
                msaaBuffer.textureWidth,
                msaaBuffer.textureHeight,
                GL30C.GL_COLOR_BUFFER_BIT,
                GL30C.GL_LINEAR
            )
            msaaBuffer.clear(false)
            mainBuffer.beginWrite(false)
        }
    }
}