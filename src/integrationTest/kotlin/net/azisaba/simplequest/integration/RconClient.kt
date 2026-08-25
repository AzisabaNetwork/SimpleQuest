package net.azisaba.simplequest.integration

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.Charset

/**
 * Lightweight Minecraft RCON client.
 *
 * Protocol (little-endian):
 *   - 4 bytes: length (remaining bytes)
 *   - 4 bytes: request ID
 *   - 4 bytes: type (3 = login, 2 = command, 0 = response)
 *   - N bytes: null-terminated payload (ASCII)
 *   - 1 byte: padding (0x00)
 */
class RconClient(
    private val host: String,
    private val port: Int,
    private val password: String,
) {
    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private var requestId = 0

    fun connect() {
        socket =
            Socket(host, port).also {
                // Grant/revoke for offline players triggers a blocking name lookup
                // server-side, which can exceed several seconds.
                it.soTimeout = 30_000
            }
        input = DataInputStream(socket!!.getInputStream())
        output = DataOutputStream(socket!!.getOutputStream())
        authenticate()
    }

    fun disconnect() {
        try {
            input?.close()
        } catch (_: Exception) {
        }
        try {
            output?.close()
        } catch (_: Exception) {
        }
        try {
            socket?.close()
        } catch (_: Exception) {
        }
    }

    fun executeCommand(command: String): String {
        val id = ++requestId
        sendPacket(id, TYPE_COMMAND, command)
        return readResponse(id)
    }

    private fun authenticate() {
        val id = ++requestId
        sendPacket(id, TYPE_LOGIN, password)
        readResponse(id) // throws if auth fails
    }

    private fun sendPacket(
        id: Int,
        type: Int,
        payload: String,
    ) {
        val payloadBytes = payload.toByteArray(Charsets.US_ASCII)

        // Build the whole packet and send it with a single write call.
        // The vanilla RCON server reads the packet with a single read() and
        // silently closes the connection if the packet arrives fragmented
        // (read returns <= 10 bytes or length != bytesRead - 4).
        val packet = ByteArray(12 + payloadBytes.size + 2)
        putLittleEndian(packet, 0, 10 + payloadBytes.size)
        putLittleEndian(packet, 4, id)
        putLittleEndian(packet, 8, type)
        payloadBytes.copyInto(packet, 12)
        // trailing null terminator + padding are already zero
        output!!.write(packet)
    }

    private fun putLittleEndian(
        b: ByteArray,
        offset: Int,
        value: Int,
    ) {
        b[offset] = (value and 0xFF).toByte()
        b[offset + 1] = ((value shr 8) and 0xFF).toByte()
        b[offset + 2] = ((value shr 16) and 0xFF).toByte()
        b[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun readResponse(expectedId: Int): String {
        val inp = input!!
        val length = readLittleEndianInt(inp)
        val id = readLittleEndianInt(inp)
        val type = readLittleEndianInt(inp)

        // payload = length - 10 (id + type bytes) - 2 (null terminator + padding)
        val payloadLen = length - 10
        val payload =
            if (payloadLen > 0) {
                val bytes = ByteArray(payloadLen)
                inp.readFully(bytes)
                String(bytes, Charsets.US_ASCII)
            } else {
                ""
            }
        // consume null terminator + padding
        inp.read()
        inp.read()

        if (id == -1) {
            throw RconException("Authentication failed")
        }

        return payload
    }

    companion object {
        private const val TYPE_LOGIN = 3
        private const val TYPE_COMMAND = 2

        private fun readLittleEndianInt(inp: DataInputStream): Int {
            val b0 = inp.readUnsignedByte()
            val b1 = inp.readUnsignedByte()
            val b2 = inp.readUnsignedByte()
            val b3 = inp.readUnsignedByte()
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }
    }
}

class RconException(
    message: String,
) : Exception(message)
