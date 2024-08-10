package com.nyasha.managers


import com.nyasha.Nyasha
import com.nyasha.surveillance.EventSurveillance
import com.nyasha.surveillance.ModuleSurveillance


/**
 * @author yuxiangll
 * @since 2024/7/6 上午9:53
 * IntelliJ IDEA
 */

object SurveillanceManager {
    private val surveillance = mutableListOf<Any>()

    fun initialize(){
        surveillance.add(ModuleSurveillance)
        surveillance.add(EventSurveillance)

        surveillance.forEach {
            Nyasha.EventBus.subscribe(it)
        }

    }

}