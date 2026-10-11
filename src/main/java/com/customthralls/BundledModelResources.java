package com.customthralls;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;

/** Reads packaged mesh data for both the live plugin and development preview. Worker thread only. */
final class BundledModelResources
{
	private final Gson gson;

	@Inject
	BundledModelResources(Gson gson)
	{
		this.gson = gson;
	}

	BlenderModel load(String resource) throws IOException
	{
		try (InputStream stream = BundledModelResources.class.getResourceAsStream(resource))
		{
			if (stream == null)
			{
				throw new FileNotFoundException("Missing bundled model " + resource);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
			{
				BlenderModel model = gson.fromJson(reader, BlenderModel.class);
				if (model == null)
				{
					throw new IOException("Empty bundled model " + resource);
				}
				return model;
			}
		}
	}
}
