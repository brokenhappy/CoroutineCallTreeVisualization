package com.woutwerkman.parallelReduceBenchmark

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.launch

suspend fun <S> List<S>.parallelReduceHeavyComputation(
    @OptIn(FlowPreview::class)
    concurrency: Int = DEFAULT_CONCURRENCY,
    minimumChunkSize: Int = 1,
    operation: suspend CoroutineScope.(acc: S, S) -> S
): S = parallelReduceInternal(concurrency, minimumChunkSize) { acc, s -> coroutineScope { operation(acc, s) } }

suspend fun <S> List<S>.parallelReduce(
    @OptIn(FlowPreview::class)
    concurrency: Int = DEFAULT_CONCURRENCY,
    minimumChunkSize: Int = 128,
    operation: (acc: S, S) -> S
): S = parallelReduceInternal(concurrency, minimumChunkSize) { acc, element -> operation(acc, element) }

private suspend inline fun <S> List<S>.parallelReduceInternal(
    concurrency: Int,
    minimumChunkSize: Int,
    crossinline operation: suspend (acc: S, S) -> S
): S {
    val effectiveConcurrency = concurrency.coerceAtMost((size / minimumChunkSize.toDouble()).roundUpToInt())
    if (effectiveConcurrency == 1) return reduce { acc, it -> operation(acc, it) }
    require(effectiveConcurrency > 0) { "Concurrency must be greater than 0" }
    require(isNotEmpty()) { "Cannot reduce an empty list" }
    val results = Array<Any?>(effectiveConcurrency) { null }
    coroutineScope {
        forEachEquallySplitRange(size, effectiveConcurrency) { chunkIndex, firstIndexOfChunk, lastIndexOfChunk ->
            launch {
                var acc = get(firstIndexOfChunk)
                for (i in (firstIndexOfChunk + 1)..lastIndexOfChunk) {
                    if (i % 100 == 0) ensureActive()
                    acc = operation(acc, get(i))
                }
                results[chunkIndex] = acc
            }
        }
    }
    var acc = results[0] as S
    for (index in 1 ..< results.size) {
        acc = operation(acc, results[index] as S)
    }
    return acc
}

private inline fun forEachEquallySplitRange(
    totalSize: Int,
    chunkCount: Int,
    onHeadAndTailIndicesOfChunk: (chunkIndex: Int, firstIndexOfChunk: Int, lastIndexOfChunk: Int) -> Unit,
) {
    require(chunkCount > 0) { "Chunks must be greater than 0" }
    val chunkSize = totalSize / chunkCount
    val numberOfChunksThatNeedToProcessOneExtra = totalSize % chunkCount
    var lastChunkEnd = -1
    for (chunkIndex in 0..<chunkCount) {
        val offsetForRemainder = if (chunkIndex < numberOfChunksThatNeedToProcessOneExtra) 0 else -1
        if (chunkSize + offsetForRemainder == -1) continue
        val head = lastChunkEnd + 1
        lastChunkEnd = head + chunkSize + offsetForRemainder
        onHeadAndTailIndicesOfChunk(chunkIndex, head, lastChunkEnd)
    }
}

private fun Double.roundUpToInt(): Int = (this + 0.5).toInt()