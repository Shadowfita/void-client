package net.runelite.client.plugins.pathtracing;

public final class PathTracingState
{
	private static volatile boolean enabled;
	private static volatile boolean pathTracedShadows;
	private static volatile boolean customLighting;
	private static volatile boolean ssao;
	private static volatile boolean antiAliasing;
	private static volatile boolean framebufferEffects;
	private static volatile boolean bloom;
	private static volatile boolean vignette;
	private static volatile boolean sharpen;
	private static volatile boolean customSkybox;
	private static volatile PathTracingSkyboxStyle skyboxStyle = PathTracingSkyboxStyle.CLEAR;
	private static volatile int samples = 8;
	private static volatile int softness = 55;
	private static volatile int lightingIntensity = 45;
	private static volatile int sunHour = 14;
	private static volatile int sunIntensity = 65;
	private static volatile int framebufferScale = 50;
	private static volatile int captureFps = 20;
	private static volatile int bloomIntensity = 28;
	private static volatile int contrast = 108;
	private static volatile int saturation = 112;
	private static volatile int temperature = 8;

	private PathTracingState()
	{
	}

	public static void setEnabled(boolean enabled)
	{
		PathTracingState.enabled = enabled;
		PathTracingState.pathTracedShadows = false;
		PathTracingState.customLighting = false;
		PathTracingState.ssao = false;
		PathTracingState.antiAliasing = false;
		PathTracingState.framebufferEffects = false;
		PathTracingState.bloom = false;
		PathTracingState.vignette = false;
		PathTracingState.sharpen = false;
		PathTracingState.customSkybox = false;
	}

	public static void configure(boolean pathTracedShadows, boolean customLighting, boolean ssao, boolean antiAliasing, boolean framebufferEffects, boolean bloom, boolean vignette, boolean sharpen, boolean customSkybox, PathTracingSkyboxStyle skyboxStyle, int samples, int softness, int lightingIntensity, int sunHour, int sunIntensity, int framebufferScale, int captureFps, int bloomIntensity, int contrast, int saturation, int temperature)
	{
		PathTracingState.enabled = true;
		PathTracingState.pathTracedShadows = pathTracedShadows;
		PathTracingState.customLighting = customLighting;
		PathTracingState.ssao = ssao;
		PathTracingState.antiAliasing = antiAliasing;
		PathTracingState.framebufferEffects = framebufferEffects;
		PathTracingState.bloom = bloom;
		PathTracingState.vignette = vignette;
		PathTracingState.sharpen = sharpen;
		PathTracingState.customSkybox = customSkybox;
		PathTracingState.skyboxStyle = skyboxStyle == null ? PathTracingSkyboxStyle.CLEAR : skyboxStyle;
		PathTracingState.samples = clamp(samples, 4, 16);
		PathTracingState.softness = clamp(softness, 0, 100);
		PathTracingState.lightingIntensity = clamp(lightingIntensity, 0, 100);
		PathTracingState.sunHour = clamp(sunHour, 0, 23);
		PathTracingState.sunIntensity = clamp(sunIntensity, 0, 100);
		PathTracingState.framebufferScale = clamp(framebufferScale, 25, 100);
		PathTracingState.captureFps = clamp(captureFps, 5, 60);
		PathTracingState.bloomIntensity = clamp(bloomIntensity, 0, 100);
		PathTracingState.contrast = clamp(contrast, 50, 150);
		PathTracingState.saturation = clamp(saturation, 50, 150);
		PathTracingState.temperature = clamp(temperature, -100, 100);
	}

	public static void observeClientShadowMode(int shadowMode)
	{
		/* Retained for renderer call sites that still report the native shadow mode. */
	}

	public static boolean isEnabled()
	{
		return enabled && pathTracedShadows;
	}

	public static boolean isCustomLightingActive()
	{
		return enabled && customLighting;
	}

