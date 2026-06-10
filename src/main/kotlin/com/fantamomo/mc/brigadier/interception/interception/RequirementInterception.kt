package com.fantamomo.mc.brigadier.interception.interception

import com.fantamomo.mc.brigadier.interception.callable.RequirementInterceptionCaller
import com.fantamomo.mc.brigadier.interception.context.RequirementInterceptionContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.util.function.Predicate

class RequirementInterception(
    private val inception: RequirementInterceptionCaller,
    private val original: Predicate<CommandSourceStack>
) : Predicate<CommandSourceStack> {
    override fun test(context: CommandSourceStack): Boolean {
        val context = RequirementInterceptionContext(context, original)
        return inception.runInterception(context)
    }
}