package com.fantamomo.mc.brigadier.interception

import com.mojang.brigadier.CommandDispatcher
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object BrigadierInterceptor {

    fun builder(
        dispatcher: CommandDispatcher<CommandSourceStack>
    ) = InterceptionBuilder(dispatcher)

    @OptIn(ExperimentalContracts::class)
    inline fun build(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        block: InterceptionBuilder.() -> Unit
    ) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        builder(dispatcher).apply(block).install()
    }
}