	public static boolean isCustomLightingActive(int shadowMode)
	{
		observeClientShadowMode(shadowMode);
		return enabled && customLighting;
	}

	public static boolean isSsaoActive()
	{
		return isCustomLightingActive() && ssao;
	}

	public static boolean isAntiAliasingActive()
	{
		return isCustomLightingActive() && antiAliasing;
	}

	public static boolean shouldCaptureFramebuffer()
	{
		return isCustomLightingActive() && framebufferEffects;
	}

	public static boolean isBloomActive()
	{
		return shouldCaptureFramebuffer() && bloom && bloomIntensity > 0;
	}

	public static boolean isVignetteActive()
	{
		return isCustomLightingActive() && vignette;
	}

	public static boolean isSharpenActive()
	{
		return shouldCaptureFramebuffer() && sharpen;
	}

	public static boolean isCustomSkyboxActive()
	{
		return isCustomLightingActive() && customSkybox;
	}

	public static int lightingIntensity()
	{
		return lightingIntensity;
	}

	public static int sunHour()
	{
		return sunHour;
	}

	public static int sunIntensity()
	{
		return sunIntensity;
	}

	public static PathTracingSkyboxStyle skyboxStyle()
	{
		return skyboxStyle;
	}

	public static int framebufferScale()
	{
		return framebufferScale;
	}

	public static int captureFps()
	{
		return captureFps;
	}

	public static int bloomIntensity()
	{
		return bloomIntensity;
	}

	public static int contrast()
	{
		return contrast;
	}

	public static int saturation()
	{
		return saturation;
	}

	public static int temperature()
	{
		return temperature;
	}

	public static long cacheSalt()
	{
		if (!enabled)
		{
			return 0L;
		}

		long salt = 0x5f3759df00000000L ^ ((long) samples << 20) ^ ((long) softness << 8);
		if (isCustomLightingActive())
		{
			salt ^= 0x0c57116000000000L ^ ((long) lightingIntensity << 36);
		}
		return salt;
	}

	public static int ringCount()
	{
		return clamp(samples, 4, 16);
	}

	public static int[] sampleRadii()
	{
		int ringCount = ringCount();
		int[] radii = new int[ringCount];
		boolean lighting = isCustomLightingActive();
		double penumbra = softness / 100.0;
		int innerRadius = lighting ? 38 - (int) (penumbra * 10.0) : 48 - (int) (penumbra * 14.0);
		int outerRadius = lighting ? 148 + (int) (penumbra * 70.0) : 128 + (int) (penumbra * 48.0);

		for (int ring = 0; ring < ringCount; ring++)
		{
			double t = (ring + 1.0) / ringCount;
			double tracedDistribution = lighting ? Math.pow(t, 0.42) : Math.sqrt(t);
			double jitter = ((ring * 1103515245 + 12345) & 0x7) / 7.0 - 0.5;
			radii[ring] = (int) (innerRadius + (outerRadius - innerRadius) * tracedDistribution + jitter * penumbra * 3.0);
		}

		return radii;
	}

	public static byte tracedAlpha(int outerAlpha, int innerAlpha, int ring, int ringCount)
	{
		double t = (ring + 0.5) / ringCount;
		boolean lighting = isCustomLightingActive();
		double falloff = Math.pow(1.0 - t, lighting ? 1.8 + softness / 250.0 : 1.35 + softness / 200.0);
		int alpha = lighting ? (int) (32 + lightingIntensity * 1.8 * falloff) : (int) (outerAlpha + (innerAlpha - outerAlpha) * falloff);
		alpha = (int) (alpha * (lighting ? 0.75 + samples / 128.0 : 0.86 + samples / 96.0));
		return (byte) clamp(alpha, 0, 255);
	}

	private static int clamp(int value, int min, int max)
	{
		if (value < min)
		{
			return min;
		}
		if (value > max)
		{
			return max;
		}
		return value;
	}
}
