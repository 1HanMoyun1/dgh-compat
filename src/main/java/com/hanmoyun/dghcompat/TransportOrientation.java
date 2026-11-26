package com.hanmoyun.dghcompat;

/** Model +Y becomes bed-forward +Z after the renderer's Y flip and the lying X rotation. */
public final class TransportOrientation {
    private TransportOrientation() {}

    public static float modelYaw(float carrierYaw, boolean bedAnchor) {
        return (bedAnchor ? 0 : 180) - carrierYaw;
    }
}
