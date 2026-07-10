/**
 * Relationship domain: public inquiries + client account provisioning.
 *
 * <p>Public {@code POST /api/v1/inquiries} lands rows in
 * {@code relationship.inquiries}. Staff triage moves them to
 * {@code in_review}, {@code rejected}, or {@code converted}. Conversion goes
 * through {@link com.edss.identity.spi.IdentityUserProvisioning} — no
 * cross-schema write. The notifications module listens for
 * {@code relationship.inquiry_converted} to send the invite email.</p>
 */
@org.springframework.modulith.ApplicationModule(displayName = "relationship")
package com.edss.relationship;
