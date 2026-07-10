package com.edss.relationship.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "relationship", name = "inquiries")
public class Inquiry {

    @Id private UUID id;

    private String name;

    private String email;

    private String phone;

    private String service;

    private String message;

    private String status;

    private String source;

    @Column(name = "converted_to_user_id")
    private UUID convertedToUserId;

    @Column(name = "submitted_at", updatable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    protected Inquiry() {}

    public Inquiry(
            UUID id,
            String name,
            String email,
            String phone,
            String service,
            String message,
            String source,
            Instant submittedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.service = service;
        this.message = message;
        this.source = source;
        this.status = InquiryStatus.NEW.wire();
        this.submittedAt = submittedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getService() {
        return service;
    }

    public String getMessage() {
        return message;
    }

    public InquiryStatus getStatus() {
        return InquiryStatus.ofWire(status);
    }

    public String getSource() {
        return source;
    }

    public UUID getConvertedToUserId() {
        return convertedToUserId;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public UUID getReviewedByUserId() {
        return reviewedByUserId;
    }

    public void moveToInReview(UUID reviewerId, Instant at) {
        requireCurrent(InquiryStatus.NEW);
        this.status = InquiryStatus.IN_REVIEW.wire();
        this.reviewedByUserId = reviewerId;
        this.reviewedAt = at;
    }

    public void reject(UUID reviewerId, Instant at) {
        if (getStatus() == InquiryStatus.CONVERTED) {
            throw new IllegalStateException("Converted inquiries cannot be rejected.");
        }
        this.status = InquiryStatus.REJECTED.wire();
        this.reviewedByUserId = reviewerId;
        this.reviewedAt = at;
    }

    public void markConverted(UUID reviewerId, UUID newUserId, Instant at) {
        if (getStatus() == InquiryStatus.CONVERTED) {
            throw new IllegalStateException("Inquiry already converted.");
        }
        this.status = InquiryStatus.CONVERTED.wire();
        this.convertedToUserId = newUserId;
        this.reviewedByUserId = reviewerId;
        this.reviewedAt = at;
    }

    private void requireCurrent(InquiryStatus expected) {
        if (getStatus() != expected) {
            throw new IllegalStateException(
                    "Expected status " + expected.wire() + ", was " + status);
        }
    }
}
