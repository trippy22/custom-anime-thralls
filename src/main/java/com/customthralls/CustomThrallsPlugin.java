package com.customthralls;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Renderable;
import net.runelite.api.WorldView;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Custom Anime Thralls",
	internalName = "custom-thralls",
	description = "Replace Arceuus thralls with custom anime characters in OSRS-style 3D or pixel art",
	tags = {"thrall", "arceuus", "cosmetic", "anime", "pixel"}
)
public class CustomThrallsPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private RenderCallbackManager renderCallbackManager;
	@Inject private BundledModels bundledModels;
	@Inject private ScheduledExecutorService executor;
	@Inject private CustomThrallsConfig config;
	@Inject private CustomModelLoader modelLoader;

	// Client-thread-owned maps. Only the selected assets are kept in memory.
	private final Map<String, ModelMesh> assets = new HashMap<>();
	private final Map<NPC, ThrallReplacement> replacements = new HashMap<>();
	private final ModelCache<Model> models = new ModelCache<>(3);
	private final CacheRetryQueue<NPC> pendingReplacements = new CacheRetryQueue<>(
		() -> modelLoader.isCacheReady(), this::createReplacement);
	private volatile boolean enabled;
	private int loadGeneration;
	private Future<?> loadFuture;

	private final RenderCallback renderCallback = new RenderCallback()
	{
		@Override
		public boolean addEntity(Renderable renderable, boolean drawingUi)
		{
			return !(renderable instanceof NPC) || !replacements.containsKey(renderable);
		}
	};

	@Provides
	CustomThrallsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CustomThrallsConfig.class);
	}

	@Override
	protected void startUp()
	{
		enabled = true;
		renderCallbackManager.register(renderCallback);
		clientThread.invoke(this::reloadAssets);
	}

	@Override
	protected void shutDown()
	{
		enabled = false;
		renderCallbackManager.unregister(renderCallback);
		clientThread.invoke(() ->
		{
			++loadGeneration;
			if (loadFuture != null)
			{
				loadFuture.cancel(true);
				loadFuture = null;
			}
			removeAllReplacements();
			assets.clear();
			models.clear();
		});
	}

	private ThrallCharacter characterFor(ThrallKind kind)
	{
		switch (kind)
		{
			case MAGIC: return config.ghostModel();
			case RANGED: return config.skeletonModel();
			default: return config.zombieModel();
		}
	}

	private RenderStyle styleFor(ThrallKind kind)
	{
		switch (kind)
		{
			case MAGIC: return config.ghostStyle();
			case RANGED: return config.skeletonStyle();
			default: return config.zombieStyle();
		}
	}

	private void reloadAssets()
	{
		if (!enabled)
		{
			return;
		}
		int generation = ++loadGeneration;
		if (loadFuture != null)
		{
			loadFuture.cancel(true);
		}
		removeAllReplacements();
		assets.clear();
		models.clear();
		Set<String> requested = new HashSet<>();
		for (ThrallKind kind : ThrallKind.values())
		{
			String modelId = characterFor(kind).getModelId(styleFor(kind));
			if (modelId != null)
			{
				requested.add(modelId);
			}
		}
		loadFuture = executor.submit(() ->
		{
			Map<String, ModelMesh> loaded = new HashMap<>();
			for (String modelId : requested)
			{
				if (Thread.currentThread().isInterrupted())
				{
					return;
				}
				try
				{
					loaded.put(modelId, bundledModels.load(modelId));
				}
				catch (ModelDataException e)
				{
					log.warn("Could not load bundled model {}", modelId, e);
				}
			}
			clientThread.invoke(() ->
			{
				if (enabled && generation == loadGeneration)
				{
					assets.putAll(loaded);
					refreshReplacements();
				}
			});
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CustomThrallsConfig.GROUP.equals(event.getGroup()) && !"animate".equals(event.getKey()))
		{
			// Height changes only rebuild geometry; the selected mesh data are unchanged.
			clientThread.invoke(() ->
			{
				if ("modelHeight".equals(event.getKey()))
				{
					models.clear();
					refreshReplacements();
				}
				else
				{
					reloadAssets();
				}
			});
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (enabled)
		{
			pendingReplacements.tick();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		addReplacement(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		removeReplacement(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		removeReplacement(event.getNpc());
		addReplacement(event.getNpc());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
			case LOGIN_SCREEN:
			case HOPPING:
			case CONNECTION_LOST:
				removeAllReplacements();
				break;
			case LOGGED_IN:
				refreshReplacements();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		for (ThrallReplacement replacement : replacements.values())
		{
			replacement.sync(config.animate());
		}
	}

	private void refreshReplacements()
	{
		removeAllReplacements();
		WorldView world = client.getTopLevelWorldView();
		if (!enabled || world == null)
		{
			return;
		}
		for (NPC npc : world.npcs())
		{
			if (npc != null)
			{
				addReplacement(npc);
			}
		}
	}

	private void addReplacement(NPC npc)
	{
		ThrallKind kind = ThrallKind.forNpc(npc.getId());
		if (enabled && kind != null && !replacements.containsKey(npc)
			&& assets.containsKey(characterFor(kind).getModelId(styleFor(kind))))
		{
			String modelId = characterFor(kind).getModelId(styleFor(kind));
			if (models.get(modelId, config.modelHeight()) != null)
			{
				createReplacement(npc);
			}
			else
			{
				pendingReplacements.submit(npc);
			}
		}
	}

	private void createReplacement(NPC npc)
	{
		ThrallKind kind = ThrallKind.forNpc(npc.getId());
		if (!enabled || kind == null || replacements.containsKey(npc))
		{
			return;
		}
		RenderStyle style = styleFor(kind);
		String modelId = characterFor(kind).getModelId(style);
		ModelMesh asset = assets.get(modelId);
		if (asset == null)
		{
			return;
		}
		int height = config.modelHeight();
		Model model = models.getOrBuild(modelId, height, () -> modelLoader.build(asset, height));
		if (model == null)
		{
			return;
		}
		ThrallReplacement replacement = new ThrallReplacement(client, npc, model, style,
			CustomModelLoader.scaleFor(asset, config.modelHeight()));
		replacement.sync(config.animate());
		replacement.activate();
		replacements.put(npc, replacement);
	}

	private void removeReplacement(NPC npc)
	{
		pendingReplacements.remove(npc);
		ThrallReplacement replacement = replacements.remove(npc);
		if (replacement != null)
		{
			replacement.deactivate();
		}
	}

	private void removeAllReplacements()
	{
		pendingReplacements.clear();
		for (ThrallReplacement replacement : replacements.values())
		{
			replacement.deactivate();
		}
		replacements.clear();
	}
}
