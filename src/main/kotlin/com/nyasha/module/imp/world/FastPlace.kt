package com.nyasha.module.imp.world

import com.nyasha.events.EventPrePlayerUpdate
import com.nyasha.inject.accessor.IMinecraftClient
import com.nyasha.module.Category
import com.nyasha.module.Module
import meteordevelopment.orbit.EventHandler

/**
 * @author yuxiangll
 * @since 2024/7/14 下午5:53
 * IntelliJ IDEA
 */
object FastPlace : Module("FastPlace", Category.WORLD) {


    @EventHandler
    fun onPlace(event: EventPrePlayerUpdate) {
        (mc as IMinecraftClient).useCooldown = 0
    }


}