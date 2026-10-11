package com.customthralls;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.RuneLiteObjectController;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/** A cosmetic mesh following a real thrall. Client thread only. */
final class ThrallReplacement extends RuneLiteObjectController
{
	private final Client client;
	private final NPC npc;
	private final Model model;
	private final boolean billboard;
	private final float scale;
	private final float timeOffset;
	private float attackStart = Float.NaN;
	private int lastAnimation = -1;
	private int lastFrame = -1;

	ThrallReplacement(Client client, NPC npc, Model model, RenderStyle style, float scale)
	{
		this.client = client;
		this.npc = npc;
		this.model = model;
		this.billboard = style == RenderStyle.PIXEL_ART;
		this.scale = scale;
		this.timeOffset = (npc.getIndex() % 97) * 0.173f;
		setRadius(60);
	}

	void activate()
	{
		client.registerRuneLiteObject(this);
	}

	void deactivate()
	{
		client.removeRuneLiteObject(this);
	}

	void sync(boolean animate)
	{
		LocalPoint point = npc.getLocalLocation();
		WorldView world = npc.getWorldView();
		if (point == null || world == null)
		{
			return;
		}
		setLocation(point, world.getPlane());
		setOrientation(billboard ? billboardOrientation(client.getCameraYaw()) : npc.getCurrentOrientation());
		float now = client.getGameCycle() * 0.02f;
		int animation = npc.getAnimation();
		int frame = npc.getAnimationFrame();
		if (ThrallKind.isAttackAnimation(animation) && (animation != lastAnimation || frame < lastFrame))
		{
			attackStart = now;
		}
		lastAnimation = animation;
		lastFrame = frame;
		float offset = 0;
		if (animate)
		{
			boolean walking = ThrallKind.isWalkAnimation(npc.getPoseAnimation());
			offset = ThrallMotion.offset(now, timeOffset, walking, now - attackStart, scale);
		}
		setZ(Perspective.getTileHeight(client, point, world.getPlane()) - Math.round(offset));
	}

	static int billboardOrientation(int cameraYaw)
	{
		return (2048 - cameraYaw) & 2047;
	}

	@Override
	public Model getModel()
	{
		return model;
	}
}
