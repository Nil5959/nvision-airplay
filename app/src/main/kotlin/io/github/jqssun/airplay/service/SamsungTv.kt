package io.github.jqssun.airplay.service

import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Turns a Samsung (Tizen) TV off over its local network remote, since Fire OS can turn the TV on
 * over HDMI-CEC but never off. The first time, the TV asks to allow "N Vision AirPlay"; the token
 * it hands back is kept, with the TV's Wi-Fi MAC to find it again when its IP changes. Blocking calls.
 */
class SamsungTv(private val prefs: SharedPreferences) {
    private data class Tv(val ip: String, val mac: String)

    private var ip get() = prefs.getString("samsung_tv_ip", "")!!; set(v) = prefs.edit().putString("samsung_tv_ip", v).apply()
    private var mac get() = prefs.getString("samsung_tv_mac", "")!!; set(v) = prefs.edit().putString("samsung_tv_mac", v).apply()
    private var token get() = prefs.getString("samsung_tv_token", "")!!; set(v) = prefs.edit().putString("samsung_tv_token", v).apply()
    private var lastPairTry = 0L

    val paired get() = token.isNotEmpty()

    /** After AirPlay turned the TV on: ask every Samsung that answers to allow us (once; then at most daily). */
    fun pairIfNeeded() {
        if (paired || System.currentTimeMillis() - lastPairTry < 24 * 3600_000L) return
        lastPairTry = System.currentTimeMillis()
        val tvs = discover()
        Log.i(TAG, "pairing: ${tvs.size} Samsung TV(s) answered")
        if (tvs.isEmpty()) return
        val pool = Executors.newFixedThreadPool(tvs.size)
        try {
            val done = CountDownLatch(1)
            for (tv in tvs) pool.execute {
                val t = remote(tv.ip, "", null, 45_000)
                if (t != null && done.count > 0) { ip = tv.ip; mac = tv.mac; token = t; done.countDown() }
            }
            done.await(50, TimeUnit.SECONDS)
            Log.i(TAG, if (paired) "paired with $ip" else "not allowed on any TV")
        } finally { pool.shutdownNow() }
    }

    /** Power the paired TV off. False when not paired or unreachable. */
    fun powerOff(): Boolean {
        if (!paired) return false
        if (remote(ip, token, "KEY_POWER", 6_000) != null) return true
        // the TV may have a new IP: find it by its MAC and retry there
        val moved = discover().firstOrNull { it.mac.isNotEmpty() && it.mac == mac } ?: return false
        ip = moved.ip
        return remote(ip, token, "KEY_POWER", 6_000) != null
    }

    /** Open the remote channel, optionally click [key]; returns the token once the TV lets us in, else null. */
    private fun remote(ip: String, token: String, key: String?, waitMs: Long): String? {
        val name = Base64.encodeToString("N Vision AirPlay".toByteArray(), Base64.NO_WRAP)
        val url = "wss://$ip:8002/api/v2/channels/samsung.remote.control?name=$name" +
            if (token.isNotEmpty() && token != NO_TOKEN) "&token=$token" else ""
        val latch = CountDownLatch(1)
        var got: String? = null
        val ws = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val j = runCatching { JSONObject(text) }.getOrNull() ?: return
                when (j.optString("event")) {
                    "ms.channel.connect" -> {
                        got = j.optJSONObject("data")?.optString("token")?.takeIf { it.isNotEmpty() }
                            ?: token.ifEmpty { NO_TOKEN }
                        if (key != null) webSocket.send(JSONObject()
                            .put("method", "ms.remote.control")
                            .put("params", JSONObject().put("Cmd", "Click").put("DataOfCmd", key)
                                .put("Option", "false").put("TypeOfRemote", "SendRemoteKey")).toString())
                        latch.countDown()
                    }
                    "ms.channel.unauthorized", "ms.channel.timeOut" -> latch.countDown()
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { latch.countDown() }
        })
        latch.await(waitMs, TimeUnit.MILLISECONDS)
        if (got != null && key != null) Thread.sleep(800)  // let the click go out before closing
        ws.cancel()
        Log.i(TAG, "remote $ip key=$key -> ${if (got != null) "ok" else "no"}")
        return got
    }

    /** Samsung TVs answering on this /24 (one in standby usually doesn't). */
    private fun discover(): List<Tv> {
        val prefix = NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress?.substringBeforeLast('.') ?: return emptyList()
        val pool = Executors.newFixedThreadPool(48)
        try {
            return (1..254).map { i -> pool.submit<Tv?> { info("$prefix.$i") } }
                .mapNotNull { runCatching { it.get(10, TimeUnit.SECONDS) }.getOrNull() }
        } finally { pool.shutdownNow() }
    }

    private fun info(ip: String): Tv? = try {
        Socket().use { it.connect(InetSocketAddress(ip, 8001), 400) }
        client.newCall(Request.Builder().url("http://$ip:8001/api/v2/").build()).execute().use { r ->
            val d = JSONObject(r.body!!.string()).optJSONObject("device")
            if (d != null && d.optString("type").contains("Samsung")) Tv(ip, d.optString("wifiMac")) else null
        }
    } catch (_: Exception) { null }

    private companion object {
        const val TAG = "SamsungTv"
        const val NO_TOKEN = "-"  // older TVs let us in without one
        // the TV's remote uses a self-signed certificate on the local network
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(c: Array<X509Certificate>, a: String) {}
            override fun checkServerTrusted(c: Array<X509Certificate>, a: String) {}
            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        }
        val client: OkHttpClient by lazy {
            val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustAll), null) }
            OkHttpClient.Builder().sslSocketFactory(ssl.socketFactory, trustAll).hostnameVerifier { _, _ -> true }
                .connectTimeout(3, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build()
        }
    }
}
