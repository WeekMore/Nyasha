package com.nyasha.command

import com.mojang.brigadier.CommandDispatcher
import com.nyasha.managers.CommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text

/**
 * @author yuxiangll
 * @since 2024/7/7 下午7:17
 * IntelliJ IDEA
 */
object HelpCommand: Command("help","display help") {

    val showHelp = {
        CommandManager.commands.forEachIndexed{ index, command ->
            send("$index /nyasha ${command.name}    ->   ${command.description}")
        }
    }

    override fun initializeCommand() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback {
                dispatcher: CommandDispatcher<FabricClientCommandSource?>, _ ->

            dispatcher.register(literal("nyasha")
                .executes {
                    it.source.sendFeedback(Text.literal("Using /nyasha help"))
                    1
                }

                .then(
                    literal("help")
                        .executes{
                            showHelp()
                            1
                        }
                )



            )
        })
    }


}