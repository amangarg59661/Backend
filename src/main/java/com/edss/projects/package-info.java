/**
 * Projects domain: client-owned engagements. Owns the {@code projects}
 * Postgres schema. Depends only on {@code shared} — reads from
 * {@code identity} go through {@link com.edss.identity.spi.IdentityQuery}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "projects")
package com.edss.projects;
