package com.forge.routing

import com.forge.core.Context

fun interface Handler {
    fun handle(ctx: Context)
}