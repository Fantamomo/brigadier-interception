package com.fantamomo.mc.brigadier.interception.context

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack

class ExecutionInterceptionContext internal constructor(
    override val context: CommandContext<CommandSourceStack>,
    private val original: Command<CommandSourceStack>
) : InterceptionContext<CommandContext<CommandSourceStack>, Int> {

    override fun runOriginalWith(context: CommandContext<CommandSourceStack>): Int = original.run(context)
}