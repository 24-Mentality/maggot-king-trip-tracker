package com.maggotkingtriptracker.tracking;

import static org.junit.Assert.assertEquals;
import com.google.common.collect.ImmutableSet;
import com.maggotkingtriptracker.model.ChargeType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpotanimID;
import org.junit.Test;

public class ChargeCounterTest
{
	private static final Set<Integer> WEAPONS = ImmutableSet.of(
		ItemID.NIGHTMARE_STAFF_HARMONISED, ItemID.WILD_CAVE_WEBWEAVER_CHARGED, ItemID.ELDER_MAUL,
		ItemID.SCYTHE_OF_VITUR, ItemID.TUMEKENS_SHADOW, ItemID.CRIMSON_KISTEN, ItemID.HALLOWFELL, ItemID.BLISTERWOOD_STAKE);
	private static final Set<Integer> AMULETS = ImmutableSet.of(ItemID.BLOOD_AMULET, 34428);

	private static final Pattern TICK = Pattern.compile("^tick=(\\d+) \\S+ \\S+ (\\w+) (.*)$");
	// Items are separated by ", "; names may contain parentheses, e.g. "Avernic treads (max) (31097) x1"
	private static final Pattern ITEM = Pattern.compile("(?:^|\\[|, )([+-]?)[^,\\[]*?\\((\\d+)\\) x\\d+");
	private static final Pattern SPOTANIMS = Pattern.compile("spotanims=\\[([\\d, ]*)\\]");
	private static final Pattern HITSPLAT = Pattern.compile("type=(\\d+) amount=(\\d+) target=npc");
	private static final Pattern ANIM = Pattern.compile("^id=(\\d+)");

	/**
	 * Diagnostic log of one kill (2026-09-27) with in-game Check messages before and after:
	 * blood fury 2,113 -> 2,096, Webweaver bow 591 -> 587, Tome of fire 6,590 -> 6,553.
	 */
	@Test
	public void replayedKillMatchesCheckMessages() throws Exception
	{
		Map<ChargeType, Integer> used = replay("charge-test-kill.log");

		assertEquals(Integer.valueOf(17), used.get(ChargeType.BLOOD_FURY));
		assertEquals(Integer.valueOf(4), used.get(ChargeType.WILDERNESS_WEAPON));
		assertEquals(Integer.valueOf(37), used.get(ChargeType.TOME_OF_FIRE));
	}

	/**
	 * Phosani's Nightmare between two in-game Check readings (2026-09-28): Scythe of Vitur 1,638 -> 1,602 and
	 * Tumeken's shadow 1,988 -> 1,942. Of 38 scythe attack animations, 2 missed with every hit, which uses no charge.
	 */
	@Test
	public void replayedPhosanisKillMatchesCheckMessages() throws Exception
	{
		Map<ChargeType, Integer> used = replay("charge-test-phosani-check.log");

		assertEquals(Integer.valueOf(36), used.get(ChargeType.SCYTHE_OF_VITUR));
		assertEquals(Integer.valueOf(46), used.get(ChargeType.TUMEKENS_SHADOW));
	}

	/**
	 * Two Phosani's Nightmare kills and a death (2026-09-28), switching between the scythe, Tumeken's shadow and
	 * other weapons: no attack is lost to the switches. 132 scythe attacks, 8 of them missing with every hit.
	 */
	@Test
	public void replayedPhosanisTripSurvivesWeaponSwitches() throws Exception
	{
		Map<ChargeType, Integer> used = replay("charge-test-phosani.log");

		assertEquals(Integer.valueOf(124), used.get(ChargeType.SCYTHE_OF_VITUR));
		assertEquals(Integer.valueOf(135), used.get(ChargeType.TUMEKENS_SHADOW));
	}

