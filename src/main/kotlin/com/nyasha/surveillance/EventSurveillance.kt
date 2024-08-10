package com.nyasha.surveillance

import com.nyasha.Nyasha
import com.nyasha.events.EventDeath
import com.nyasha.events.EventPrePacketReceive
import com.nyasha.events.EventPrePlayerUpdate
import com.nyasha.events.EventTotemPop
import com.nyasha.util.IMinecraft
import meteordevelopment.orbit.EventHandler
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket

/**
 * @author yuxiangll
 * @since 2024/7/8 下午3:07
 * IntelliJ IDEA
 */
object EventSurveillance : IMinecraft {


    @EventHandler
    fun tick(event: EventPrePlayerUpdate){

        mc.world?.getPlayers()?.forEach {
            if (it.isDead || it.health == 0F) Nyasha.EventBus.post(EventDeath(it))
        }


    }


    @EventHandler
    fun onPacketReceive(event: EventPrePacketReceive){

        when (event.packet){
            is EntityStatusS2CPacket -> {
                if (event.packet.status == EntityStatuses.USE_TOTEM_OF_UNDYING){
                    val event = EventTotemPop(event.packet.getEntity(mc.world) as PlayerEntity)
                    Nyasha.EventBus.post(event)
                }


            }
        }



    }






}