/**
 * Finance domain: invoices, billing state. Owns the {@code finance} Postgres
 * schema. Depends only on {@code shared} for cross-cutting concerns.
 */
@org.springframework.modulith.ApplicationModule(displayName = "finance")
package com.edss.finance;