	@Test
	public void scytheBlocksMissesAndUnchargedScythesUseNoCharge()
	{
		Map<ChargeType, Integer> used = new EnumMap<>(ChargeType.class);
		ChargeCounter counter = new ChargeCounter(id -> true);

		counter.gearChanged(1, new ChargeCounter.Gear(ItemID.SCYTHE_OF_VITUR, -1, -1));
		counter.animation(2, AnimationID.SCYTHE_OF_VITUR_ATTACK);
		counter.hitsplat(3, HitsplatID.DAMAGE_ME_CYAN, 12);
		counter.animation(3, AnimationID.HUMAN_SCYTHE_BLOCK);
		// Switched to the scythe in the same tick as the attack
		counter.gearChanged(5, new ChargeCounter.Gear(ItemID.TUMEKENS_SHADOW, -1, -1));
		counter.animation(6, AnimationID.SCYTHE_OF_VITUR_ATTACK);
		counter.gearChanged(6, new ChargeCounter.Gear(ItemID.SCYTHE_OF_VITUR, -1, -1));
		counter.hitsplat(7, HitsplatID.DAMAGE_ME, 30);
		// Every hit missed
		counter.animation(10, AnimationID.SCYTHE_OF_VITUR_ATTACK);
		counter.hitsplat(11, HitsplatID.BLOCK_ME, 0);
		counter.hitsplat(11, HitsplatID.BLOCK_ME, 0);
		counter.gearChanged(13, new ChargeCounter.Gear(ItemID.SCYTHE_OF_VITUR_UNCHARGED, -1, -1));
		counter.animation(14, AnimationID.SCYTHE_OF_VITUR_ATTACK);
		counter.hitsplat(15, HitsplatID.DAMAGE_ME, 5);
		counter.process(20, (type, n) -> used.merge(type, n, Integer::sum));

		assertEquals(Integer.valueOf(2), used.get(ChargeType.SCYTHE_OF_VITUR));
	}

	@Test
	public void shadowCastsOnlyCountWithTheShadowWorn()
	{
		Map<ChargeType, Integer> used = new EnumMap<>(ChargeType.class);
		ChargeCounter counter = new ChargeCounter(id -> false);

		counter.gearChanged(1, new ChargeCounter.Gear(ItemID.TUMEKENS_SHADOW, -1, -1));
		counter.spotAnimsChanged(2, ImmutableSet.of(SpotanimID.TUMEKENS_SHADOW_CASTING));
		// The graphic re-applied in the same tick is still one cast
		counter.spotAnimsChanged(2, ImmutableSet.of(SpotanimID.TUMEKENS_SHADOW_CASTING));
		counter.gearChanged(3, new ChargeCounter.Gear(ItemID.TUMEKENS_SHADOW_UNCHARGED, -1, -1));
		counter.spotAnimsChanged(5, ImmutableSet.of(SpotanimID.TUMEKENS_SHADOW_CASTING));
		counter.process(10, (type, n) -> used.merge(type, n, Integer::sum));

		assertEquals(Integer.valueOf(1), used.get(ChargeType.TUMEKENS_SHADOW));
	}

	@Test
	public void rangedHitAfterSwitchingToMeleeUsesNoBloodFuryCharge()
	{
		Map<ChargeType, Integer> used = new EnumMap<>(ChargeType.class);
		ChargeCounter counter = new ChargeCounter(id -> id == ItemID.ELDER_MAUL);

		counter.gearChanged(10, new ChargeCounter.Gear(ItemID.WILD_CAVE_WEBWEAVER_CHARGED, -1, ItemID.BLOOD_AMULET));
		counter.animation(10, 0);
		counter.spotAnimsChanged(10, ImmutableSet.of(SpotanimID.WILD_CAVE_BOW_ARROW_LAUNCH02));
		counter.gearChanged(11, new ChargeCounter.Gear(ItemID.ELDER_MAUL, -1, ItemID.BLOOD_AMULET));
		counter.hitsplat(13, HitsplatID.DAMAGE_ME, 30);
		counter.animation(14, 0);
		counter.hitsplat(15, HitsplatID.DAMAGE_MAX_ME, 75);
		counter.hitsplat(16, HitsplatID.BLOCK_ME, 0);
		counter.process(20, (type, n) -> used.merge(type, n, Integer::sum));

		assertEquals(Integer.valueOf(1), used.get(ChargeType.WILDERNESS_WEAPON));
		assertEquals(Integer.valueOf(1), used.get(ChargeType.BLOOD_FURY));
	}

