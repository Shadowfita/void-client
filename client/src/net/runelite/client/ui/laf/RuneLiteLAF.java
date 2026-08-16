/*
 * Copyright (c) 2022 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.ui.laf;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatSystemProperties;
import java.awt.Color;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.UIDefaults;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Modern RuneLite-style FlatLaf shell. Automatic FlatLaf scaling is disabled so
 * the 634 client's native interface scale remains the sole owner of game/HUD
 * coordinates and framebuffer sizing.
 */
public class RuneLiteLAF extends FlatDarkLaf
{
	public static boolean setup()
	{
		System.setProperty(FlatSystemProperties.UI_SCALE_ENABLED, "false");
		return FlatLaf.setup(new RuneLiteLAF());
	}

	public RuneLiteLAF()
	{
		Map<String, String> extras = new HashMap<String, String>();
		Properties properties = new Properties();
		Class<?>[] sources = {FlatLaf.class, FlatDarkLaf.class, RuneLiteLAF.class};
		for (Class<?> source : sources)
		{
			String resource = "/" + source.getName().replace('.', '/') + ".properties";
			try (InputStream input = source.getResourceAsStream(resource))
			{
				if (input != null)
				{
					properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
				}
			}
			catch (IOException ignored)
			{
				// FlatLaf's built-in defaults remain a safe fallback.
			}
		}

		for (Map.Entry<Object, Object> entry : properties.entrySet())
		{
			String key = (String) entry.getKey();
			if (key.charAt(0) == '[' && !key.startsWith("[style]"))
			{
				continue;
			}
			extras.put(key, (String) entry.getValue());
		}

		for (Field field : ColorScheme.class.getDeclaredFields())
		{
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == Color.class)
			{
				try
				{
					String name = field.getName();
					if (name.endsWith("_COLOR"))
					{
						name = name.substring(0, name.length() - 6);
					}
					Color color = (Color) field.get(null);
					extras.put("@" + name, String.format("#%06x%02x", color.getRGB() & 0xffffff, color.getRGB() >>> 24));
				}
				catch (IllegalAccessException ignored)
				{
				}
			}
		}
		setExtraDefaults(extras);
	}

	@Override
	protected List<Class<?>> getLafClassesForDefaultsLoading()
	{
		return Collections.emptyList();
	}

	@Override
	public String getName()
	{
		return "RuneLite";
	}

	@Override
	public String getDescription()
	{
		return "RuneLite FlatLaf";
	}

	@Override
	public UIDefaults getDefaults()
	{
		UIDefaults defaults = super.getDefaults();
		defaults.put("defaultFont", FontManager.getRunescapeFont());
		defaults.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		return defaults;
	}
}
