package com.fantamomo.mc.brigadier.interception.errors

/**
 * Thrown when the target of the interception is not found.
 * E.g. you tried to intercept an execute command, but the target has no execute command.
 */
class MissingInterceptionTargetException : InterceptionException {
    constructor() : super("The target of the interception is not found")
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}