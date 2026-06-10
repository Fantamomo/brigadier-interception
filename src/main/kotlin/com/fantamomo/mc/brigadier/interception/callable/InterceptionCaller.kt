package com.fantamomo.mc.brigadier.interception.callable

import com.fantamomo.mc.brigadier.interception.context.InterceptionContext

fun interface InterceptionCaller<in C : InterceptionContext<*, R>, out R> {

    fun C.intercept(): R

    /**
     * Helper methode to run the interception.
     */
    fun runInterception(context: C): R = context.intercept()
}