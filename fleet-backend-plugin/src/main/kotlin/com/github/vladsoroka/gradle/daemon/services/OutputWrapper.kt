package com.github.vladsoroka.gradle.daemon.services

import java.io.OutputStream

class OutputWrapper(private val listener: (String) -> Unit) : OutputStream() {
    private var buffer = StringBuilder()
    override fun write(b: Int) {
        buffer.append(b.toChar())
    }

    override fun flush() {
        listener(buffer.toString())
        buffer.setLength(0);
    }
}