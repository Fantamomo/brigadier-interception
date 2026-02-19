package com.fantamomo.mc.brigadier.interception

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.ShadowBrigNode

class InterceptionBuilder internal constructor(
    private val dispatcher: CommandDispatcher<CommandSourceStack>
) {
    private val interceptions: MutableList<List<String>> = mutableListOf()

    private val commandField = CommandNode::class.java
        .getDeclaredField("command")
        .apply { isAccessible = true }

    private lateinit var block: InceptionContext.() -> Int

    private var installed = false

    fun interception(block: InceptionContext.() -> Int) {
        if (installed) throw IllegalStateException("Interception already installed.")
        this.block = block
    }

    fun path(
        vararg path: String
    ) {
        if (installed) throw IllegalStateException("Interception already installed.")
        require(path.isNotEmpty()) { "Command path cannot be empty." }

        interceptions += path.toList()
    }

    fun install() {
        if (installed) throw IllegalStateException("Interception already installed.")
        installed = true
        if (!::block.isInitialized)
            throw IllegalStateException("Interception block not set.")

        if (interceptions.isEmpty())
            throw IllegalStateException("No interceptions registered.")

        interceptions.forEach { install(it) }
    }

    private fun install(path: List<String>) {
        val node = findNode(path)
        val original = node.getCommandOrThrow(path)

        node.setCommand(
            BrigadierInception(block, original)
        )
    }

    private fun findNode(path: List<String>): CommandNode<CommandSourceStack> {
        var current: CommandNode<out CommandSourceStack> = dispatcher.root

        for (segment in path) {
            current = current.children
                .firstOrNull { it.name == segment }
                ?.unbox()
                ?: throw IllegalArgumentException(
                    "Command path segment not found: '$segment' in $path"
                )
        }

        @Suppress("UNCHECKED_CAST")
        return current as CommandNode<CommandSourceStack>
    }

    private fun CommandNode<CommandSourceStack>.getCommandOrThrow(
        path: List<String>
    ): Command<CommandSourceStack> {
        @Suppress("UNCHECKED_CAST")
        val command = commandField.get(this) as? Command<CommandSourceStack>
        return command
            ?: throw IllegalStateException(
                "Target node at path $path is not executable."
            )
    }

    private fun CommandNode<CommandSourceStack>.setCommand(
        command: Command<CommandSourceStack>
    ) {
        setCommand0(command, mutableSetOf(this))
    }

    private fun CommandNode<CommandSourceStack>.setCommand0(command: Command<CommandSourceStack>, visit: MutableSet<CommandNode<CommandSourceStack>>) {
        setCommand1(command)
        clientNode?.let {
            if (visit.add(it)) it.setCommand0(command, visit)
        }
        wrappedCached?.let {
            if (visit.add(it)) it.setCommand0(command, visit)
        }
        unwrappedCached?.let {
            if (visit.add(it)) it.setCommand0(command, visit)
        }
    }

    private fun CommandNode<CommandSourceStack>.setCommand1(command: Command<CommandSourceStack>) {
        commandField.set(this, command)
    }

    private fun CommandNode<out CommandSourceStack>.unbox(): CommandNode<out CommandSourceStack> =
        when (this) {
            is ShadowBrigNode -> this.handle
            else -> this
        }
}