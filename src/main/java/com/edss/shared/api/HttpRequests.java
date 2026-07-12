package com.edss.shared.api;

import com.edss.shared.config.ProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cross-controller helpers for HTTP request extraction. {@code X-Forwarded-For}
 * is trusted only when the immediate peer is inside a configured
 * {@link ProxyProperties} CIDR — otherwise ignored to prevent header-injected
 * lockout bypass. Without any trusted CIDR the code always returns
 * {@code request.getRemoteAddr()}.
 */
@Component
public class HttpRequests {

    private static final ThreadLocal<HttpRequests> HOLDER = new ThreadLocal<>();

    private final ProxyProperties proxyProperties;
    private final List<CidrRange> trustedRanges;

    public HttpRequests(ProxyProperties proxyProperties) {
        this.proxyProperties = proxyProperties;
        this.trustedRanges =
                proxyProperties.isEmpty()
                        ? List.of()
                        : proxyProperties.trustedCidrs().stream()
                                .map(CidrRange::parse)
                                .filter(java.util.Objects::nonNull)
                                .toList();
        HOLDER.set(this);
    }

    /** Static entry-point for legacy callers that still take only the request. */
    public static String clientIp(HttpServletRequest request) {
        HttpRequests self = HOLDER.get();
        if (self == null) {
            return request.getRemoteAddr();
        }
        return self.resolveClientIp(request);
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (proxyProperties.isEmpty() || !isTrusted(remote)) {
            return remote;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remote;
        }
        int lastComma = forwarded.lastIndexOf(',');
        return (lastComma < 0 ? forwarded : forwarded.substring(lastComma + 1)).trim();
    }

    private boolean isTrusted(String remote) {
        if (remote == null) {
            return false;
        }
        try {
            byte[] address = InetAddress.getByName(remote).getAddress();
            for (CidrRange range : trustedRanges) {
                if (range.contains(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException ignored) {
            // Fall through — treat as untrusted.
        }
        return false;
    }

    private record CidrRange(byte[] network, int prefixLength) {

        static CidrRange parse(String cidr) {
            int slash = cidr.indexOf('/');
            if (slash < 0) {
                return null;
            }
            try {
                InetAddress network = InetAddress.getByName(cidr.substring(0, slash));
                int prefix = Integer.parseInt(cidr.substring(slash + 1));
                return new CidrRange(network.getAddress(), prefix);
            } catch (UnknownHostException | NumberFormatException ex) {
                return null;
            }
        }

        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF & (0xFF << (8 - remainingBits));
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
