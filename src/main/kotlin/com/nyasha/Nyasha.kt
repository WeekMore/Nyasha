package com.nyasha

import com.nyasha.managers.CommandManager
import com.nyasha.managers.ModuleManager
import com.nyasha.managers.SurveillanceManager
import com.nyasha.util.render.Render2DEngine
import meteordevelopment.orbit.EventBus
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method




object Nyasha : ModInitializer {
    private val logger = LoggerFactory.getLogger("Nyasha")
	val NAME = "Nyasha"
	val VERSION = "1.0.0"

	var FirstTimeLoad = false

	val EventBus = EventBus()


	override fun onInitialize() {


		EventBus.registerLambdaFactory( "com.nyasha") { lookupInMethod: Method, klass: Class<*> ->
			lookupInMethod(null, klass, MethodHandles.lookup()) as MethodHandles.Lookup
		}


		logger.info("initializing managers")

		ModuleManager.initialize()
		SurveillanceManager.initialize()
		CommandManager.initialize()
		Render2DEngine.initShaders()

	}
}