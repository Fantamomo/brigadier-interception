package com.fantamomo.mc.brigadier.interception

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack

class InceptionContext internal constructor(
    val context: CommandContext<CommandSourceStack>,
    private val original: Command<CommandSourceStack>
) {
    fun runOriginal(): Int = original.run(context)
}