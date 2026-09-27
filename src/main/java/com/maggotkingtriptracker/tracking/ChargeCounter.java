package com.maggotkingtriptracker.tracking;

import com.google.common.collect.ImmutableSet;
import com.maggotkingtriptracker.model.ChargeType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntPredicate;
import lombok.Value;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpotanimID;

/**
 * Counts charges used from the local player's attacks, since the game does not update its charge varbits live.
 * <ul>
 * <li>Tome of fire: a fire spell casting graphic on the player while the tome is worn.</li>
 * <li>Revenant bows: a revenant bow launch graphic on the player (one ether per shot or special).</li>
 * <li>Blood fury: each damaging hitsplat the player deals while wearing the amulet, if the player's last
 * attack before the hit was with a melee weapon. That excludes ranged and magic hits still in flight
 * after switching to melee.</li>
 * </ul>
 * Events are grouped by game tick and evaluated with the gear worn at the end of that tick, because gear
 * switches in the same tick can arrive after the attack. Validated against the game's Check messages.
 * Contains no client calls, so it can be tested by replaying a diagnostic log.
 */
class ChargeCounter
{
	static final Set<Integer> FIRE_CAST_SPOTANIMS = ImmutableSet.of(
		SpotanimID.FIRESTRIKE_CASTING,
		SpotanimID.FIREBOLT_CASTING,
		SpotanimID.FIREBLAST_CASTING,
		SpotanimID.FIREWAVE_CASTING,
		SpotanimID.FIRESURGE_CASTING,
		SpotanimID.FIRESURGE_CASTING_FAST
	);

	static final Set<Integer> REVENANT_BOW_SPOTANIMS = ImmutableSet.of(
		SpotanimID.WILD_CAVE_BOW_ARROW_LAUNCH,
		SpotanimID.WILD_CAVE_BOW_ARROW_LAUNCH02,
		SpotanimID.FX_WEBWEAVER01_LAUNCH_SPOTANIM
	);

	static final Set<Integer> DAMAGE_HITSPLATS = ImmutableSet.of(HitsplatID.DAMAGE_ME, HitsplatID.DAMAGE_MAX_ME);

	interface Listener
	{
		void chargesUsed(ChargeType type, int charges);
	}

	@Value
	static class Gear
	{
		static final Gear NONE = new Gear(-1, -1, -1);

		int weapon;
		int shield;
		int amulet;
	}

	private static class TickEvents
	{
		final Set<Integer> spotAnims = new HashSet<>();
		boolean animation;
		int damagingHits;
	}

	private final IntPredicate isMeleeWeapon;
	private final TreeMap<Integer, Gear> gearByTick = new TreeMap<>();
	private final TreeMap<Integer, TickEvents> pending = new TreeMap<>();
	private Boolean lastAttackMelee;

	ChargeCounter(IntPredicate isMeleeWeapon)
	{
		this.isMeleeWeapon = isMeleeWeapon;
	}

	void reset()
	{
		gearByTick.clear();
		pending.clear();
		lastAttackMelee = null;
	}

	/**
	 * Gear after a change during this tick; later calls in the same tick replace earlier ones.
	 */
	void gearChanged(int tick, Gear gear)
	{
		gearByTick.put(tick, gear);
	}

	/**
	 * The player's spot animations after a change. The game re-applies a casting or launch graphic on every
	 * attack, so each one counts once per tick.
	 */
	void spotAnimsChanged(int tick, Set<Integer> spotAnims)
	{
		events(tick).spotAnims.addAll(spotAnims);
	}

	void animation(int tick)
	{
		events(tick).animation = true;
	}

	/**
	 * A hitsplat the local player dealt to an NPC.
	 */
	void hitsplat(int tick, int type, int amount)
	{
		if (amount > 0 && DAMAGE_HITSPLATS.contains(type))
		{
			events(tick).damagingHits++;
		}
	}

	/**
	 * Evaluates every tick up to and including {@code lastCompleteTick}.
	 */
	void process(int lastCompleteTick, Listener listener)
	{
		while (!pending.isEmpty() && pending.firstKey() <= lastCompleteTick)
		{
			Map.Entry<Integer, TickEvents> entry = pending.pollFirstEntry();
			evaluate(entry.getKey(), entry.getValue(), listener);
		}

		// Keep only the gear in effect at the last complete tick, plus anything newer
		Integer floor = gearByTick.floorKey(lastCompleteTick);
		if (floor != null)
		{
			gearByTick.headMap(floor, false).clear();
		}
	}

	private void evaluate(int tick, TickEvents events, Listener listener)
	{
		Map.Entry<Integer, Gear> gearEntry = gearByTick.floorEntry(tick);
		Gear gear = gearEntry != null ? gearEntry.getValue() : Gear.NONE;

		boolean fireCast = events.spotAnims.stream().anyMatch(FIRE_CAST_SPOTANIMS::contains);
		if (fireCast && gear.getShield() == ItemID.TOME_OF_FIRE)
		{
			listener.chargesUsed(ChargeType.TOME_OF_FIRE, 1);
		}

		if (events.spotAnims.stream().anyMatch(REVENANT_BOW_SPOTANIMS::contains))
		{
			listener.chargesUsed(ChargeType.WILDERNESS_WEAPON, 1);
		}

		// Hits land at least a tick after the attack, so judge them by attacks from earlier ticks
		if (events.damagingHits > 0 && Boolean.TRUE.equals(lastAttackMelee) && gear.getAmulet() == ItemID.BLOOD_AMULET)
		{
			listener.chargesUsed(ChargeType.BLOOD_FURY, events.damagingHits);
		}

		if (events.animation)
		{
			lastAttackMelee = isMeleeWeapon.test(gear.getWeapon());
		}
	}

	private TickEvents events(int tick)
	{
		return pending.computeIfAbsent(tick, t -> new TickEvents());
	}
}
