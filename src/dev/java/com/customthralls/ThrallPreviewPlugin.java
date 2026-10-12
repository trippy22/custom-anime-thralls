package com.customthralls;

import com.google.inject.Provides;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Development-only cosmetic object, compiled with the launcher and excluded from the release JAR. */
@PluginDescriptor(name = "Custom Thralls Preview", description = "Development preview beside your player",
	developerPlugin = true, enabledByDefault = true)
public class ThrallPreviewPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(ThrallPreviewPlugin.class);
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private BundledModels bundledModels;
	@Inject private ScheduledExecutorService executor;
	@Inject private CustomModelLoader modelLoader;
	@Inject private ThrallPreviewConfig config;

	private volatile boolean enabled;
	private int generation;
	private Future<?> loadFuture;
	private ModelMesh asset;
	private int height;
	private RenderStyle style;
	private PreviewObject object;
	private final ModelCache<Model> models = new ModelCache<>(1);
	private String modelId;

	@Provides
	ThrallPreviewConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(ThrallPreviewConfig.class);
	}

	@Override
	protected void startUp()
	{
		enabled = true;
		clientThread.invoke(this::reload);
	}

	@Override
	protected void shutDown()
	{
		enabled = false;
		clientThread.invoke(() ->
		{
			++generation;
			if (loadFuture != null)
			{
				loadFuture.cancel(true);
				loadFuture = null;
			}
			removeObject();
			asset = null;
			models.clear();
		});
	}

	private void reload()
	{
		if (!enabled)
		{
			return;
		}
		int requestedGeneration = ++generation;
		if (loadFuture != null)
		{
			loadFuture.cancel(true);
		}
		removeObject();
		asset = null;
		models.clear();
		height = config.height();
		style = config.style();
		modelId = config.character().getModelId(style);
		String requestedModelId = modelId;
		if (requestedModelId == null)
		{
			return;
		}
		loadFuture = executor.submit(() ->
		{
			try
			{
				ModelMesh loaded = bundledModels.load(requestedModelId);
				clientThread.invoke(() ->
				{
					if (enabled && generation == requestedGeneration)
					{
						asset = loaded;
						ensureObject();
					}
				});
			}
			catch (ModelDataException e)
			{
				log.warn("Could not load preview model {}", requestedModelId, e);
			}
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ThrallPreviewConfig.GROUP.equals(event.getGroup()) && !"motion".equals(event.getKey()))
		{
			clientThread.invoke(this::reload);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			removeObject();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Cache building blocks may arrive after the selected mesh data. Retry at most once per game tick.
		ensureObject();
	}

	private void ensureObject()
	{
		Player player = client.getLocalPlayer();
		if (!enabled || object != null || asset == null || client.getGameState() != GameState.LOGGED_IN
			|| player == null || player.getWorldView() == null)
		{
			return;
		}
		Model model = models.getOrBuild(modelId, height, () -> modelLoader.build(asset, height));
		if (model != null)
		{
			object = new PreviewObject(model, style, CustomModelLoader.scaleFor(asset, height));
			syncObject();
			if (object != null)
			{
				client.registerRuneLiteObject(object);
			}
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		syncObject();
	}

	private void syncObject()
	{
		if (object == null)
		{
			return;
		}
		Player player = client.getLocalPlayer();
		if (!enabled || client.getGameState() != GameState.LOGGED_IN || player == null
			|| player.getWorldView() == null || player.getLocalLocation() == null)
		{
			removeObject();
			return;
		}
		WorldView world = player.getWorldView();
		LocalPoint location = player.getLocalLocation();
		int x = location.getX() + Perspective.LOCAL_TILE_SIZE;
		if (x >= world.getSizeX() * Perspective.LOCAL_TILE_SIZE)
		{
			x = location.getX() - Perspective.LOCAL_TILE_SIZE;
		}
		LocalPoint besidePlayer = new LocalPoint(x, location.getY(), world);
		object.setLocation(besidePlayer, world.getPlane());
		object.setOrientation(object.style == RenderStyle.PIXEL_ART
			? ThrallReplacement.billboardOrientation(client.getCameraYaw()) : player.getCurrentOrientation());
		float now = client.getGameCycle() * 0.02f;
		PreviewMotion motion = config.motion();
		float offset = motion == PreviewMotion.STILL ? 0 : ThrallMotion.offset(now, 0,
			motion == PreviewMotion.WALK, motion == PreviewMotion.ATTACK ? now % 2f : Float.NaN, object.scale);
		object.setZ(Perspective.getTileHeight(client, besidePlayer, world.getPlane()) - Math.round(offset));
	}

	private void removeObject()
	{
		if (object != null)
		{
			client.removeRuneLiteObject(object);
			object = null;
		}
	}

	private static final class PreviewObject extends RuneLiteObjectController
	{
		private final Model model;
		private final RenderStyle style;
		private final float scale;

		private PreviewObject(Model model, RenderStyle style, float scale)
		{
			this.model = model;
			this.style = style;
			this.scale = scale;
		}

		@Override
		public Model getModel() { return model; }
	}
}
