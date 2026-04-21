package net.runelite.client.plugins.pathtracing;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public final class PathTracingFrameCapture
{
	private static volatile Frame latestFrame;
	private static long nextCaptureNanos;

	private PathTracingFrameCapture()
	{
	}

	public static boolean shouldCapture()
	{
		if (!PathTracingState.shouldCaptureFramebuffer())
		{
			return false;
		}

		long now = System.nanoTime();
		if (now < nextCaptureNanos)
		{
			return false;
		}

		nextCaptureNanos = now + 1_000_000_000L / Math.max(1, PathTracingState.captureFps());
		return true;
	}

	public static int captureWidth(int width)
	{
		return Math.max(1, width * PathTracingState.framebufferScale() / 100);
	}

	public static int captureHeight(int height)
	{
		return Math.max(1, height * PathTracingState.framebufferScale() / 100);
	}

	public static void publish(int[] pixels, int width, int height)
	{
		if (pixels == null || width <= 0 || height <= 0 || pixels.length < width * height)
		{
			return;
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] dest = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < width * height; i++)
		{
			dest[i] = 0xff000000 | pixels[i] & 0x00ffffff;
		}
		latestFrame = new Frame(image, System.nanoTime());
	}

	public static Frame latestFrame()
	{
		return latestFrame;
	}

	public static final class Frame
	{
		private final BufferedImage image;
		private final long captureNanos;

		private Frame(BufferedImage image, long captureNanos)
		{
			this.image = image;
			this.captureNanos = captureNanos;
		}

		public BufferedImage getImage()
		{
			return image;
		}

		public boolean isFresh()
		{
			return System.nanoTime() - captureNanos < 250_000_000L;
		}
	}
}
