/**
 * Careers module — moderated job postings + public application intake.
 *
 * <p>Aggregates:
 * <ul>
 *   <li>{@link com.edss.careers.domain.JobPosting} — draft / published / archived job listings.</li>
 *   <li>{@link com.edss.careers.domain.JobApplication} — inbound applications; staff triages.</li>
 * </ul>
 *
 * <p>Public HTTP surface — no authentication required:
 * <ul>
 *   <li>{@code GET /api/v1/careers} — list published postings.</li>
 *   <li>{@code GET /api/v1/careers/{slug}} — single published posting.</li>
 *   <li>{@code POST /api/v1/careers/{slug}/apply} — submit an application (rate-limited).</li>
 * </ul>
 *
 * <p>Staff HTTP surface — {@code careers:*} permission required:
 * <ul>
 *   <li>{@code GET /api/v1/staff/careers} — list all statuses.</li>
 *   <li>{@code POST /api/v1/staff/careers} — draft a new posting.</li>
 *   <li>{@code PATCH /api/v1/staff/careers/{id}} — edit fields.</li>
 *   <li>{@code POST /api/v1/staff/careers/{id}/publish} — publish a draft.</li>
 *   <li>{@code POST /api/v1/staff/careers/{id}/archive} — archive.</li>
 *   <li>{@code GET /api/v1/staff/careers/{id}/applications} — list applications for a posting.</li>
 *   <li>{@code PATCH /api/v1/staff/careers/applications/{applicationId}} — status change + note.</li>
 * </ul>
 *
 * <p>Events emitted through the outbox:
 * <ul>
 *   <li>{@code careers.posting_published}</li>
 *   <li>{@code careers.application_submitted}</li>
 *   <li>{@code careers.application_reviewed}</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Careers")
package com.edss.careers;
