package io.github.itsmelissadev.swiftsense.service.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import java.net.InetAddress

@RequiresApi(Build.VERSION_CODES.R)
class AdbMdns(
    private val context: Context,
    private val serviceType: String,
    private val onResolved: (host: String, port: Int) -> Unit
) {
    companion object {
        const val TLS_PAIRING = "_adb-tls-pairing._tcp"
        const val TLS_CONNECT = "_adb-tls-connect._tcp"
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false

    fun start() {
        if (isDiscovering || nsdManager == null) return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                isDiscovering = true
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val type = serviceInfo.serviceType
                if (type == serviceType || type == "$serviceType." || type.contains(serviceType.trimStart('.'))) {
                    resolveService(serviceInfo)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            }
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            discoveryListener = listener
            isDiscovering = true
        } catch (_: Exception) {
            isDiscovering = false
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val nsd = nsdManager ?: return
        try {
            nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                }

                override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                    val port = resolvedService.port
                    val hostAddress = resolvedService.host?.hostAddress ?: "127.0.0.1"
                    if (port > 0) {
                        onResolved(hostAddress, port)
                    }
                }
            })
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!isDiscovering) return
        discoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (_: Exception) {
            }
        }
        discoveryListener = null
        isDiscovering = false
    }
}
