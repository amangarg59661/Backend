/**
 * Cross-cutting infrastructure shared by all domain modules.
 *
 * <p>Not a domain. Holds security, API error handling, event/outbox plumbing,
 * rate limiting, and configuration primitives. Every domain module may depend
 * on this package; nothing here may depend on a domain module.</p>
 */
@org.springframework.modulith.ApplicationModule(displayName = "shared")
package com.edss.shared;
