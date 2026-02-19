package com.fantamomo.mc.brigadier.interception

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack

class BrigadierInception(
    private val inception: InceptionContext.() -> Int,
    private val original: Command<CommandSourceStack>
) : Command<CommandSourceStack> {
    override fun run(context: CommandContext<CommandSourceStack>): Int {
        val context = InceptionContext(context, original)
        return context.inception()
    }
}