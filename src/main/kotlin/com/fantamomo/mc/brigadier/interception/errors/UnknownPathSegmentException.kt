package com.fantamomo.mc.brigadier.interception.errors

class UnknownPathSegmentException : InterceptionException {
    constructor() : super("Unknown path segment")
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}