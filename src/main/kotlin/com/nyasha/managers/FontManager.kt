package com.nyasha.managers


import com.nyasha.Nyasha
import com.nyasha.font.FontRenderer
import java.awt.Font
import java.util.Objects


/**
 * @author yuxiangll
 * @since 2024/7/5 下午9:02
 * IntelliJ IDEA
 */
@Suppress("MemberVisibilityCanBePrivate")
object FontManager {


    val pingFang10 by lazy {
        creatFont("PingFang_Normal",10F)
    }

    lateinit var pingFang22: FontRenderer;
    lateinit var pingFang16: FontRenderer;
    lateinit var pingFang12: FontRenderer;


    @JvmStatic
    fun initialize() {
        pingFang22 = creatFont("PingFang_Normal",22F)
        pingFang16 = creatFont("PingFang_Normal",16F)
        pingFang12 = creatFont("PingFang_Normal",12F)

    }

    private fun creatFont(name: String, size: Float): FontRenderer{

        val fontInputStream = Nyasha.javaClass.classLoader.getResourceAsStream("assets/nyasha/fonts/$name.ttf")
        val procFontStream = Objects.requireNonNull(fontInputStream)
        val font = Font.createFont(Font.TRUETYPE_FONT, procFontStream)
        val procFont = font.deriveFont(Font.PLAIN,size/2f)
        return FontRenderer(procFont, size/2f)

    }


}