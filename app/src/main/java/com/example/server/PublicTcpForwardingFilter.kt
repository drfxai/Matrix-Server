package com.example.server

import org.apache.sshd.common.session.Session
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.server.forward.TcpForwardingFilter
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Authenticated direct-tcpip forwarding for a Mihomo SSH outbound.
 * Never provide remote listen/forwarded-tcpip; X11 and agent forwarding remain disabled.
 * Even with authentication, avoid connecting the proxy to the phone's private LAN,
 * loopback, link-local services, or known special-purpose addresses.
 */
internal object PublicTcpForwardingFilter : TcpForwardingFilter {
    override fun canListen(address: SshdSocketAddress, session: Session): Boolean = false

    override fun canConnect(
        type: TcpForwardingFilter.Type,
        address: SshdSocketAddress,
        session: Session
    ): Boolean =
        type == TcpForwardingFilter.Type.Direct &&
            address.port in 1..65535 &&
            PublicDestinationPolicy.isAllowed(address.hostName)
}

/**
 * Conservative resolver check at admission time. This is NOT a substitute for firewall
 * egress filtering (DNS may change between this check and the actual socket connect).
 * Only expose the SSH listener on trusted LAN or on a separately secured private overlay.
 */
internal object PublicDestinationPolicy {
    fun isAllowed(host: String): Boolean {
        val name = host.trim().trimEnd('.')
        if (name.isBlank() || name.length > 253 || name.contains('%')) return false
        if (name.equals("localhost", true) ||
            name.endsWith(".localhost", true) ||
            name.endsWith(".local", true) ||
            name.endsWith(".lan", true) ||
            name.endsWith(".internal", true)
        ) return false
        return try {
            val addresses = InetAddress.getAllByName(name)
            addresses.isNotEmpty() && addresses.all(::isPublicInternetAddress)
        } catch (_: Exception) {
            false
        }
    }

    private fun isPublicInternetAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress ||
            address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) return false
        val b = address.address.map { it.toInt() and 0xff }
        return when (address) {
            is Inet4Address -> {
                val a = b[0]
                val second = b[1]
                when {
                    a == 0 || a == 10 || a == 127 || a >= 224 -> false
                    a == 100 && second in 64..127 -> false // CGNAT
                    a == 169 && second == 254 -> false
                    a == 172 && second in 16..31 -> false
                    a == 192 && (second == 168 || (second == 0 && b[2] == 0)) -> false
                    a == 192 && second == 0 && b[2] == 2 -> false
                    a == 198 && (second == 18 || second == 19 || second == 51 && b[2] == 100) -> false
                    a == 203 && second == 0 && b[2] == 113 -> false
                    else -> true
                }
            }
            is Inet6Address -> b[0] and 0xe0 == 0x20 // globally routed 2000::/3 only
            else -> false
        }
    }
}
