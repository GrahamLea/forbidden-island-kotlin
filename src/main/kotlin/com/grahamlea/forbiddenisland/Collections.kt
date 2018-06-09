package com.grahamlea.forbiddenisland

import java.util.*

/**
 * Returns a shuffled [ImmutableList] of the specified (or inferred) Enum, or a random subset thereof as specified.
 *
 * @param count the number of enum values to include in the returned list, or null to include all values
 */
inline fun <reified T : Enum<T>> shuffled(random: Random = Random(), count: Int? = null): ImmutableList<T>
    = enumValues<T>().toList().shuffled(random).let { sl -> count?.let { sl.take(it) } ?: sl }.imm()

/**
 * A truly immutable list.
 *
 * Unlike [List], it is not an interface which might be implemented by a mutable list, but a class which contains an
 * inaccessible copy of the items provided to the list at construction time.
 *
 * @see immListOf
 */
data class ImmutableList<out E> (private val inner:List<E>) : List<E> by ArrayList(inner) {
    override fun toString() = "@" + inner.toString()
}

/** Converts any [List] to an [ImmutableList] */
fun <E> List<E>.imm() = this as? ImmutableList<E> ?: ImmutableList(this)

/** Returns an [ImmutableList] of a single element */
fun <T> immListOf(element: T): ImmutableList<T> = java.util.Collections.singletonList(element).imm()

/** Returns an [ImmutableList] of the provided elements */
fun <T> immListOf(vararg elements: T): ImmutableList<T> = listOf(*elements).imm()

/** Returns a new [ImmutableList] with the provided element appended to this list  */
infix operator fun <E> ImmutableList<E>.plus(e: E): ImmutableList<E> = ImmutableList(this as List<E> + e)

/** Returns a new [ImmutableList] with the elements of the provided collection appended to this list  */
infix operator fun <E> ImmutableList<E>.plus(es: Collection<E>): ImmutableList<E> = ImmutableList(this as List<E> + es)

/**
 * Removes __one__ instance of each of the items in the given list from this list.
 *
 * Note that this is different to [List.minus] (taking an Iterable), which removes _all_ instances of the items in the
 * argument from the receiver.
 */
fun <E> ImmutableList<E>.subtract(es: Iterable<E>): ImmutableList<E> {
    val undesired = es.toMutableList()
    return this.filter { !undesired.remove(it) }.imm()
}

/**
 * A truly immutable map.
 *
 * Unlike [Map], it is not an interface which might be implemented by a mutable map, but a class which contains an
 * inaccessible copy of the pairs provided to the map at construction time.
 *
 * @see immMapOf
 */
data class ImmutableMap<K, out V>(private val inner: Map<K, V>) : Map<K, V> by LinkedHashMap(inner) {
    override fun toString() = "@" + inner.toString()
}

/** Converts any [Map] to an [ImmutableMap] */
fun <K, V> Map<K, V>.imm() = this as? ImmutableMap<K, V> ?: ImmutableMap(this)

/** Returns an [ImmutableMap] of a single pair */
fun <K, V> immMapOf(pair: Pair<K, V>): ImmutableMap<K, V> = java.util.Collections.singletonMap(pair.first, pair.second).imm()

/** Returns an [ImmutableMap] of the provided pairs */
fun <K, V> immMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = mapOf(*pairs).imm()

/** Returns a new [ImmutableList] with the provided pair added to this map  */
infix operator fun <K, V> ImmutableMap<K, V>.plus(p: Pair<K, V>): ImmutableMap<K, V> = ImmutableMap(this as Map<K, V> + p)

/**
 * A truly immutable and sorted set.
 *
 * Unlike [SortedSet], it is not an interface which might be implemented by a mutable map, but a class which contains an
 * inaccessible copy of the pairs provided to the map at construction time.
 *
 * @see immSetOf
 */
data class ImmutableSet<E: Comparable<E>>(private val inner:Set<E>) : Set<E>, SortedSet<E> by TreeSet(inner) {
    override fun toString() = "@" + inner.toString()
}

/** Converts any [Set] to an [ImmutableSet] */
fun <E: Comparable<E>> Set<E>.imm() = this as? ImmutableSet<E> ?: ImmutableSet(this)

/** Returns an [ImmutableSet] of a single element */
fun <T: Comparable<T>> immSetOf(element: T): ImmutableSet<T> = java.util.Collections.singleton(element).imm()

/** Returns an [ImmutableSet] of the provided elements */
fun <T: Comparable<T>> immSetOf(vararg elements: T): ImmutableSet<T> = sortedSetOf(*elements).imm()
