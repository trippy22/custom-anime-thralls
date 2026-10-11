package com.customthralls;

/** Shared motion for live replacements and the development preview. */
final class ThrallMotion
{
	private ThrallMotion() { }

	static float offset(float now, float phase, boolean walking, float attackElapsed, float scale)
	{
		float hop = attackElapsed >= 0 && attackElapsed < 0.5f
			? (float) Math.sin(attackElapsed * Math.PI / 0.5f) * 10f : 0;
		float bob = (float) (1 + Math.sin((now + phase) * (walking ? 12 : 3))) * (walking ? 2f : 0.5f);
		return (hop + bob) * scale;
	}
}
