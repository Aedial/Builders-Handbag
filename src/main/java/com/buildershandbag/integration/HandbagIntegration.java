package com.buildershandbag.integration;


/**
 * The origin of a configuration option, used to identify the integration that produced it.
 */
public enum HandbagIntegration {
    CHISEL,
    ARCHITECTURECRAFT,
    BLOCKCRAFTERY;

    public static HandbagIntegration fromOrdinal(int ordinal) {
        HandbagIntegration[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
