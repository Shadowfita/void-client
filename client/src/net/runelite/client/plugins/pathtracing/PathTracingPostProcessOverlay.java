package net.runelite.client.plugins.pathtracing;

import com.GameClient;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.geom.Point2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class PathTracingPostProcessOverlay extends Overlay
{
	private final GameClient client;

	@Inject
	PathTracingPostProcessOverlay(GameClient client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!PathTracingState.isCustomLightingActive())
		{
			return null;
		}

		int width = Math.max(1, client.getCanvasWidth());
		int height = Math.max(1, client.getCanvasHeight());
		Composite previousComposite = graphics.getComposite();
		Object previousAa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Object previousTextAa = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);

		if (PathTracingState.isCustomSkyboxActive())
		{
			renderSkybox(graphics, width, height);
		}

		if (PathTracingState.isAntiAliasingActive())
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			renderEdgeSmoothing(graphics, width, height);
		}

		renderFramebufferEffects(graphics, width, height);

		if (PathTracingState.isSsaoActive())
		{
			renderSsao(graphics, width, height);
		}

		if (PathTracingState.isVignetteActive())
		{
			renderVignette(graphics, width, height);
		}

		graphics.setComposite(previousComposite);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAa);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, previousTextAa);
		return new Dimension(width, height);
	}

	private void renderSsao(Graphics2D graphics, int width, int height)
	{
		int intensity = PathTracingState.lightingIntensity();
		int horizonAlpha = Math.min(70, 14 + intensity / 2);
		int floorAlpha = Math.min(55, 8 + intensity / 3);

		graphics.setComposite(AlphaComposite.SrcOver);
		graphics.setPaint(new LinearGradientPaint(
			new Point2D.Float(0, height * 0.46F),
			new Point2D.Float(0, height),
			new float[]{0.0F, 0.45F, 1.0F},
			new Color[]{
				new Color(0, 0, 0, 0),
				new Color(0, 0, 0, horizonAlpha),
				new Color(0, 0, 0, floorAlpha)
			}
		));
		graphics.fillRect(0, 0, width, height);

		graphics.setColor(new Color(0, 0, 0, Math.min(38, 6 + intensity / 4)));
		int step = Math.max(28, width / 24);
		for (int x = -step; x < width + step; x += step)
		{
			graphics.drawLine(x, height, x + step * 3, height / 2);
		}
	}

	private void renderSkybox(Graphics2D graphics, int width, int height)
	{
		Color zenith;
		Color horizon;
		Color haze;
		switch (PathTracingState.skyboxStyle())
		{
			case WARM:
				zenith = new Color(82, 133, 196, 84);
				horizon = new Color(255, 190, 112, 78);
				haze = new Color(255, 235, 190, 42);
				break;
			case OVERCAST:
				zenith = new Color(90, 105, 120, 96);
				horizon = new Color(172, 181, 188, 82);
				haze = new Color(220, 224, 224, 48);
				break;
			case DUSK:
				zenith = new Color(40, 54, 104, 102);
				horizon = new Color(218, 104, 88, 86);
				haze = new Color(255, 167, 112, 48);
				break;
			case CLEAR:
			default:
				zenith = new Color(58, 133, 218, 76);
				horizon = new Color(178, 221, 255, 70);
				haze = new Color(235, 247, 255, 38);
				break;
		}

		int skyHeight = Math.max(1, (int) (height * 0.56));
		graphics.setComposite(AlphaComposite.SrcOver);
		graphics.setPaint(new LinearGradientPaint(
			new Point2D.Float(0, 0),
			new Point2D.Float(0, skyHeight),
			new float[]{0.0F, 0.72F, 1.0F},
			new Color[]{zenith, horizon, haze}
		));
		graphics.fillRect(0, 0, width, skyHeight);
		renderSun(graphics, width, height);
	}

	private void renderSun(Graphics2D graphics, int width, int height)
	{
		float hour = PathTracingState.sunHour();
		float t = Math.max(0.0F, Math.min(1.0F, (hour - 6.0F) / 12.0F));
		float arc = (float) Math.sin(t * Math.PI);
		float x = width * (0.12F + 0.76F * t);
		float y = height * (0.44F - 0.34F * arc);
		int intensity = PathTracingState.sunIntensity();
		float radius = Math.max(45.0F, Math.min(width, height) * (0.10F + intensity / 900.0F));
		int coreAlpha = Math.min(225, 85 + intensity);
		int glowAlpha = Math.min(115, 22 + intensity);

		graphics.setPaint(new RadialGradientPaint(
			new Point2D.Float(x, y),
			radius,
			new float[]{0.0F, 0.22F, 1.0F},
			new Color[]{
				new Color(255, 246, 207, coreAlpha),
				new Color(255, 214, 143, glowAlpha),
				new Color(255, 214, 143, 0)
			}
		));
		graphics.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2.0F), (int) (radius * 2.0F));
	}

	private void renderFramebufferEffects(Graphics2D graphics, int width, int height)
	{
		PathTracingFrameCapture.Frame frame = PathTracingFrameCapture.latestFrame();
		if (frame == null || !frame.isFresh())
		{
			return;
		}

		BufferedImage source = frame.getImage();
		BufferedImage graded = colorGrade(source);
		graphics.setComposite(AlphaComposite.SrcOver.derive(0.18F));
		graphics.drawImage(graded, 0, 0, width, height, null);

		if (PathTracingState.isBloomActive())
		{
			BufferedImage bloom = bloomMask(source);
			graphics.setComposite(AlphaComposite.SrcOver.derive(PathTracingState.bloomIntensity() / 160.0F));
			graphics.drawImage(bloom, 0, 0, width, height, null);
		}

		if (PathTracingState.isSharpenActive())
		{
			BufferedImage sharpened = sharpen(source);
			graphics.setComposite(AlphaComposite.SrcOver.derive(0.10F));
			graphics.drawImage(sharpened, 0, 0, width, height, null);
		}
	}

	private BufferedImage colorGrade(BufferedImage source)
	{
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		float contrast = PathTracingState.contrast() / 100.0F;
		float saturation = PathTracingState.saturation() / 100.0F;
		int temperature = PathTracingState.temperature();
		int warm = Math.max(0, temperature);
		int cool = Math.max(0, -temperature);

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int argb = source.getRGB(x, y);
				int r = argb >> 16 & 0xff;
				int g = argb >> 8 & 0xff;
				int b = argb & 0xff;
				int luma = (r * 54 + g * 183 + b * 19) >> 8;
				r = clamp((int) ((luma + (r - luma) * saturation - 128) * contrast + 128) + warm / 3 - cool / 6);
				g = clamp((int) ((luma + (g - luma) * saturation - 128) * contrast + 128) + warm / 8);
				b = clamp((int) ((luma + (b - luma) * saturation - 128) * contrast + 128) + cool / 3 - warm / 6);
				image.setRGB(x, y, 0xff000000 | r << 16 | g << 8 | b);
			}
		}

		return image;
	}

	private BufferedImage bloomMask(BufferedImage source)
	{
		int width = source.getWidth();
		int height = source.getHeight();
		BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int argb = source.getRGB(x, y);
				int r = argb >> 16 & 0xff;
				int g = argb >> 8 & 0xff;
				int b = argb & 0xff;
				int luma = (r * 54 + g * 183 + b * 19) >> 8;
				int alpha = clamp((luma - 150) * 2);
				mask.setRGB(x, y, alpha << 24 | r << 16 | g << 8 | b);
			}
		}

		float[] kernel = {
			1 / 16F, 2 / 16F, 1 / 16F,
			2 / 16F, 4 / 16F, 2 / 16F,
			1 / 16F, 2 / 16F, 1 / 16F
		};
		ConvolveOp blur = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
		return blur.filter(blur.filter(mask, null), null);
	}

	private BufferedImage sharpen(BufferedImage source)
	{
		float[] kernel = {
			0F, -0.35F, 0F,
			-0.35F, 2.4F, -0.35F,
			0F, -0.35F, 0F
		};
		return new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null).filter(source, null);
	}

	private void renderVignette(Graphics2D graphics, int width, int height)
	{
		int alpha = Math.min(95, 20 + PathTracingState.lightingIntensity());
		float radius = Math.max(width, height) * 0.72F;
		graphics.setComposite(AlphaComposite.SrcOver);
		graphics.setPaint(new RadialGradientPaint(
			new Point2D.Float(width / 2.0F, height / 2.0F),
			radius,
			new float[]{0.0F, 0.68F, 1.0F},
			new Color[]{
				new Color(0, 0, 0, 0),
				new Color(0, 0, 0, 0),
				new Color(0, 0, 0, alpha)
			}
		));
		graphics.fillRect(0, 0, width, height);
	}

	private void renderEdgeSmoothing(Graphics2D graphics, int width, int height)
	{
		int alpha = Math.min(22, 6 + PathTracingState.lightingIntensity() / 8);
		graphics.setComposite(AlphaComposite.SrcOver);
		graphics.setColor(new Color(255, 255, 255, alpha));
		graphics.drawRect(0, 0, width - 1, height - 1);
		graphics.setColor(new Color(0, 0, 0, alpha));
		graphics.drawRect(1, 1, Math.max(0, width - 3), Math.max(0, height - 3));
	}

	private int clamp(int value)
	{
		if (value < 0)
		{
			return 0;
		}
		if (value > 255)
		{
			return 255;
		}
		return value;
	}
}
