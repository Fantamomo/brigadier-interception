package com.fantamomo.mc.brigadier.interception.interception

import com.fantamomo.mc.brigadier.interception.callable.ExecutionInterceptionCaller
import com.fantamomo.mc.brigadier.interception.context.ExecutionInterceptionContext
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack

class ExecutionInterception(
    private val inception: ExecutionInterceptionCaller,
    private val original: Command<CommandSourceStack>
) : Command<CommandSourceStack> {
    override fun run(context: CommandContext<CommandSourceStack>): Int {
        val context = ExecutionInterceptionContext(context, original)
        return inception.runInterception(context)
    }
}