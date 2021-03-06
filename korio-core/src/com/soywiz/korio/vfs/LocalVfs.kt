package com.soywiz.korio.vfs

import com.soywiz.korio.service.Services
import java.io.File

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = LocalVfs(base)
		operator fun get(base: File) = LocalVfs(base)
	}
}

fun LocalVfs(base: String): VfsFile = VfsFile(localVfsProvider(), base)
fun TempVfs() = LocalVfs(System.getProperty("java.io.tmpdir"))
fun LocalVfs(base: File): VfsFile = LocalVfs(base.absolutePath)
fun JailedLocalVfs(base: File): VfsFile = LocalVfs(base.absolutePath).jail()
fun JailedLocalVfs(base: String): VfsFile = LocalVfs(base).jail()
suspend fun File.open(mode: VfsOpenMode) = LocalVfs(this).open(mode)

fun CacheVfs() = LocalVfs(localVfsProvider.getCacheFolder()).jail()
fun ExternalStorageVfs() = LocalVfs(localVfsProvider.getExternalStorageFolder()).jail()

abstract class LocalVfsProvider : Services.Impl() {
	abstract operator fun invoke(): LocalVfs
	open fun getCacheFolder(): String = System.getProperty("java.io.tmpdir")
	open fun getExternalStorageFolder(): String = System.getProperty("java.io.tmpdir")
}

fun File.toVfs() = LocalVfs(this)