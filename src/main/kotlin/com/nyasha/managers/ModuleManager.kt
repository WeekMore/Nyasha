package com.nyasha.managers

import com.nyasha.Nyasha
import com.nyasha.module.Module
import com.nyasha.module.imp.combat.*
import com.nyasha.module.imp.player.*
import com.nyasha.module.imp.render.*
import com.nyasha.module.imp.world.*


/**
 * @author yuxiangll
 * @since 2024/7/6 上午9:42
 * IntelliJ IDEA
 */

@Suppress("MemberVisibilityCanBePrivate")
object ModuleManager {


    val modules = mutableListOf<Module>()



    fun initialize(){
        modules.add(AutoClicker)
        modules.add(ClickGui)
        modules.add(Reach)
        modules.add(Trigger)
        modules.add(FastPlace)
        modules.add(SwardBlock)
        modules.add(NoBreakCooldown)
        Nyasha.EventBus.subscribe(this)
    }






}