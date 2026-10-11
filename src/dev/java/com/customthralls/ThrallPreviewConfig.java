package com.customthralls;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(ThrallPreviewConfig.GROUP)
public interface ThrallPreviewConfig extends Config
{
	String GROUP = "custom-thralls-preview";

	@ConfigItem(keyName = "character", name = "Character", position = 0,
		description = "Character to draw beside you; Original game thrall hides the preview")
	default ThrallCharacter character() { return ThrallCharacter.GOJO; }

	@ConfigItem(keyName = "style", name = "Style", position = 1,
		description = "Switch between flat pixel art, a 3D extrusion, and an OSRS-style model")
	default RenderStyle style() { return RenderStyle.OSRS_MODEL; }

	@Range(min = 50, max = 1000)
	@ConfigItem(keyName = "height", name = "Model height", position = 2,
		description = "Preview height in game units")
	default int height() { return 210; }

	@ConfigItem(keyName = "motion", name = "Motion", position = 3,
		description = "Simulated motion; Attack repeats the hop every two seconds")
	default PreviewMotion motion() { return PreviewMotion.IDLE; }
}