	@Test
	public void fireSpellWithoutTomeUsesNoCharge()
	{
		Map<ChargeType, Integer> used = new EnumMap<>(ChargeType.class);
		ChargeCounter counter = new ChargeCounter(id -> false);

		counter.gearChanged(1, new ChargeCounter.Gear(ItemID.NIGHTMARE_STAFF_HARMONISED, -1, -1));
		counter.spotAnimsChanged(2, ImmutableSet.of(SpotanimID.FIRESURGE_CASTING));
		// Tome equipped in the same tick as the cast still counts
		counter.spotAnimsChanged(6, ImmutableSet.of(SpotanimID.FIRESURGE_CASTING));
		counter.gearChanged(6, new ChargeCounter.Gear(ItemID.NIGHTMARE_STAFF_HARMONISED, ItemID.TOME_OF_FIRE, -1));
		counter.process(10, (type, n) -> used.merge(type, n, Integer::sum));

		assertEquals(Integer.valueOf(1), used.get(ChargeType.TOME_OF_FIRE));
	}

	private Map<ChargeType, Integer> replay(String resource) throws Exception
	{
		Map<ChargeType, Integer> used = new EnumMap<>(ChargeType.class);
		ChargeCounter counter = new ChargeCounter(id -> id == ItemID.ELDER_MAUL);
		Set<Integer> worn = new HashSet<>();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				Matcher m = TICK.matcher(line);
				if (!m.matches())
				{
					continue;
				}
				int tick = Integer.parseInt(m.group(1));
				String category = m.group(2);
				String details = m.group(3);

				switch (category)
				{
					case "EQUIPMENT":
						if (details.startsWith("snapshot"))
						{
							worn.clear();
						}
						Matcher item = ITEM.matcher(details);
						while (item.find())
						{
							int id = Integer.parseInt(item.group(2));
							if ("-".equals(item.group(1)))
							{
								worn.remove(id);
							}
							else
							{
								worn.add(id);
							}
						}
						counter.gearChanged(tick, gear(worn));
						break;
					case "ANIM":
						Matcher a = ANIM.matcher(details);
						counter.animation(tick, a.find() ? Integer.parseInt(a.group(1)) : -1);
						break;
					case "GRAPHIC":
						Matcher s = SPOTANIMS.matcher(details);
						Set<Integer> ids = new HashSet<>();
						if (s.find() && !s.group(1).isEmpty())
						{
							for (String id : s.group(1).split(", "))
							{
								ids.add(Integer.parseInt(id));
							}
						}
						counter.spotAnimsChanged(tick, ids);
						break;
					case "HITSPLAT":
						Matcher h = HITSPLAT.matcher(details);
						if (h.find())
						{
							counter.hitsplat(tick, Integer.parseInt(h.group(1)), Integer.parseInt(h.group(2)));
						}
						break;
					default:
						break;
				}
			}
		}

		counter.process(Integer.MAX_VALUE, (type, n) -> used.merge(type, n, Integer::sum));
		return used;
	}

	private static ChargeCounter.Gear gear(Set<Integer> worn)
	{
		int weapon = worn.stream().filter(WEAPONS::contains).findFirst().orElse(-1);
		int shield = worn.contains(ItemID.TOME_OF_FIRE) ? ItemID.TOME_OF_FIRE : -1;
		int amulet = worn.stream().filter(AMULETS::contains).findFirst().orElse(-1);
		return new ChargeCounter.Gear(weapon, shield, amulet);
	}
}
