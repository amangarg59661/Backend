package com.edss.shared.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Trusted-proxy allow-list for {@code X-Forwarded-For} interpretation. When
 * empty (default), XFF is ignored and {@code request.getRemoteAddr()} is used
 * — safest posture. When populated with CIDR blocks, XFF is trusted only
 * when the immediate peer is inside one of them.
 */
@ConfigurationProperties(prefix = "edss.security.proxy")
public record ProxyProperties(List<String> trustedCidrs) {

    public boolean isEmpty() {
        return trustedCidrs == null || trustedCidrs.isEmpty();
    }
}
