package com.edss.identity.domain;

public enum MfaMethodType {
    TOTP("totp"),
    WHATSAPP_OTP("whatsapp_otp"),
    BACKUP_CODE("backup_code");

    private final String wire;

    MfaMethodType(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static MfaMethodType ofWire(String v) {
        for (MfaMethodType t : values()) {
            if (t.wire.equals(v)) return t;
        }
        throw new IllegalArgumentException("Unknown MFA method: " + v);
    }
}
