/**
 * Integrations domain: third-party clients — payment gateways, storage
 * providers, calendar systems, messaging APIs. All impls are flag-gated so
 * a fresh dev env boots without any external credentials.
 */
@org.springframework.modulith.ApplicationModule(displayName = "integrations")
package com.edss.integrations;
