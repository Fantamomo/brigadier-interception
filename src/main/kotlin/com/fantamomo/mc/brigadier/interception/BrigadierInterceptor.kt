package com.fantamomo.mc.brigadier.interception

import com.fantamomo.mc.brigadier.interception.errors.UnknownPathSegmentException
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.ShadowBrigNode
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass


class BrigadierInterceptor(private val dispatcher: CommandDispatcher<CommandSourceStack>) {

    /**
     * Gets the interception target for the given path.
     * @param path The path to the target.
     */
    @Suppress("UNCHECKED_CAST")
    fun path(path: List<String>): InterceptionTarget {
        if (path.isEmpty()) throw IllegalArgumentException("Path must not be empty")

        val target = findNode(path) { segment, index, current ->
            current
                .getChild(segment)
                ?.unbox()
                    as? CommandNode<CommandSourceStack>
                ?: throw UnknownPathSegmentException(
                    "Could not find path segment '$segment' at index $index in $path"
                )
        }

        return InterceptionTarget(target)
    }

    /**
     * Gets the interception target for the given path.
     * @param path The path to the target.
     */
    fun path(vararg path: String): InterceptionTarget = path(path.toList())

    /**
     * Resolves a path of mixed path segments ([KClass], [Class], or [String]) to find a corresponding
     * command node and returns an instance of [InterceptionTarget] with the located node.
     *
     * Path segments are interpreted as follows:
     * - [String]: Tries to find a child node with the same name.
     * - [Class] or [KClass]:
     *   1. If it is a subclass of [ArgumentType], tries to find an argument node with the same type.
     *   2. If not a subclass of [ArgumentType] or a node couldn't be resolved in the previous step,
     *      searches for an argument node whose generic type matches the given class.
     *      E.g. [com.mojang.brigadier.arguments.StringArgumentType] will be matched with [String].
     *
     * @param path The path segments used to locate the command node. The segments can be of type [KClass], [Class], or [String].
     * @return An `InceptionTarget` instance wrapping the resolved command node.
     * @throws IllegalArgumentException If an invalid type is provided in the path.
     * @throws UnknownPathSegmentException If a path segment cannot be resolved to a command node.
     */
    fun typed(path: List<Any>): InterceptionTarget {
        if (path.isEmpty()) throw IllegalArgumentException("Path must not be empty")
        val path = path.map { segment ->
            when (segment) {
                is KClass<*> -> segment.javaObjectType
                is Class<*>, is String -> segment
                else -> throw IllegalArgumentException("Invalid path segment type: ${segment::class}")
            }
        }

        val target = findNode(path) { segment, index, current ->
            val node = when (segment) {
                is String -> {
                    current.getChild(segment)
                        ?: throw UnknownPathSegmentException("Could not find path segment '$segment' at index $index in $path")
                }

                is Class<*> -> {
                    if (ArgumentType::class.java.isAssignableFrom(segment)) {
                        current.children.firstOrNull { it is ArgumentCommandNode<*, *> && it.type.javaClass == segment }
                            ?.let { return@findNode it }
                    }
                    current.children.firstOrNull { it is ArgumentCommandNode<*, *> && getTypeOfArgument(it.type) == segment }
                        ?: throw UnknownPathSegmentException("Could not find argument of type '${segment.name}' at index $index in $path")
                }

                else -> throw IllegalArgumentException("Invalid path segment type: ${segment::class}")
            }.unbox()

            @Suppress("UNCHECKED_CAST")
            node as CommandNode<CommandSourceStack>
        }

        return InterceptionTarget(target)
    }

    private fun <T> findNode(path: List<T>, action: (T, Int, CommandNode<CommandSourceStack>) -> CommandNode<CommandSourceStack>): CommandNode<CommandSourceStack> {
        var current: CommandNode<CommandSourceStack> = dispatcher.root
        for ((index, segment) in path.withIndex()) {
            val new = action(segment, index, current)

            // internal check to ensure that the node has actually changed
            // should never happen, but just to be sure
            if (new === current) throw IllegalStateException("No change in node at index $index, check the lambda")

            current = new
        }
        if (current === dispatcher.root) throw IllegalArgumentException("Can not intercept root node")
        return current
    }

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

    companion object {
        private val statelessArgumentTypes = listOf(
            BoolArgumentType::class.java,

        )
        private val argumentTypeClassCache = mutableMapOf<Class<ArgumentType<*>>, Class<*>>()

        private fun getTypeOfArgument(argument: ArgumentType<*>): Class<*> {
            val clazz = argument.javaClass
            argumentTypeClassCache[clazz]?.let { return it }
            for (type in clazz.getGenericInterfaces()) {
                if (type is ParameterizedType) {
                    if (type.rawType === ArgumentType::class.java) {
                        val arg: Type = type.actualTypeArguments[0]

                        if (arg is Class<*>) {
                            argumentTypeClassCache[clazz] = arg
                            return arg
                        }
                    }
                }
            }
            throw IllegalArgumentException("Could not find type of argument $argument")
        }
    }
}