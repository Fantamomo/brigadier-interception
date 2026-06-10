package com.fantamomo.mc.brigadier.interception

import com.fantamomo.mc.brigadier.interception.errors.UnknownPathSegmentException
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.ShadowBrigNode

class BrigadierInterceptor(private val dispatcher: CommandDispatcher<CommandSourceStack>) {

    @Suppress("UNCHECKED_CAST")
    fun path(path: List<String>): InceptionTarget {
        var current: CommandNode<CommandSourceStack> = dispatcher.root

        for ((index, segment) in path.withIndex()) {
            current = current
                .getChild(segment)
                ?.unbox()
                    as? CommandNode<CommandSourceStack>
                ?: throw UnknownPathSegmentException(
                    "Could not find path segment '$segment'($index) in $path"
                )
        }

        return InceptionTarget(current)
    }

    fun path(vararg path: String): InceptionTarget = path(path.toList())

//    fun byCommand(command: String): InceptionTarget {
//        val server = MinecraftServer.getServer()
//        val results = dispatcher.parse(
//            command, net.minecraft.commands.CommandSourceStack(
//                CommandSource.NULL,
//                Vec3.ZERO,
//                Vec2.ZERO,
//                server.allLevels.first(),
//                PermissionSet.ALL_PERMISSIONS,
//                "",
//                Component.empty(),
//                server,
//                null
//            )
//        )
//        results.context
//    }

    private fun CommandNode<out CommandSourceStack>.unbox(): CommandNode<out CommandSourceStack> =
        when (this) {
            is ShadowBrigNode -> this.handle // ShadowBrigNode is a wrapper used by Paper to prevent developers from accessing the original node, which we ignore
            else -> this
        }
}