package com.customthralls;

import com.google.common.hash.Hashing;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.inject.Inject;
import net.runelite.client.util.Filepath;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Worker-thread-only release download and disk cache. Every filesystem operation uses Filepath. */
final class ModelRepository
{
	private final ModelJsonDecoder decoder;
	private final OkHttpClient http;
	private Filepath preparedDirectory;
	private long retryAfter;

	@Inject
	ModelRepository(ModelJsonDecoder decoder, OkHttpClient http)
	{
		this.decoder = decoder;
		this.http = http.newBuilder().callTimeout(60, TimeUnit.SECONDS).build();
	}

	/** Cache the complete release on first use, but decode only the requested model. */
	synchronized BlenderModel load(Filepath pluginDirectory, String fileName) throws ModelResourceException
	{
		if (!ModelRelease.HASHES.containsKey(fileName))
		{
			throw new ModelResourceException("Unknown model: " + fileName);
		}
		try
		{
			checkInterrupted();
			Filepath directory = pluginDirectory.joinSegment("models").joinSegment(ModelRelease.VERSION);
			prepare(directory);
			byte[] json = readVerified(directory, fileName);
			if (json == null)
			{
				// Repair a missing or modified file, including damage after the initial cache scan.
				preparedDirectory = null;
				prepare(directory);
				json = readVerified(directory, fileName);
			}
			if (json == null)
			{
				throw new IOException("Model is missing after installation: " + fileName);
			}
			return decoder.decode(new String(json, StandardCharsets.UTF_8), fileName);
		}
		catch (IOException e)
		{
			throw new ModelResourceException("Could not load " + fileName + ": " + e.getMessage(), e);
		}
	}

	private void prepare(Filepath directory) throws IOException
	{
		if (directory.equals(preparedDirectory))
		{
			return;
		}
		checkInterrupted();
		directory.createDirectories();
		boolean complete = true;
		for (String name : ModelRelease.HASHES.keySet())
		{
			checkInterrupted();
			if (readVerified(directory, name) == null)
			{
				complete = false;
				break;
			}
		}
		if (!complete)
		{
			if (retryAfter != 0 && System.nanoTime() - retryAfter < 0)
			{
				throw new IOException("Model download is temporarily delayed after a failure; try again in a minute");
			}
			try
			{
				install(directory);
			}
			catch (IOException e)
			{
				if (!Thread.currentThread().isInterrupted())
				{
					retryAfter = System.nanoTime() + TimeUnit.MINUTES.toNanos(1);
				}
				throw e;
			}
		}
		preparedDirectory = directory;
	}

	private byte[] readVerified(Filepath directory, String name) throws IOException
	{
		Filepath file = directory.joinSegment(name);
		if (!file.isFile() || file.size() > ModelRelease.MAX_MODEL_BYTES)
		{
			return null;
		}
		try (var stream = file.openInputStream())
		{
			byte[] bytes = stream.readNBytes(ModelRelease.MAX_MODEL_BYTES + 1);
			return matches(bytes, ModelRelease.HASHES.get(name)) ? bytes : null;
		}
	}

	private void install(Filepath directory) throws IOException
	{
		Request request = new Request.Builder().url(ModelRelease.URL).build();
		byte[] archive;
		try (var response = http.newCall(request).execute())
		{
			if (!response.isSuccessful() || response.body() == null)
			{
				throw new IOException("Model release download returned HTTP " + response.code());
			}
			archive = response.body().byteStream().readNBytes(ModelRelease.ARCHIVE_BYTES + 1);
		}
		checkInterrupted();
		if (archive.length != ModelRelease.ARCHIVE_BYTES || !matches(archive, ModelRelease.SHA256))
		{
			throw new IOException("Model release size or SHA-256 does not match this plugin version");
		}
		Filepath download = directory.createTempFile("download-", ".zip");
		try
		{
			download.write(archive);
			try (var zip = new ZipInputStream(download.openInputStream()))
			{
				Set<String> extracted = new HashSet<>();
				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null)
				{
					checkInterrupted();
					String name = entry.getName();
					// Only compiled catalog names can become paths, never arbitrary archive paths.
					if (entry.isDirectory() || !ModelRelease.HASHES.containsKey(name) || !extracted.add(name))
					{
						throw new IOException("Unexpected or duplicate model in release: " + name);
					}
					byte[] bytes = zip.readNBytes(ModelRelease.MAX_MODEL_BYTES + 1);
					if (bytes.length > ModelRelease.MAX_MODEL_BYTES || !matches(bytes, ModelRelease.HASHES.get(name)))
					{
						throw new IOException("Model size or SHA-256 mismatch: " + name);
					}
					writeModel(directory, name, bytes);
				}
				if (extracted.size() != ModelRelease.HASHES.size())
				{
					throw new IOException("Model release is incomplete");
				}
			}
		}
		finally
		{
			download.deleteIfExists();
		}
	}

	private static void writeModel(Filepath directory, String name, byte[] bytes) throws IOException
	{
		Filepath temporary = directory.createTempFile("model-", ".tmp");
		try
		{
			temporary.write(bytes);
			Filepath destination = directory.joinSegment(name);
			try
			{
				temporary.moveTo(destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException e)
			{
				temporary.moveTo(destination, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			temporary.deleteIfExists();
		}
	}

	private static boolean matches(byte[] bytes, String expected)
	{
		return Hashing.sha256().hashBytes(bytes).toString().equals(expected);
	}

	private static void checkInterrupted() throws InterruptedIOException
	{
		if (Thread.currentThread().isInterrupted())
		{
			throw new InterruptedIOException("Model loading cancelled");
		}
	}
}
