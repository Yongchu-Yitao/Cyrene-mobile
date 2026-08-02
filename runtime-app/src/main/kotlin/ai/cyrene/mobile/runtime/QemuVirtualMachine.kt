package ai.cyrene.mobile.runtime

import android.content.Context
import android.net.ConnectivityManager
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.Build
import android.util.Base64
import android.util.Log
import com.max2idea.android.limbo.jni.VMExecutor
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

data class VmHealth(
    val version: String,
    val network: String,
    val workspace: String,
)

data class VmCommandResult(val stdout: String, val stderr: String, val exitCode: Int)

class QemuVirtualMachine(
    private val context: Context,
    private val bundle: RuntimeImageBundle,
    private val rootfsDisk: File,
) : AutoCloseable {
    private val executor = VMExecutor()
    private val vmThread = Executors.newSingleThreadExecutor()
    private val socketPath = File(context.filesDir, "qemu-serial.sock")
    private var future: Future<*>? = null
    private var socket: LocalSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    @Volatile private var nativeResult: String? = null
    @Volatile private var started = false
    @Volatile var lastHealth: VmHealth? = null
        private set

    @Synchronized
    fun start(deadlineEpochMs: Long): VmHealth {
        if (started) return ping(deadlineEpochMs)
        require(Build.SUPPORTED_64_BIT_ABIS.any { it == "arm64-v8a" || it == "x86_64" }) {
            "QEMU runtime requires a 64-bit ARM or x86 Android device"
        }
        rootfsDisk.parentFile?.mkdirs()
        if (socketPath.exists()) socketPath.delete()
        val libraryPath = File(context.applicationInfo.nativeLibraryDir, "libqemu-system-x86_64.so")
        val libraryLocation = libraryPath.takeIf(File::isFile)?.absolutePath ?: libraryPath.name
        val upstreamDns = upstreamDnsAddress()
        File(bundle.directory, "etc/resolv.conf").apply {
            parentFile?.mkdirs()
            writeText("nameserver $upstreamDns\n")
        }
        val parameters = arrayOf(
            "libqemu-system-x86_64.so",
            "-machine", "pc",
            "-cpu", "qemu64",
            "-smp", "1",
            "-m", "256",
            "-kernel", bundle.kernel.absolutePath,
            "-initrd", bundle.initramfs.absolutePath,
            "-append", "console=ttyS0 rdinit=/init panic=-1 loglevel=4",
            "-nodefaults",
            "-display", "none",
            "-monitor", "none",
            "-chardev", "socket,id=cyrene,path=${socketPath.absolutePath},server=on,wait=off",
            "-serial", "chardev:cyrene",
            "-drive", "if=none,id=rootfs,file=${rootfsDisk.absolutePath},format=raw,cache=writeback",
            "-device", "virtio-blk-pci,drive=rootfs",
            // Limbo's Android libc shim cannot read Android's /etc/resolv.conf,
            // so give slirp the active Android network's upstream resolver. The
            // guest still uses QEMU's DNS proxy at 10.0.2.3.
            "-netdev", "user,id=net0,restrict=off,dns=$upstreamDns",
            "-device", "virtio-net-pci,netdev=net0",
            "-no-reboot",
            "-overcommit", "mem-lock=off",
            "-L", bundle.directory.absolutePath,
        )
        future = vmThread.submit {
            nativeResult = runCatching {
                executor.start(
                    context.filesDir.absolutePath,
                    bundle.directory.absolutePath,
                    libraryPath.name,
                    libraryLocation,
                    0,
                    parameters,
                )
            }.fold({ it }, { "QEMU failed: ${it.message}" })
        }
        connect(deadlineEpochMs)
        started = true
        return waitForReady(deadlineEpochMs)
    }

    @Synchronized
    fun ping(deadlineEpochMs: Long): VmHealth {
        ensureRunning()
        val id = token()
        send("CYRENE_PING $id -")
        val line = readUntil(deadlineEpochMs) { it.startsWith("CYRENE_PONG $id ") }
        val parts = line.split(' ', limit = 5)
        check(parts.size == 5) { "Malformed QEMU guest health response" }
        return VmHealth(parts[2], parts[3], parts[4]).also { lastHealth = it }
    }

    private fun waitForReady(deadlineEpochMs: Long): VmHealth {
        val line = readUntil(deadlineEpochMs) { it.startsWith("CYRENE_VM_READY ") }
        val parts = line.split(' ', limit = 4)
        check(parts.size == 4) { "Malformed QEMU guest ready response" }
        return VmHealth(parts[1], parts[2], parts[3]).also { lastHealth = it }
    }

    @Synchronized
    fun execute(command: String, deadlineEpochMs: Long): VmCommandResult {
        ensureRunning()
        val id = token()
        val payload = Base64.encodeToString(command.toByteArray(), Base64.NO_WRAP)
        send("CYRENE_EXEC $id $payload")
        val line = try {
            readUntil(deadlineEpochMs) { it.startsWith("CYRENE_RESULT $id ") }
        } catch (error: Throwable) {
            close()
            throw error
        }
        val parts = line.split(' ', limit = 5)
        check(parts.size == 5) { "Malformed QEMU guest command response" }
        fun decode(value: String): String = if (value == "-") "" else
            Base64.decode(value, Base64.DEFAULT).toString(Charsets.UTF_8)
        return VmCommandResult(decode(parts[3]), decode(parts[4]), parts[2].toInt())
    }

    @Synchronized
    override fun close() {
        if (started) {
            runCatching {
                val id = token()
                send("CYRENE_SHUTDOWN $id -")
            }
            runCatching { executor.stop(0) }
        }
        runCatching { reader?.close() }
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        reader = null
        writer = null
        socket = null
        started = false
        future?.let { runCatching { it.get(5, TimeUnit.SECONDS) } }
        future = null
    }

    fun describeFailure(): String = nativeResult ?: "QEMU guest is not running"

    private fun connect(deadlineEpochMs: Long) {
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadlineEpochMs) {
            if (future?.isDone == true) error(nativeResult ?: "QEMU stopped before its serial channel opened")
            val candidate = LocalSocket()
            try {
                candidate.connect(LocalSocketAddress(socketPath.absolutePath, LocalSocketAddress.Namespace.FILESYSTEM))
                socket = candidate
                reader = BufferedReader(InputStreamReader(candidate.inputStream, Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(candidate.outputStream, Charsets.UTF_8))
                return
            } catch (error: Throwable) {
                lastError = error
                runCatching { candidate.close() }
                Thread.sleep(100)
            }
        }
        error("Timed out connecting to QEMU guest serial channel: ${lastError?.message}")
    }

    private fun send(line: String) {
        writer?.apply { write(line); newLine(); flush() } ?: error("QEMU serial channel is unavailable")
    }

    private fun readUntil(deadlineEpochMs: Long, predicate: (String) -> Boolean): String {
        while (System.currentTimeMillis() < deadlineEpochMs) {
            val remaining = (deadlineEpochMs - System.currentTimeMillis()).coerceIn(1, 2_000).toInt()
            socket?.soTimeout = remaining
            val line = try {
                reader?.readLine()
            } catch (_: java.io.InterruptedIOException) {
                null
            } catch (error: java.io.IOException) {
                if (error.message?.contains("Try again", ignoreCase = true) == true ||
                    error.message?.contains("EAGAIN", ignoreCase = true) == true
                ) null else throw error
            }
            if (line != null) {
                Log.d(SERIAL_TAG, line.take(4000))
                if (predicate(line)) return line
            }
            if (future?.isDone == true) error(nativeResult ?: "QEMU stopped unexpectedly")
        }
        error("QEMU guest request timed out")
    }

    private fun ensureRunning() {
        check(started && future?.isDone != true) { describeFailure() }
    }

    private fun token(): String = UUID.randomUUID().toString().replace("-", "")

    private fun upstreamDnsAddress(): String {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        return runCatching {
            connectivity.getLinkProperties(connectivity.activeNetwork)
                ?.dnsServers
                ?.firstOrNull { it is Inet4Address }
                ?.hostAddress
        }.getOrNull() ?: "1.1.1.1"
    }

    companion object {
        private const val SERIAL_TAG = "CyreneQemuSerial"
    }
}
