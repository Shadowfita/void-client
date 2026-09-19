package com.voidclient.mobile;

/** Minimum raster dimensions constrain both axes together, never independently stretch them. */
public final class UniformScale {
    private UniformScale() { }
    public static double effective(int width, int height, double requested) {
        if (width < 1 || height < 1 || !Double.isFinite(requested) || requested <= 0)
            throw new IllegalArgumentException("Positive dimensions and scale required");
        return Math.min(requested, Math.min(width / 256.0, height / 192.0));
    }
}
