package com.buildershandbag.client.overlay;


/**
 * Display category and text color for an overlay message.
 */
public enum MessageType {
    SUCCESS(0x55FF55),
    ERROR(0xFF5555),
    WARNING(0xFFFF55);

    private final int color;

    MessageType(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
