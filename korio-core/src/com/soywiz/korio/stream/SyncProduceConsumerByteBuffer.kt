package com.soywiz.korio.stream

import com.soywiz.korio.util.indexOf
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.Semaphore

class SyncProduceConsumerByteBuffer : SyncOutputStream, SyncInputStream {
	companion object {
		private val EMPTY = byteArrayOf()
	}

	private var current: ByteArray = EMPTY
	private var currentPos = 0
	private val buffers = LinkedList<ByteArray>()
	private var availableInBuffers = 0
	private val availableInCurrent: Int get() = current.size - currentPos

	private val producedSema = Semaphore(0)

	val available: Int get() = availableInCurrent + availableInBuffers

	fun produce(data: ByteArray) = synchronized(this) {
		buffers += data
		availableInBuffers += data.size
		producedSema.release()
	}

	private fun useNextBuffer() = synchronized(this) {
		current = if (buffers.isEmpty()) EMPTY else buffers.removeFirst()
		currentPos = 0
		availableInBuffers -= current.size
	}

	private fun ensureCurrentBuffer() = synchronized(this) {
		if (availableInCurrent <= 0) {
			useNextBuffer()
		}
	}

	fun consume(data: ByteArray, offset: Int = 0, len: Int = data.size): Int = synchronized(this) {
		var totalRead = 0
		var remaining = len
		var outputPos = offset
		while (remaining > 0) {
			ensureCurrentBuffer()
			val readInCurrent = Math.min(availableInCurrent, len)
			if (readInCurrent <= 0) break
			System.arraycopy(current, currentPos, data, outputPos, readInCurrent)
			currentPos += readInCurrent
			remaining -= readInCurrent
			totalRead += readInCurrent
			outputPos += readInCurrent
		}
		totalRead
	}

	fun consume(len: Int): ByteArray = ByteArray(len).run { Arrays.copyOf(this, consume(this, 0, len)) }

	fun consumeUntil(end: Byte, including: Boolean = true): ByteArray = synchronized(this) {
		val out = ByteArrayOutputStream()
		while (true) {
			ensureCurrentBuffer()
			if (availableInCurrent <= 0) break // no more data!
			val p = current.indexOf(currentPos, end)
			val pp = if (p < 0) current.size else if (including) p + 1 else p
			val len = pp - currentPos
			if (len > 0) out.write(current, currentPos, len)
			currentPos += len
			if (p >= 0) break // found!
		}
		return out.toByteArray()
	}

	override fun write(buffer: ByteArray, offset: Int, len: Int) {
		produce(Arrays.copyOfRange(buffer, offset, offset + len))
	}

	override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		while (true) {
			if (len == 0) return 0
			val out = consume(buffer, offset, len)
			if (out == 0) {
				producedSema.acquire()
			} else {
				return out
			}
		}
	}
}