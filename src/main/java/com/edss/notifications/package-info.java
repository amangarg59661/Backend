/**
 * Notifications domain: user-facing alerts (email + in-app). Listens to events
 * from other modules and produces both delivery attempts and durable
 * notification rows. Owns the {@code notifications} Postgres schema.
 */
@org.springframework.modulith.ApplicationModule(displayName = "notifications")
package com.edss.notifications;
