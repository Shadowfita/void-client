package net.runelite.client.util;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

public final class BuildIdentity
{
	private BuildIdentity()
	{
	}

	public static String describe(Class<?> anchor)
	{
		try
		{
			URL location = anchor.getProtectionDomain().getCodeSource().getLocation();
			Path path = Paths.get(location.toURI()).toAbsolutePath().normalize();
			if (!Files.isRegularFile(path))
			{
				return "classes=" + path;
			}
			return "jar=" + path + ", size=" + Files.size(path) + ", sha256=" + sha256(path);
		}
		catch (Throwable ex)
		{
			return "unavailable (" + ex.getClass().getSimpleName() + ")";
		}
	}

	private static String sha256(Path path) throws Exception
	{
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] buffer = new byte[64 * 1024];
		try (InputStream input = Files.newInputStream(path))
		{
			int read;
			while ((read = input.read(buffer)) != -1)
			{
				digest.update(buffer, 0, read);
			}
		}
		StringBuilder value = new StringBuilder(64);
		for (byte b : digest.digest())
		{
			value.append(String.format("%02x", b & 0xff));
		}
		return value.toString();
	}
}
