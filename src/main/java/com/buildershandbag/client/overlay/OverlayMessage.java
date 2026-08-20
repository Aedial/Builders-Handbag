package com.buildershandbag.client.overlay;


/**
 * A translated message together with its display and fade timing.
 */
public class OverlayMessage {

    public static final long DISPLAY_DURATION_MS = 3000L;
    public static final long FADE_DURATION_MS = 1000L;
    public static final long TOTAL_DURATION_MS = DISPLAY_DURATION_MS + FADE_DURATION_MS;

    private final String text;
    private final MessageType type;
    private final long createdAt;

    public OverlayMessage(String text, MessageType type) {
        this.text = text;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public MessageType getType() {
        return type;
    }

    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - createdAt;
        if (elapsed < DISPLAY_DURATION_MS) return 1.0F;

        long fadeElapsed = elapsed - DISPLAY_DURATION_MS;
        if (fadeElapsed >= FADE_DURATION_MS) return 0.0F;

        return 1.0F - fadeElapsed / (float) FADE_DURATION_MS;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt >= TOTAL_DURATION_MS;
    }
}
