package com.webframework.routing

import com.webframework.core.Context

fun interface Handler {
    fun handle(ctx: Context)
}