package com.grahamlea.forbiddenisland

import java.util.*

data class ImmutableList<out E>(private val inner:List<E>) : List<E> by inner
fun <T> immutableListOf(element: T): ImmutableList<T> = java.util.Collections.singletonList(element).immutable()
fun <T> immutableListOf(vararg elements: T): ImmutableList<T> = listOf(*elements).immutable()
infix operator fun <E> ImmutableList<E>.plus(e: E): ImmutableList<E> = ImmutableList(this as List<E> + e)
infix operator fun <E> ImmutableList<E>.plus(es: Collection<E>): ImmutableList<E> = ImmutableList(this as List<E> + es)
fun <E> List<E>.immutable() = this as? ImmutableList<E> ?: ImmutableList(this)

data class ImmutableMap<K, out V>(private val inner: Map<K, V>) : Map<K, V> by inner
fun <K, V> immutableMapOf(pair: Pair<K, V>): ImmutableMap<K, V> = java.util.Collections.singletonMap(pair.first, pair.second).immutable()
fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = mapOf(*pairs).immutable()
fun <K, V> Map<K, V>.immutable() = this as? ImmutableMap<K, V> ?: ImmutableMap(this)
infix operator fun <K, V> ImmutableMap<K, V>.plus(p: Pair<K, V>): ImmutableMap<K, V> = ImmutableMap(this as Map<K, V> + p)

inline fun <reified T : Enum<T>> shuffled(random: Random = Random(), count: Int = enumValues<T>().size): ImmutableList<T>
        = enumValues<T>().toList().shuffled(random).take(count).immutable()
