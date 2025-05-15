package io.airbyte.cdk.load.file

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.ServerSocketChannel

class SocketWriterOutputStream(
    private val socketPath: String
): OutputStream() {
    private val log = KotlinLogging.logger {}
    private var outputStream: OutputStream? = null
    private var serverSocketChannel: ServerSocketChannel? = null
    private var socketFile: File? = null

    fun create(): File {
        socketFile = File(socketPath)
        if (socketFile!!.exists()) {
            log.info { "Deleting existing socket file $socketFile" }
            socketFile!!.delete()
        }
        log.info { "Creating socket file $socketFile" }

        val address = UnixDomainSocketAddress.of(socketFile!!.toPath())
        serverSocketChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        serverSocketChannel!!.bind(address)

        return socketFile!!
    }

    private fun accept(): OutputStream {
        if (socketFile == null) {
            create()
        }
        val socketChannel = serverSocketChannel!!.accept()

        log.info { "Connected to socket $socketFile for writing" }

        return Channels.newOutputStream(socketChannel)
    }

    override fun write(b: Int) {
        outputStream = outputStream ?: accept()
        outputStream?.write(b)
    }

    override fun close() {
        super.close()
        log.info { "Closing socket file $socketPath"}
        outputStream?.close()
    }
}
