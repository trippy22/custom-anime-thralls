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
				decodeGeometry(model, resource);
				return model;
			}
		}
	}

	private static void decodeGeometry(BlenderModel model, String resource) throws IOException
	{
		String encoding = model.getGeometryEncoding();
		if (encoding == null)
		{
			return;
		}
		if (!"delta-v1".equals(encoding))
		{
			throw new IOException("Unsupported geometry encoding in " + resource + ": " + encoding);
		}
		try
		{
			decodeRows(model.getVertices(), CustomModelLoader.MAX_VERTICES, resource);
			decodeRows(model.getFaces(), CustomModelLoader.MAX_FACES, resource);
		}
		catch (ArithmeticException e)
		{
			throw new IOException("Geometry overflow in " + resource, e);
		}
		for (int[] face : model.getFaces())
		{
			for (int index : face)
			{
				if (index < 0 || index >= model.getVertices().length)
				{
					throw new IOException("Invalid face index in " + resource);
				}
			}
		}
		model.setGeometryEncoding(null);
	}

	/** Each column stores the difference from the preceding row, starting at zero. */
	private static void decodeRows(int[][] rows, int limit, String resource) throws IOException
	{
		if (rows == null || rows.length == 0 || rows.length > limit)
		{
			throw new IOException("Invalid geometry size in " + resource);
		}
		int[] previous = new int[3];
		for (int[] row : rows)
		{
			if (row == null || row.length != 3)
			{
				throw new IOException("Invalid geometry row in " + resource);
			}
			for (int column = 0; column < 3; column++)
			{
				row[column] = Math.addExact(previous[column], row[column]);
				previous[column] = row[column];
			}
		}
	}
}
