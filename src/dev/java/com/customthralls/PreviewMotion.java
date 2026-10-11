package com.customthralls;

/** Motion controls for the development preview. */
public enum PreviewMotion
{
	STILL("Still"),
	IDLE("Idle"),
	WALK("Walk"),
	ATTACK("Attack");

	private final String label;

	PreviewMotion(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
