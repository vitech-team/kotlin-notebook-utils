package com.vitechteam.wrappers.maestro

import java.io.Closeable
import java.lang.IllegalStateException

// SDK imports (provided by io.modelcontextprotocol:kotlin-sdk-client)
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Thin Kotlin wrapper over Maestro MCP client for simple flow execution from JVM/Kotlin notebooks.
 */
class MaestroWrapper private constructor(
    private val process: Process,
    private val client: Client,
    private val defaultDeviceId: String
) : Closeable {

    fun isConnected(): Boolean = client.transport != null

    /**
     * Runs a Maestro YAML flow via the Maestro MCP tool `run_flow` which accepts YAML content.
     * @param deviceId Target device id (e.g., emulator-5554 or iOS device id)
     * @param flowYaml YAML content of the flow to execute
     * @param env Optional environment variables to inject
     * @return stringified result returned by the tool
     */
    fun runFlow(deviceId: String, flowYaml: String, env: Map<String, String> = emptyMap()): CallToolResult {
        val params = buildMap<String, Any?> {
            put("device_id", deviceId)
            put("flow_yaml", flowYaml)
            if (env.isNotEmpty()) put("env", env)
        }
        val result = runBlocking {
            client.callTool("run_flow", params)
        }
        if (result.isError == true) {
            val msg = result.content
                .filterIsInstance<TextContent>()
                .firstOrNull()
                ?.text
            throw RuntimeException("Error while executing flow call: ${msg ?: result.toString()}")
        }
        return result
    }

    /**
     * Overload of runFlow that uses the defaultDeviceId selected at startup.
     */
    fun runFlow(flowYaml: String, env: Map<String, String> = emptyMap()): CallToolResult =
        runFlow(defaultDeviceId, flowYaml, env)

    override fun close() {
        try {
            runBlocking { client.close() }
        } catch (_: Exception) {}
        try { process.destroy() } catch (_: Exception) {}
    }

    companion object {
        /**
         * Starts `maestro mcp` as a subprocess and connects to it over stdio using MCP Kotlin SDK.
         */
        @JvmStatic
        fun start(requestedDeviceId: String? = null): MaestroWrapper {
            val builder = ProcessBuilder("maestro", "mcp")
                .redirectError(ProcessBuilder.Redirect.INHERIT)
            val process = try {
                builder.start()
            } catch (e: Exception) {
                throw IllegalStateException("Failed to start 'maestro mcp'. Ensure Maestro CLI is installed and on PATH. ", e)
            }

            val transport = StdioClientTransport(
                input = process.inputStream.asSource().buffered(),
                output = process.outputStream.asSink().buffered()
            )

            val client = Client(
                clientInfo = Implementation(
                    name = "vitechteam-maestro-client",
                    version = "0.0.1"
                )
            )

            runBlocking { client.connect(transport) }

            // Resolve device id using Maestro MCP list_devices tool
            val resolvedDeviceId = runBlocking { resolveDeviceId(client, requestedDeviceId) }

            return MaestroWrapper(process, client, resolvedDeviceId)
        }

        private suspend fun resolveDeviceId(client: Client, requestedDeviceId: String?): String {
            val res = client.callTool("list_devices", emptyMap<String, Any>())

            if (res.isError == true) {
                val errMsg = res.content
                    .filterIsInstance<TextContent>()
                    .firstOrNull()
                    ?.text
                throw IllegalStateException(
                    "Maestro 'list_devices' tool returned error: ${errMsg ?: res.toString()}"
                )
            }

            val rawText = res.content
                .filterIsInstance<TextContent>()
                .firstOrNull()
                ?.text
                ?: throw IllegalStateException(
                    "Unexpected list_devices response: expected TextContent but got ${res.content.map { it::class.simpleName }}"
                )

            val json = try {
                Json.parseToJsonElement(rawText).jsonObject
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse list_devices result: ${e.message}. Raw text: ${rawText}")
            }
            val devices = json["devices"]?.jsonArray
                ?: throw IllegalStateException("list_devices returned no 'devices' array")

            fun deviceIds(): List<String> = devices.mapNotNull { it.jsonObject["device_id"]?.jsonPrimitive?.content }
            fun connectedIds(): List<String> = devices.filter {
                it.jsonObject["connected"]?.jsonPrimitive?.booleanOrNull == true
            }.mapNotNull { it.jsonObject["device_id"]?.jsonPrimitive?.content }

            return if (requestedDeviceId != null) {
                val all = deviceIds()
                if (!all.contains(requestedDeviceId)) {
                    throw IllegalStateException("Device '$requestedDeviceId' not found. Available: ${all.joinToString()}")
                }
                requestedDeviceId
            } else {
                val connected = connectedIds()
                when {
                    connected.isNotEmpty() -> connected.first()
                    devices.isNotEmpty() -> devices.first().jsonObject["device_id"]!!.jsonPrimitive.content
                    else -> throw IllegalStateException("No devices available. Please start a device and try again.")
                }
            }
        }
    }
}
