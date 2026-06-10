package com.club.backend.service

import java.lang.reflect.Proxy

internal inline fun <reified T> proxyRepo(noinline handler: (String, Array<out Any?>) -> Any?): T {
    val iface = T::class.java
    lateinit var proxyInstance: Any
    proxyInstance = Proxy.newProxyInstance(iface.classLoader, arrayOf(iface)) { _, method, args ->
        when (method.name) {
            "toString" -> "${iface.simpleName}Proxy"
            "hashCode" -> System.identityHashCode(proxyInstance)
            "equals" -> proxyInstance === args?.firstOrNull()
            else -> handler(method.name, args ?: emptyArray())
        }
    }
    @Suppress("UNCHECKED_CAST")
    return proxyInstance as T
}

internal fun unsupported(methodName: String): Nothing =
    error("Method $methodName is not supported in this test stub")
