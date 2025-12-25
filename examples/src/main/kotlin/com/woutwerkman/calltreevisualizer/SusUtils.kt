import java.io.BufferedReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.use

inline fun <T, K> SusSequence<T>.groupingBy(crossinline keySelector: suspend (T) -> K): SusGrouping<T, K> {
    return object : SusGrouping<T, K> {
        override suspend fun sourceIterator(): SusIterator<T> = this@groupingBy.iterator()
        override suspend fun keyOf(element: T): K = keySelector(element)
    }
}

suspend fun <T> SusSequence<T>.toList(): List<T> {
    val it = iterator()
    if (!it.hasNext())
        return emptyList()
    val element = it.next()
    if (!it.hasNext())
        return listOf(element)
    val dst = ArrayList<T>()
    dst.add(element)
    while (it.hasNext()) dst.add(it.next())
    return dst
}

suspend fun <S, T : S, K> SusGrouping<T, K>.reduce(
    operation: suspend (key: K, accumulator: S, element: T) -> S
): Map<K, S> =
    aggregate { key, acc, e, first ->
        @Suppress("UNCHECKED_CAST")
        if (first) e else operation(key, acc as S, e)
    }

suspend fun <T, K, R> SusGrouping<T, K>.aggregate(
    operation: suspend (key: K, accumulator: R?, element: T, first: Boolean) -> R
): Map<K, R> = aggregateTo(mutableMapOf(), operation)

suspend fun <T, K, R, M : MutableMap<in K, R>> SusGrouping<T, K>.aggregateTo(
    destination: M,
    operation: suspend (key: K, accumulator: R?, element: T, first: Boolean) -> R
): M {
    for (e in this.sourceIterator()) {
        val key = keyOf(e)
        val accumulator = destination[key]
        destination[key] = operation(key, accumulator, e, accumulator == null && !destination.containsKey(key))
    }
    return destination
}

interface SusGrouping<T, out K> {
    suspend fun sourceIterator(): SusIterator<T>
    suspend fun keyOf(element: T): K
}

fun <T> Iterable<T>.susSequence(): SusSequence<T> = object : SusSequence<T> {
    override suspend fun iterator(): SusIterator<T> = object : SusIterator<T> {
        private val iterator = this@susSequence.iterator()
        override suspend fun next(): T = iterator.next()
        override suspend fun hasNext(): Boolean = iterator.hasNext()
    }
}

suspend fun <T> Path.suseLines(charset: Charset = Charsets.UTF_8, block: suspend (SusSequence<String>) -> T): T =
    Files.newBufferedReader(this, charset).use { block(it.lineSequence()) }

suspend fun BufferedReader.lineSequence(): SusSequence<String> = SusLinesSequence(this)

private class SusLinesSequence(private val reader: BufferedReader) : SusSequence<String> {
    override suspend fun iterator(): SusIterator<String> {
        return object : SusIterator<String> {
            private var nextValue: String? = null
            private var done = false

            override suspend fun hasNext(): Boolean {
                if (nextValue == null && !done) {
                    nextValue = reader.readLine()
                    if (nextValue == null) done = true
                }
                return nextValue != null
            }

            override suspend fun next(): String {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                val answer = nextValue
                nextValue = null
                return answer!!
            }
        }
    }
}

suspend fun <T, R> SusSequence<T>.map(transform: suspend (T) -> R): SusSequence<R> =
    TransformingSequence(this, transform)

internal class TransformingSequence<T, R>(
    private val sequence: SusSequence<T>,
    private val transformer: suspend (T) -> R
) : SusSequence<R> {
    override suspend fun iterator(): SusIterator<R> {
        val iterator = sequence.iterator()
        return object : SusIterator<R> {
            override suspend fun next(): R = transformer(iterator.next())
            override suspend fun hasNext(): Boolean = iterator.hasNext()
        }
    }
}

interface SusSequence<out T> {
    suspend operator fun iterator(): SusIterator<T>
}

interface SusIterator<out T> {
    suspend operator fun next(): T
    suspend operator fun hasNext(): Boolean
}

suspend operator fun <T> SusIterator<T>.iterator(): SusIterator<T> = this