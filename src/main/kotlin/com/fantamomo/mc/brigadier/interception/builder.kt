package com.fantamomo.mc.brigadier.interception

import com.mojang.brigadier.arguments.ArgumentType
import kotlin.reflect.KClass

class BrigadierInterceptionTypedBuilder(private val interceptor: BrigadierInterceptor) {
    private val path = mutableListOf<Any>()

    fun name(name: String) = path.add(name)

    fun type(type: KClass<*>) = path.add(type.java)

    fun type(type: Class<*>) = path.add(type)

    inline fun <reified T> type() = type(T::class)

    fun argument(type: KClass<out ArgumentType<*>>) = path.add(type.java)
    fun argument(type: Class<out ArgumentType<*>>) = path.add(type)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : ArgumentType<*>> argument() = argument(T::class.java as Class<ArgumentType<*>>)

    fun build(): InterceptionTarget = interceptor.typed(path)
}

inline fun BrigadierInterceptor.typed(action: BrigadierInterceptionTypedBuilder.() -> Unit): InterceptionTarget {
    val builder = BrigadierInterceptionTypedBuilder(this)
    builder.action()
    return builder.build()
}