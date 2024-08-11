package com.nyasha.gui.testcef

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.nyasha.Nyasha
import com.nyasha.mcef.MCEF
import com.nyasha.mcef.MCEFBrowser
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * @author yuxiangll
 * @since 2024/8/11 下午12:01
 * IntelliJ IDEA
 */
object TestMenu : Screen(Text.of("Mcef")) {


    private val browser: MCEFBrowser =
        MCEF.createBrowser("/Users/yuxiangll/Documents/Nyasha/src/main/resources/assets/nyasha/web/test.html",true)


    override fun init() {
        browser.loadURL("/Users/yuxiangll/Documents/Nyasha/src/main/resources/assets/nyasha/web/test.html")
        super.init()
        browser.resize(width, height)
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        super.resize(client, width, height)
        browser.resize(width,height)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        //browser.loadURL("/Users/yuxiangll/Documents/Nyasha/src/main/resources/assets/nyasha/web/test.html")
        val x = 10f
        val y = 10f
        val height = 1000f
        val width = 1000f
        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShader {
            GameRenderer.getPositionTexColorProgram()
        }
        RenderSystem.setShaderTexture(0, browser.renderer.textureID)
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(x, y + height, 0.0f)
            .texture(0.0f, 1.0f)
            .color(255, 255, 255, 255)
        buffer.vertex(x + width, y + height, 0.0f)
            .texture(1.0f, 1.0f)
            .color(255, 255, 255, 255)
        buffer.vertex(x + width, y, 0.0f)
            .texture(1.0f, 0.0f)
            .color(255, 255, 255, 255)
        buffer.vertex(x, y, 0.0f)
            .texture(0.0f, 0.0f)
            .color(255, 255, 255, 255)
        drawWithGlobalProgram(buffer.end())
        RenderSystem.setShaderTexture(0, 0)
        RenderSystem.enableDepthTest()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableBlend()



    }

    override fun close() {
        browser.close()
        super.close()
    }




}