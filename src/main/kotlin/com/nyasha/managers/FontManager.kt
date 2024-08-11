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

    //为什么不一开始就加载pingFang，因为体积过大，会导致加载速度变慢
    lateinit var pingFang : Font

    val pingFang10 by lazy {
        creatFont(pingFang,10F)
    }

    val pingFang16 by lazy {
        creatFont(pingFang,16F)
    }

    val pingFang22 by lazy {
        creatFont(pingFang,22F)
    }

    val pingFang12 by lazy {
        creatFont(pingFang,12F)
    }

    val conther16 by lazy {
        creatFont("Conther",16F)
    }





    @JvmStatic
    fun initialize() {
        pingFang = creatFontGroup("PingFang_Normal")

    }

    private fun creatFont(name: String, size: Float): FontRenderer{
        val fontInputStream = Nyasha.javaClass.classLoader.getResourceAsStream("assets/nyasha/fonts/$name.ttf")
        val procFontStream = Objects.requireNonNull(fontInputStream)
        val font = Font.createFont(Font.TRUETYPE_FONT, procFontStream)
        val procFont = font.deriveFont(Font.PLAIN,size/2f)
        return FontRenderer(procFont, size/2f)
    }

    private fun creatFont(font: Font, size: Float): FontRenderer{
        val procFont = font.deriveFont(Font.PLAIN,size/2f)
        return FontRenderer(procFont, size/2f)
    }

    private fun creatFontGroup(groupName: String): Font{
        val fontInputStream = Nyasha.javaClass.classLoader.getResourceAsStream("assets/nyasha/fonts/$groupName.ttf")
        val procFontStream = Objects.requireNonNull(fontInputStream)
        return Font.createFont(Font.TRUETYPE_FONT, procFontStream)
    }


}