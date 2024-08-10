package com.nyasha.module.imp.render

import com.nyasha.gui.clickgui.ClickScreen
import com.nyasha.module.Bind
import com.nyasha.module.Category
import com.nyasha.module.Module
import org.lwjgl.glfw.GLFW

/**
 * @author yuxiangll
 * @since 2024/7/8 下午12:13
 * IntelliJ IDEA
 */
object ClickGui : Module(
    "ClickGui",
    Category.RENDER,
    Bind(GLFW.GLFW_KEY_RIGHT_SHIFT)
) {

    var openned = false

    override fun onEnable() {
        if (!openned){
            openned = true
            //mc.setScreen(ClickScreen)
            //mc.player?.sendMessage(Text.of("开启clickgui"))
            mc.setScreen(ClickScreen)
        }

    }

    override fun onDisable() {
        openned = false
    }


}