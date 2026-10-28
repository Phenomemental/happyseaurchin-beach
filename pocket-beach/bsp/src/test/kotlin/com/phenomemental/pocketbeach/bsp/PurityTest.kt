package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The core is plain Kotlin on the JVM: not one compiled class of it names an
 * Android API, so it runs as it is on a Windows or Mac laptop. Read straight off
 * the class files the module builds.
 */
class PurityTest {
    @Test
    fun noCoreClassTouchesAndroid() {
        val classes = File(PocketBsp::class.java.protectionDomain.codeSource.location.toURI())
        assertTrue(classes.isDirectory, "expected the module's class folder, found $classes")
        val files = classes.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.toList()
        assertTrue(files.size > 20, "found only ${files.size} classes in $classes")
        val touching = files.filter { f ->
            val text = String(f.readBytes(), Charsets.ISO_8859_1)
            text.contains("android/") || text.contains("androidx/")
        }
        assertTrue(touching.isEmpty(), "classes that reach Android: ${touching.map { it.relativeTo(classes) }}")
    }
}
