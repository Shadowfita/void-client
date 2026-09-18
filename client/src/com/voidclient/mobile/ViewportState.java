package com.voidclient.mobile;

/** Immutable snapshot. Host and pointer coordinates are CSS/AWT display units, never device pixels. */
public final class ViewportState {
    public final long revision;
    public final int x, y, width, height, nativeWidth, nativeHeight, logicalWidth, logicalHeight;
    public final String profile;

    public ViewportState(long revision, int x, int y, int width, int height,
                         int nativeWidth, int nativeHeight, int logicalWidth, int logicalHeight) {
        if (width < 1 || height < 1 || nativeWidth < 1 || nativeHeight < 1 || logicalWidth < 1 || logicalHeight < 1)
            throw new IllegalArgumentException("Viewport dimensions must be positive");
        this.revision = revision; this.x = x; this.y = y;
        this.width = width; this.height = height;
        this.nativeWidth = nativeWidth; this.nativeHeight = nativeHeight;
        this.logicalWidth = logicalWidth; this.logicalHeight = logicalHeight;
        profile = Math.min(width, height) >= 600 ? "tablet" : width >= height ? "compact-landscape" : "compact-portrait";
    }

    public boolean contains(double hostX, double hostY) {
        return Double.isFinite(hostX) && Double.isFinite(hostY)
            && hostX >= x && hostY >= y && hostX < (double) x + width && hostY < (double) y + height;
    }
    public int nativeX(double hostX) { return map(hostX, x, width, nativeWidth); }
    public int nativeY(double hostY) { return map(hostY, y, height, nativeHeight); }
    public int logicalX(double hostX) { return map(hostX, x, width, logicalWidth); }
    public int logicalY(double hostY) { return map(hostY, y, height, logicalHeight); }
    public double hostX(double logicalX) { return x + logicalX * width / logicalWidth; }
    public double hostY(double logicalY) { return y + logicalY * height / logicalHeight; }
    private static int map(double value, int origin, int displaySize, int targetSize) {
        // Reject outside the content before conversion: do not turn letterbox taps into edge clicks.
        if (!Double.isFinite(value) || value < origin || value >= (double) origin + displaySize)
            throw new IllegalArgumentException("Pointer outside viewport");
        return Math.min(targetSize - 1, (int) Math.floor((value - origin) * targetSize / displaySize));
    }
    public boolean sameGeometry(ViewportState other) {
        return other != null && x == other.x && y == other.y && width == other.width && height == other.height
            && nativeWidth == other.nativeWidth && nativeHeight == other.nativeHeight
            && logicalWidth == other.logicalWidth && logicalHeight == other.logicalHeight;
    }
}
