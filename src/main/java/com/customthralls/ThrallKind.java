package com.customthralls;

import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;

/** The three families of Arceuus thralls. */
enum ThrallKind
{
	MAGIC, RANGED, MELEE;

	static ThrallKind forNpc(int npcId)
	{
		switch (npcId)
		{
			case NpcID.ARCEUUS_THRALL_GHOST_LESSER:
			case NpcID.ARCEUUS_THRALL_GHOST_SUPERIOR:
			case NpcID.ARCEUUS_THRALL_GHOST_GREATER:
				return MAGIC;
			case NpcID.ARCEUUS_THRALL_SKELETON_LESSER:
			case NpcID.ARCEUUS_THRALL_SKELETON_SUPERIOR:
			case NpcID.ARCEUUS_THRALL_SKELETON_GREATER:
				return RANGED;
			case NpcID.ARCEUUS_THRALL_ZOMBIE_LESSER:
			case NpcID.ARCEUUS_THRALL_ZOMBIE_SUPERIOR:
			case NpcID.ARCEUUS_THRALL_ZOMBIE_GREATER:
				return MELEE;
			default:
				return null;
		}
	}

	static boolean isAttackAnimation(int animationId)
	{
		switch (animationId)
		{
			case AnimationID.GHOST_UPDATE_TENDRILL_ATTACK_THRALL:
			case AnimationID.SKELETON_UPDATE_CHAMPION_ATTACK_THRALL:
			case AnimationID.ZOMBIE_UPDATE_ATTACK_NORMAL_THRALL:
				return true;
			default:
				return false;
		}
	}

	static boolean isWalkAnimation(int poseAnimationId)
	{
		switch (poseAnimationId)
		{
			case AnimationID.GHOST_UPDATE_TENDRILL_WALK_THRALL:
			case AnimationID.SKELETON_UPDATE_CHAMPION_WALK_THRALL:
			case AnimationID.ZOMBIE_UPDATE_WALK_NORMAL_THRALL:
			case AnimationID.ZOMBIE_UPDATE_WALK_LEFT_NORMAL_THRALL:
			case AnimationID.ZOMBIE_UPDATE_WALK_RIGHT_NORMAL_THRALL:
			case AnimationID.ZOMBIE_UPDATE_WALKBACKWARDS_WEAPON_THRALL:
				return true;
			default:
				return false;
		}
	}
}
