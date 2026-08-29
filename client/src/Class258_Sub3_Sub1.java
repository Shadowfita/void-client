/* Class258_Sub3_Sub1 - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */

final class Class258_Sub3_Sub1 extends Class258_Sub3 {
    float aFloat9937;
    float aFloat9938;
    int anInt9939;
    int anInt9940;
    boolean aBoolean9941;
    static int anInt9942;
    private Class258_Sub3 interfaceSupersampleTexture;
    private int interfaceSupersampleFactor = 1;
    private static final int MAX_RETAINED_SOURCE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_PROXY_BYTES = 32 * 1024 * 1024;

    private int[] interfaceIntPixels;
    private byte[] interfaceBytePixels;
    // Deliberately has no field initializer: the superclass constructor may
    // capture byte-backed source pixels before subclass initializers run.
    private int interfaceByteFormat;
    private boolean interfaceSourceComplete;

    final void recordInterfaceIntPixels(int x, int y, int width, int height, int[] pixels, int offset, int stride) {
        if (!validRegion(x, y, width, height) || pixels == null) {
            clearInterfaceSource();
            return;
        }
        if (stride <= 0) {
            stride = width;
        }
        long last = (long) offset + (long) (height - 1) * stride + width;
        if (offset < 0 || stride < width || last > pixels.length) {
            clearInterfaceSource();
            return;
        }

        int texturePixels = retainedTexturePixelCount(4);
        if (texturePixels <= 0) {
            clearInterfaceSource();
            return;
        }

        boolean sameBacking = interfaceIntPixels != null
                && interfaceIntPixels.length == texturePixels
                && interfaceBytePixels == null;
        if (!sameBacking) {
            try {
                interfaceIntPixels = new int[texturePixels];
            } catch (OutOfMemoryError error) {
                clearInterfaceSource();
                return;
            }
            interfaceSourceComplete = false;
        }
        interfaceBytePixels = null;
        interfaceByteFormat = -1;

        for (int row = 0; row < height; row++) {
            System.arraycopy(pixels, offset + row * stride,
                    interfaceIntPixels, (y + row) * this.anInt8547 + x, width);
        }
        if (isFullTextureUpdate(x, y, width, height)) {
            interfaceSourceComplete = true;
        }
    }

    final void recordInterfaceBytePixels(int x, int y, int width, int height, byte[] pixels,
                                         int offset, int stridePixels, int format) {
        if (!validRegion(x, y, width, height) || pixels == null) {
            clearInterfaceSource();
            return;
        }
        final int channels;
        try {
            channels = Class183.method1382(format, -6409);
        } catch (IllegalArgumentException ex) {
            clearInterfaceSource();
            return;
        }
        if (stridePixels <= 0) {
            stridePixels = width;
        }
        int strideBytes = stridePixels * channels;
        int rowBytes = width * channels;
        long last = (long) offset + (long) (height - 1) * strideBytes + rowBytes;
        if (offset < 0 || stridePixels < width || last > pixels.length) {
            clearInterfaceSource();
            return;
        }

        int texturePixels = retainedTexturePixelCount(channels);
        if (texturePixels <= 0) {
            clearInterfaceSource();
            return;
        }
        int textureBytes = texturePixels * channels;
        boolean sameBacking = interfaceBytePixels != null
                && interfaceBytePixels.length == textureBytes
                && interfaceByteFormat == format
                && interfaceIntPixels == null;
        if (!sameBacking) {
            try {
                interfaceBytePixels = new byte[textureBytes];
            } catch (OutOfMemoryError error) {
                clearInterfaceSource();
                return;
            }
            interfaceSourceComplete = false;
        }
        interfaceIntPixels = null;
        interfaceByteFormat = format;

        for (int row = 0; row < height; row++) {
            System.arraycopy(pixels, offset + row * strideBytes,
                    interfaceBytePixels, ((y + row) * this.anInt8547 + x) * channels, rowBytes);
        }
        if (isFullTextureUpdate(x, y, width, height)) {
            interfaceSourceComplete = true;
        }
    }

