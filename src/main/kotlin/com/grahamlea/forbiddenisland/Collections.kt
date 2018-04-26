package com.grahamlea.forbiddenisland

import java.util.*

data class ImmutableList<E>(private val inner:List<E>) : List<E> by inner
data class ImmutableMap<K, V>(private val inner: Map<K, V>) : Map<K, V> by inner

fun <E> List<E>.immutable() = this as? ImmutableList<E> ?: ImmutableList(this)
fun <K, V> Map<K, V>.immutable() = this as? ImmutableMap<K, V> ?: ImmutableMap(this)

infix operator fun <E> ImmutableList<E>.plus(e: E) = ImmutableList(this as List<E> + e)
infix operator fun <K, V> ImmutableMap<K, V>.plus(p: Pair<K, V>) = ImmutableMap(this as Map<K, V> + p)

fun <T> immutableListOf(element: T): ImmutableList<T> = java.util.Collections.singletonList(element).immutable()
fun <T> immutableListOf(vararg elements: T): ImmutableList<T> = listOf(*elements).immutable()

inline fun <reified T : Enum<T>> shuffled(random: Random = Random(), count: Int = enumValues<T>().size): ImmutableList<T>
        = enumValues<T>().toList().shuffled(random).take(count).immutable()

