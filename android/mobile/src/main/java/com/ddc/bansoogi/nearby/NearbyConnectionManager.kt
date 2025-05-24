/* ────────────────────────── NearbyConnectionManager.kt ────────────────────────── */
package com.ddc.bansoogi.nearby

import android.content.Context
import android.util.Log
import com.ddc.bansoogi.common.wear.communication.processor.StaticEventProcessor
import com.ddc.bansoogi.nearby.data.BansoogiFriend
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

class NearbyConnectionManager(private val ctx: Context) {

    companion object {
        private const val TAG        = "NearbyManager"
        private const val SERVICE_ID = NearbyConstants.SERVICE_ID
        private val STRATEGY         = Strategy.P2P_CLUSTER

        /* 메시지 타입 바이트 */
        const val TYPE_NICK        : Byte = 0x01
        const val TYPE_STATIC_WARN : Byte = 0x02       // ★ 추가
        const val TYPE_ALERT       : Byte = 0x03
        const val TYPE_STATE       : Byte = 0x04
    }

    private val client = Nearby.getConnectionsClient(ctx)
    private val gson   = Gson()

    /* 코루틴 스코프 (수신 처리용) */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /* ------------------------ 상태 ------------------------ */
    private var isAdvertising  = false
    private var isDiscovering  = false
    private var myNickname     = ""
    private val pendingConnect = mutableSetOf<String>()     // request ↔ accept 진행 중

    /* --------------------- Peer 목록 Flow ----------------- */
    private val _peers = MutableStateFlow<List<BansoogiFriend>>(emptyList())
    val peers: StateFlow<List<BansoogiFriend>> = _peers.asStateFlow()

    /* ======================================================================== */
    /*                               PUBLIC API                                 */
    /* ======================================================================== */
    fun start(userNickname: String) {
        if (isAdvertising || isDiscovering) return          // ✅ 중복 차단
        myNickname = userNickname
        startAdvertising(); startDiscovery()
    }

    fun stop() {
        client.stopAllEndpoints(); client.stopAdvertising(); client.stopDiscovery()
        isAdvertising = false; isDiscovering = false
        pendingConnect.clear(); _peers.value = emptyList()
    }

    /** ★ UI → 특정 피어로 STATIC_WARN JSON 전송 */
    fun sendStaticWarnTo(endpointId: String, type: String, duration: Int) {
        val json = gson.toJson(WarnDto(type, duration))
        val payload = Payload.fromBytes(byteArrayOf(TYPE_STATIC_WARN) + json.encode())
        client.sendPayload(endpointId, payload)
    }

    /* ======================================================================== */
    /*                           Advertising / Discovery                        */
    /* ======================================================================== */
    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        client.startAdvertising(myNickname, SERVICE_ID, connLifecycleCb, options)
            .addOnSuccessListener { isAdvertising = true; Log.d(TAG, "Advertising started") }
            .addOnFailureListener { Log.e(TAG, "Advertising fail: $it") }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        client.startDiscovery(SERVICE_ID, endpointDiscoveryCb, options)
            .addOnSuccessListener { isDiscovering = true; Log.d(TAG, "Discovery started") }
            .addOnFailureListener { Log.e(TAG, "Discovery fail: $it") }
    }

    /* ======================================================================== */
    /*                                 Callbacks                                */
    /* ======================================================================== */
    private val endpointDiscoveryCb = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
            if (pendingConnect.contains(id) || _peers.value.any { it.endpointId == id }) return
            Log.d(TAG, "Found $id (${info.endpointName})")
            pendingConnect += id
            client.requestConnection(myNickname, id, connLifecycleCb)
        }
        override fun onEndpointLost(id: String) = removePeer(id)
    }

    private val connLifecycleCb = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            client.acceptConnection(id, payloadCb)          // ✅ accept
        }
        override fun onConnectionResult(id: String, res: ConnectionResolution) {
            pendingConnect -= id
            if (res.status.isSuccess) {
                Log.d(TAG, "Connected $id")
                sendNickname(id)
                addOrUpdatePeer(id, "<알 수 없음>")
            } else removePeer(id)
        }
        override fun onDisconnected(id: String) = removePeer(id)
    }

    private val payloadCb = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            when (bytes[0]) {
                TYPE_NICK -> addOrUpdatePeer(id, bytes.drop(1).toByteArray().decode())
                TYPE_STATIC_WARN -> {
                    /* 기존 StaticEventProcessor 로 그대로 전달 */
                    StaticEventProcessor.handleWarn(
                        ctx, scope, bytes.drop(1).toByteArray()
                    )
                }
                TYPE_ALERT  -> Log.d(TAG, "Alert from $id : ${bytes.drop(1).toByteArray().decode()}")
                TYPE_STATE  -> Log.d(TAG, "State from $id : ${bytes.drop(1).toByteArray().decode()}")
            }
        }
        override fun onPayloadTransferUpdate(id: String, update: PayloadTransferUpdate) {}
    }

    /* ======================================================================== */
    /*                                 Helpers                                  */
    /* ======================================================================== */
    private data class WarnDto(val type: String, val duration: Int)

    private fun sendNickname(endpointId: String) = client.sendPayload(
        endpointId,
        Payload.fromBytes(byteArrayOf(TYPE_NICK) + myNickname.encode())
    )

    private fun addOrUpdatePeer(id: String, nickname: String) {
        _peers.value = _peers.value.toMutableList().apply {
            val idx = indexOfFirst { it.endpointId == id }
            if (idx >= 0) this[idx] = this[idx].copy(nickname = nickname)
            else           add(BansoogiFriend(id, nickname))
        }
    }

    private fun removePeer(id: String) {
        _peers.value = _peers.value.filterNot { it.endpointId == id }
        pendingConnect -= id
    }

    private fun String.encode()            = toByteArray(StandardCharsets.UTF_8)
    private fun ByteArray.decode(): String = String(this, StandardCharsets.UTF_8)
}
