package com.customthralls;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Manual development launcher; this class contains no automated assertions. */
public class CustomThrallsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		List<String> arguments = new ArrayList<>(Arrays.asList(args));
		boolean preview = arguments.removeIf("--preview"::equals);
		if (preview)
		{
			// loadBuiltin replaces the entire list, so register both plugins in one call.
			ExternalPluginManager.loadBuiltin(CustomThrallsPlugin.class, ThrallPreviewPlugin.class);
			if (!arguments.contains("--developer-mode"))
			{
				arguments.add("--developer-mode");
			}
		}
		else
		{
			ExternalPluginManager.loadBuiltin(CustomThrallsPlugin.class);
		}
		RuneLite.main(arguments.toArray(new String[0]));
	}
}
