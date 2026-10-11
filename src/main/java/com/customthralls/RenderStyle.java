package com.customthralls;

/** How the selected character is drawn. */
public enum RenderStyle
{
	MODEL_3D("3D extrusion", "3d"),
	PIXEL_ART("Flat pixel art", "flat"),
	OSRS_MODEL("OSRS-style model", "osrs");

	private final String label;
	private final String resourceSuffix;

	RenderStyle(String label, String resourceSuffix)
	{
		this.label = label;
		this.resourceSuffix = resourceSuffix;
	}

	String getResourceSuffix()
	{
		return resourceSuffix;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
