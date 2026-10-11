package com.customthralls;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(CustomThrallsConfig.GROUP)
public interface CustomThrallsConfig extends Config
{
	String GROUP = "custom-thralls";

	@ConfigItem(keyName = "ghostModel", name = "Ghost", position = 0,
		description = "Character for all tiers of ghost thralls")
	default ThrallCharacter ghostModel()
	{
		return ThrallCharacter.GOJO;
	}

	@ConfigItem(keyName = "ghostStyle", name = "Ghost style", position = 1,
		description = "Flat pixel art, 3D extrusion, or a rounded OSRS-style model")
	default RenderStyle ghostStyle()
	{
		return RenderStyle.OSRS_MODEL;
	}

	@ConfigItem(keyName = "skeletonModel", name = "Skeleton", position = 2,
		description = "Character for all tiers of skeleton thralls")
	default ThrallCharacter skeletonModel()
	{
		return ThrallCharacter.GOKU;
	}

	@ConfigItem(keyName = "skeletonStyle", name = "Skeleton style", position = 3,
		description = "Flat pixel art, 3D extrusion, or a rounded OSRS-style model")
	default RenderStyle skeletonStyle()
	{
		return RenderStyle.OSRS_MODEL;
	}

	@ConfigItem(keyName = "zombieModel", name = "Zombie", position = 4,
		description = "Character for all tiers of zombie thralls")
	default ThrallCharacter zombieModel()
	{
		return ThrallCharacter.PIKACHU;
	}

	@ConfigItem(keyName = "zombieStyle", name = "Zombie style", position = 5,
		description = "Flat pixel art, 3D extrusion, or a rounded OSRS-style model")
	default RenderStyle zombieStyle()
	{
		return RenderStyle.OSRS_MODEL;
	}

	@Range(min = 50, max = 1000)
	@ConfigItem(keyName = "modelHeight", name = "Model height", position = 6,
		description = "Height in game units; a player is roughly 180 units tall")
	default int modelHeight()
	{
		return 210;
	}

	@ConfigItem(keyName = "animate", name = "Animate", position = 7,
		description = "Add a gentle idle/walking bob and a hop when the thrall attacks")
	default boolean animate()
	{
		return true;
	}
}
