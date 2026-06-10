package com.fantamomo.mc.brigadier.interception.context

import io.papermc.paper.command.brigadier.CommandSourceStack
import java.util.function.Predicate

class RequirementInterceptionContext(
    override val context: CommandSourceStack,
    private val original: Predicate<CommandSourceStack>
) : InterceptionContext<CommandSourceStack, Boolean> {
    override fun runOriginalWith(context: CommandSourceStack): Boolean = original.test(context)
}