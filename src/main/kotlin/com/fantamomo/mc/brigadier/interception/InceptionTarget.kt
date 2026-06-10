package com.fantamomo.mc.brigadier.interception

import com.fantamomo.mc.brigadier.interception.callable.ExecutionInterceptionCaller
import com.fantamomo.mc.brigadier.interception.callable.RequirementInterceptionCaller
import com.fantamomo.mc.brigadier.interception.errors.MissingInterceptionTargetException
import com.fantamomo.mc.brigadier.interception.interception.ExecutionInterception
import com.fantamomo.mc.brigadier.interception.interception.RequirementInterception
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack

class InceptionTarget internal constructor(private val target: CommandNode<CommandSourceStack>) {

    /**
     * Checks if the target has an execute command.
     * @return true if the target has an execute command, false otherwise.
     */
    fun hasExecute(): Boolean = commandField.get(target) != null

    /**
     * Intercepts the execute command of the target.
     * @param interceptor The interceptor to call when the execute command is executed.
     * @throws MissingInterceptionTargetException if the target has no execute command.
     */
    fun interceptExecute(interceptor: ExecutionInterceptionCaller) {
        @Suppress("UNCHECKED_CAST")
        val originalCommand = commandField.get(target) as? Command<CommandSourceStack>
            ?: throw MissingInterceptionTargetException("This target has no execute command")

        val interception = ExecutionInterception(interceptor, originalCommand)
        set {
            commandField.set(it, interception)
        }
    }

    /**
     * Checks if the target has a custom requirement, which is not the default one.
     * (The default is a lambda that always returns true)
     * @return true if the target has a custom requirement, false otherwise.
     */
    fun hasCustomRequirement(): Boolean = target.requirement::class != requirementDefaultLambdaClass

    /**
     * Intercepts the requirement of the target.
     * @param interception The interceptor to call when the requirement is checked.
     */
    fun interceptRequirement(interception: RequirementInterceptionCaller) {
        val originalRequirement = target.requirement

        val interception = RequirementInterception(interception, originalRequirement)
        set {
            it.requirement = interception
        }
    }

    /**
     * Applies a given action to the target node and its connected nodes recursively.
     *
     * @param action The action to perform on each visited node of type `CommandNode<CommandSourceStack>`.
     */
    private fun set(action: (CommandNode<CommandSourceStack>) -> Unit) {
        val visited = LinkedHashSet<CommandNode<CommandSourceStack>>(4) as MutableSet<CommandNode<CommandSourceStack>>
        visited.add(target)
        target.set0(action, visited)
    }

    /**
     * Applies a given action to the current `CommandNode` and its associated nodes recursively.
     *
     * @param action The action to perform on each visited node of type `CommandNode<CommandSourceStack>`.
     * @param visited A mutable set that tracks the nodes already visited to prevent infinite recursion.
     */
    private fun CommandNode<CommandSourceStack>.set0(
        action: (CommandNode<CommandSourceStack>) -> Unit,
        visited: MutableSet<CommandNode<CommandSourceStack>>
    ) {
        action(this)
        clientNode?.let {
            if (visited.add(it)) it.set0(action, visited)
        }
        wrappedCached?.let {
            if (visited.add(it)) it.set0(action, visited)
        }
        unwrappedCached?.let {
            if (visited.add(it)) it.set0(action, visited)
        }
    }

    companion object {
        private val commandField = try {
            CommandNode::class.java
                .getDeclaredField("command")
                .apply { isAccessible = true }
        } catch (e: Exception) {
            throw RuntimeException("Failed to access CommandNode::command field", e)
        }

        private val requirementDefaultLambdaClass = try {
            LiteralArgumentBuilder.literal<String>("test").requirement::class
        } catch (e: Exception) {
            throw RuntimeException("Failed to access LiteralArgumentBuilder::requirement field", e)
        }
    }
}