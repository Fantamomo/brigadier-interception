package com.fantamomo.mc.brigadier.interception.context

interface InterceptionContext<C, out R> {
    /**
     * The context [C] of the interception.
     */
    val context: C

    /**
     * Runs the original command, with the same context.
     */
    fun runOriginal(): R = runOriginalWith(context)

    /**
     * Runs the original command, with the given context.
     */
    fun runOriginalWith(context: C): R
}