    final Class258_Sub3 createInterfaceSupersampleTexture(int factor) {
        if (factor <= 1 || this.anInt4849 != 3553 || !interfaceSourceComplete) {
            return null;
        }
        int width = this.anInt8547;
        int height = this.anInt8551;
        if (width <= 0 || height <= 0 || width > 4096 / factor || height > 4096 / factor) {
            return null;
        }
        int targetWidth = width * factor;
        int targetHeight = height * factor;

        try {
            if (interfaceIntPixels != null && interfaceIntPixels.length == width * height) {
                long targetCount = (long) targetWidth * targetHeight;
                long targetBytes = targetCount * 4L;
                if (targetCount <= 0L || targetCount > Integer.MAX_VALUE || targetBytes > MAX_PROXY_BYTES) {
                    return null;
                }
                int[] target = new int[(int) targetCount];
                expandIntegerNearest(interfaceIntPixels, width, height, target, targetWidth, factor);
                Class258_Sub3 result = new Class258_Sub3(this.aHa_Sub2_4851, 3553, this.anInt4858,
                        targetWidth, targetHeight, false, target, 0, 0, false);
                // Stage one is exact integer nearest-neighbour. Linear filtering
                // performs only the unavoidable final fractional reduction and
                // materially improves thin 634 font strokes at 1.3x-1.5x.
                result.method1957(9728, true);
                result.method1965(false, false, 10243);
                return result;
            }

            if (interfaceBytePixels != null && interfaceByteFormat != -1) {
                final int channels;
                try {
                    channels = Class183.method1382(interfaceByteFormat, -6409);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
                if (interfaceBytePixels.length != width * height * channels) {
                    return null;
                }
                long targetBytes = (long) targetWidth * targetHeight * channels;
                if (targetBytes <= 0L || targetBytes > MAX_PROXY_BYTES || targetBytes > Integer.MAX_VALUE) {
                    return null;
                }
                byte[] target = new byte[(int) targetBytes];
                expandIntegerNearest(interfaceBytePixels, width, height, channels, target, targetWidth, factor);
                Class258_Sub3 result = new Class258_Sub3(this.aHa_Sub2_4851, 3553, this.anInt4858,
                        targetWidth, targetHeight, false, target, interfaceByteFormat, false);
                result.method1957(9728, true);
                result.method1965(false, false, 10243);
                return result;
            }
        } catch (OutOfMemoryError error) {
            // Supersampling is an optional quality path. Falling back to the
            // original texture must never take down login or the render loop.
            return null;
        }

        // Render-target, framebuffer-copy, oversized and partially populated
        // textures have no safe complete CPU source. Bind the original texture.
        return null;
    }

    private void clearInterfaceSource() {
        interfaceIntPixels = null;
        interfaceBytePixels = null;
        interfaceByteFormat = -1;
        interfaceSourceComplete = false;
        releaseInterfaceSupersampleTexture();
    }

    private boolean validRegion(int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && width > 0 && height > 0
                && (long) x + width <= this.anInt8547
                && (long) y + height <= this.anInt8551;
    }

    private boolean isFullTextureUpdate(int x, int y, int width, int height) {
        return x == 0 && y == 0 && width == this.anInt8547 && height == this.anInt8551;
    }

    private int retainedTexturePixelCount(int bytesPerPixel) {
        long pixels = (long) this.anInt8547 * this.anInt8551;
        long bytes = pixels * bytesPerPixel;
        return pixels > 0L && pixels <= Integer.MAX_VALUE && bytes <= MAX_RETAINED_SOURCE_BYTES
                ? (int) pixels : -1;
    }

    private static void expandIntegerNearest(int[] source, int width, int height,
                                             int[] target, int targetWidth, int factor) {
        int[] expandedRow = new int[targetWidth];
        for (int y = 0; y < height; y++) {
            int sourceRow = y * width;
            int out = 0;
            for (int x = 0; x < width; x++) {
                int pixel = source[sourceRow + x];
                for (int copy = 0; copy < factor; copy++) {
                    expandedRow[out++] = pixel;
                }
            }
            int targetRow = y * factor * targetWidth;
            for (int copyY = 0; copyY < factor; copyY++) {
                System.arraycopy(expandedRow, 0, target, targetRow + copyY * targetWidth, targetWidth);
            }
        }
    }

    private static void expandIntegerNearest(byte[] source, int width, int height, int channels,
                                             byte[] target, int targetWidth, int factor) {
        int sourceStride = width * channels;
        int targetStride = targetWidth * channels;
        byte[] expandedRow = new byte[targetStride];
        for (int y = 0; y < height; y++) {
            int sourceRow = y * sourceStride;
            int out = 0;
            for (int x = 0; x < width; x++) {
                int pixel = sourceRow + x * channels;
                for (int copy = 0; copy < factor; copy++) {
                    System.arraycopy(source, pixel, expandedRow, out, channels);
                    out += channels;
                }
            }
            int targetRow = y * factor * targetStride;
            for (int copyY = 0; copyY < factor; copyY++) {
                System.arraycopy(expandedRow, 0, target, targetRow + copyY * targetStride, targetStride);
            }
        }
    }

    final Class258 getInterfaceSupersampleTexture(int factor) {
        if (factor <= 1 || this.anInt4849 != 3553) {
            if (factor <= 1) {
                releaseInterfaceSupersampleTexture();
            }
            return this;
        }
        if (interfaceSupersampleTexture != null && interfaceSupersampleFactor == factor) {
            return interfaceSupersampleTexture;
        }
        releaseInterfaceSupersampleTexture();
        Class258_Sub3 texture = this.aHa_Sub2_4851.createInterfaceSupersampleTexture(this, factor);
        if (texture != null) {
            interfaceSupersampleTexture = texture;
            interfaceSupersampleFactor = factor;
            return texture;
        }
        return this;
    }

    @Override
    void invalidateInterfaceSupersampleTexture() {
        releaseInterfaceSupersampleTexture();
    }

    @Override
    void releaseInterfaceSupersampleTexture() {
        if (interfaceSupersampleTexture != null) {
            interfaceSupersampleTexture.method1952(-19948);
            interfaceSupersampleTexture = null;
        }
        interfaceSupersampleFactor = 1;
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_0_, int i_1_, int i_2_) {
        super(var_ha_Sub2, i, i_0_, i_1_, i_2_);
        if (this.anInt4849 == 34037) {
            this.aFloat9938 = (float) i_2_;
            this.aBoolean9941 = false;
            this.aFloat9937 = (float) i_1_;
        } else {
            this.aBoolean9941 = true;
            this.aFloat9937 = this.aFloat9938 = 1.0F;
        }
        this.anInt9940 = i_1_;
        this.anInt9939 = i_2_;
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_3_, int i_4_, int i_5_, int i_6_) {
        super(var_ha_Sub2, 3553, i, i_5_, i_6_);
        this.aFloat9937 = (float) i_3_ / (float) i_5_;
        this.anInt9939 = i_4_;
        this.anInt9940 = i_3_;
        this.aFloat9938 = (float) i_4_ / (float) i_6_;
        this.aBoolean9941 = false;
        this.method1965(false, false, 10243);
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_7_, int i_8_, int i_9_, int i_10_, boolean bool) {
        super(var_ha_Sub2, i, i_7_, i_8_, i_9_, i_10_);
        this.anInt9940 = i_9_;
        if (this.anInt4849 == 34037) {
            this.aBoolean9941 = false;
            this.aFloat9937 = (float) i_9_;
            this.aFloat9938 = (float) i_10_;
        } else {
            this.aBoolean9941 = true;
            this.aFloat9937 = this.aFloat9938 = 1.0F;
        }
        this.anInt9939 = i_10_;
    }

    static final void method1971(int i, String string, boolean bool, int i_11_) {
        anInt9942++;
        Class59_Sub1_Sub1.method556(false);
        Class341.method2681(9864);
        Class348_Sub22.method2959(-1);
        Class348_Sub23.method2965(string, i_11_, bool, 0);
        Class348_Sub24.method2994(2);
        Class170.method1311(5139, Class348_Sub8.aHa6654);
        Class369.method3568(Class348_Sub8.aHa6654, 4);
        ObjTypeList.method1933(Class21.aClass45_322, Class348_Sub8.aHa6654, true);
        Class274.method2061(-128);
        Class101.method901(Class113.aClass105Array1744, 515880227);
        Class354.method3466(i ^ ~0x4f);
        Class348_Sub40.method3038(-1);
        if (Class240.anInt4674 == 3) Packet.method3379(2, 4);
        else if (Class240.anInt4674 == 7) Packet.method3379(2, 8);
        else if (Class240.anInt4674 != 10) {
            if (Class240.anInt4674 == 1 || Class240.anInt4674 == 2) Class376.method3616(12639);
        } else Packet.method3379(2, 11);
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_12_, int i_13_, int i_14_, int i_15_, int i_16_, boolean bool) {
        super(var_ha_Sub2, 3553, i, i_12_, i_15_, i_16_);
        this.anInt9940 = i_13_;
        this.aFloat9938 = (float) i_14_ / (float) i_16_;
        this.aFloat9937 = (float) i_13_ / (float) i_15_;
        this.anInt9939 = i_14_;
        this.aBoolean9941 = false;
        this.method1965(false, false, 10243);
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_17_, int i_18_, int i_19_, int i_20_, byte[] is, int i_21_) {
        super(var_ha_Sub2, 3553, i, i_19_, i_20_);
        try {
            this.anInt9940 = i_17_;
            this.anInt9939 = i_18_;
            this.method1970(0, 0, i_17_, true, 0, i_18_, i_21_, 127, is, 0);
            this.aFloat9938 = (float) i_18_ / (float) i_20_;
            this.aFloat9937 = (float) i_17_ / (float) i_19_;
            this.aBoolean9941 = false;
            this.method1965(false, false, 10243);
        } catch (RuntimeException runtimeexception) {
            throw Class348_Sub17.method2929(runtimeexception, ("eba.<init>(" + (var_ha_Sub2 != null ? "{...}" : "null") + ',' + i + ',' + i_17_ + ',' + i_18_ + ',' + i_19_ + ',' + i_20_ + ',' + (is != null ? "{...}" : "null") + ',' + i_21_ + ')'));
        }
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_22_, int i_23_, boolean bool, int[] is, int i_24_, int i_25_) {
        super(var_ha_Sub2, i, 6408, i_22_, i_23_, bool, is, i_24_, i_25_, true);
        try {
            if (this.anInt4849 == 34037) {
                this.aFloat9937 = (float) i_22_;
                this.aBoolean9941 = false;
                this.aFloat9938 = (float) i_23_;
            } else {
                this.aBoolean9941 = true;
                this.aFloat9937 = this.aFloat9938 = 1.0F;
            }
            this.anInt9939 = i_23_;
            this.anInt9940 = i_22_;
        } catch (RuntimeException runtimeexception) {
            throw Class348_Sub17.method2929(runtimeexception, ("eba.<init>(" + (var_ha_Sub2 != null ? "{...}" : "null") + ',' + i + ',' + i_22_ + ',' + i_23_ + ',' + bool + ',' + (is != null ? "{...}" : "null") + ',' + i_24_ + ',' + i_25_ + ')'));
        }
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_26_, int i_27_, int i_28_, int[] is) {
        super(var_ha_Sub2, 3553, 6408, i_27_, i_28_);
        try {
            this.anInt9940 = i;
            this.anInt9939 = i_26_;
            this.method1964(true, i_26_, 0, is, 0, (byte) -73, i, 0, 0);
            this.aFloat9937 = (float) i / (float) i_27_;
            this.aFloat9938 = (float) i_26_ / (float) i_28_;
            this.aBoolean9941 = false;
            this.method1965(false, false, 10243);
        } catch (RuntimeException runtimeexception) {
            throw Class348_Sub17.method2929(runtimeexception, ("eba.<init>(" + (var_ha_Sub2 != null ? "{...}" : "null") + ',' + i + ',' + i_26_ + ',' + i_27_ + ',' + i_28_ + ',' + (is != null ? "{...}" : "null") + ')'));
        }
    }

    Class258_Sub3_Sub1(ha_Sub2 var_ha_Sub2, int i, int i_29_, int i_30_, int i_31_, boolean bool, byte[] is, int i_32_) {
        super(var_ha_Sub2, i, i_29_, i_30_, i_31_, bool, is, i_32_, true);
        try {
            this.anInt9940 = i_30_;
            if (this.anInt4849 == 34037) {
                this.aFloat9938 = (float) i_31_;
                this.aBoolean9941 = false;
                this.aFloat9937 = (float) i_30_;
            } else {
                this.aBoolean9941 = true;
                this.aFloat9937 = this.aFloat9938 = 1.0F;
            }
            this.anInt9939 = i_31_;
        } catch (RuntimeException runtimeexception) {
            throw Class348_Sub17.method2929(runtimeexception, ("eba.<init>(" + (var_ha_Sub2 != null ? "{...}" : "null") + ',' + i + ',' + i_29_ + ',' + i_30_ + ',' + i_31_ + ',' + bool + ',' + (is != null ? "{...}" : "null") + ',' + i_32_ + ')'));
        }
    }
}
