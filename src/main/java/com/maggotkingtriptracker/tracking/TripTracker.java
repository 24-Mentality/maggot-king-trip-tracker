package com.maggotkingtriptracker.tracking;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.maggotkingtriptracker.MaggotKingIds;
import com.maggotkingtriptracker.MaggotKingRates;
import com.maggotkingtriptracker.MaggotKingTripTrackerConfig;
import com.maggotkingtriptracker.model.AccountHistory;
import com.maggotkingtriptracker.model.ChargeType;
import com.maggotkingtriptracker.model.CorpseChoice;
import com.maggotkingtriptracker.model.DeathRecord;
import com.maggotkingtriptracker.model.EggPop;
import com.maggotkingtriptracker.model.ItemEntry;
import com.maggotkingtriptracker.model.Kill;
import com.maggotkingtriptracker.model.KillGoal;
import com.maggotkingtriptracker.model.Trip;
import com.maggotkingtriptracker.model.TripEndReason;
import com.maggotkingtriptracker.model.TripMath;
import com.maggotkingtriptracker.persistence.HistoryStore;
import com.maggotkingtriptracker.pricing.PriceService;
import com.maggotkingtriptracker.view.DrynessView;
import com.maggotkingtriptracker.view.PanelState;
import com.maggotkingtriptracker.view.TripView;
import com.maggotkingtriptracker.view.ViewBuilder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;

/**
 * Tracks Maggot King trips from game events: trip boundaries, kills, loot, supplies, drops and deaths.
 * All state lives on the client thread. It only listens to events and never creates input or draws anything.
 */
@Slf4j
public class TripTracker
{
	/**
	 * How many ticks a menu click may precede the container change or ground spawn it caused.
	 */
	private static final int CLICK_MATCH_TICKS = 3;
	/**
	 * After a corpse click, ground spawns count as loot overflow and pet messages count for this kill.
	 */
	private static final int CORPSE_WINDOW_TICKS = 10;
	/**
	 * If the Loot Tracker has not reported the kill this many ticks after the corpse click,
	 * inventory gains are used instead.
	 */
	private static final int LOOT_FALLBACK_TICKS = 8;
	/**
	 * Paying the aranei scout goes through dialogue, so allow about a minute after clicking it.
	 */
	private static final int GRAVE_MOVE_WINDOW_TICKS = 100;
	/**
	 * Container changes this soon after respawning are the death itself, not consumption.
	 */
	private static final int POST_DEATH_IGNORE_TICKS = 5;
	/**
	 * A pet or dead-maggot message this soon after popping an egg belongs to that egg.
	 */
	private static final int EGG_WINDOW_TICKS = 10;
	private static final long PRE_ENTRY_WINDOW_MS = 60_000;
	/**
	 * Dropped items worth less than this each (empty vials are 2 gp) are junk, not a cost.
	 */
	private static final long JUNK_PRICE = 100;
	private static final long SAVE_DELAY_MS = 1_000;
	private static final long ACTIVE_SAVE_INTERVAL_MS = 60_000;

	private static final String OPTION_OPEN_STOMACH = "Open-stomach";
	private static final String OPTION_TAKE_EGGS = "Take-eggs";
	private static final String OPTION_DROP = "Drop";
	private static final String OPTION_POLISH = "Polish";
	private static final String OPTION_CAST = "Cast";
	private static final Set<String> CONSUME_OPTIONS = ImmutableSet.of("Eat", "Drink", "Cast");

	private final Client client;
	private final ClientThread clientThread;
	private final MaggotKingTripTrackerConfig config;
	private final PriceService prices;
	private final ViewBuilder viewBuilder;
	private final HistoryStore store;
	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final Consumer<PanelState> stateListener;
	private final Consumer<String> alerter;
	private final Runnable onLairEntered;
	private final AllTimeRecords allTimeRecords;
	private final InventoryLedger ledger;

	private AccountHistory history;
	private boolean readOnly;
	private long accountHash = -1;
	private boolean loading;

	private Trip currentTrip;
	/**
	 * When the player logged out mid-trip; the trip resumes if they are back within the grace period.
	 */
	private Long suspendedAt;
	private Trip lastEndedTrip;

	private boolean inLair;
	private boolean dead;
	private int ignoreDeltasUntilTick = -1;
	private DeathRecord pendingDeath;
	private int graveWindowEndTick = -1;

	private Kill lootKill;
	private int lastKillTick = -100;
	private int lastCorpseClickTick = -100;
	private boolean lootReceived;
	private int fallbackEndTick = -1;
	private final Map<Integer, Long> fallbackGains = new HashMap<>();
	private Long bossSpawnedAt;
	private Long bossDiedAt;
	private String bossName;

	private int lastBankTick = -100;
	private final Deque<Click> recentClicks = new ArrayDeque<>();
	private final List<GroundEntry> groundItems = new ArrayList<>();
	private final List<GroundEntry> recentDespawns = new ArrayList<>();
	private final Map<Integer, Long> pendingDrops = new HashMap<>();
	private final ChargeCounter chargeCounter;
	private final List<PendingPolish> pendingPolishes = new ArrayList<>();
	private EggPop lastEggPop;
	private int lastEggPopTick = -100;
	private int lastEggClickTick = -100;
	private int unclaimedPetMessageTick = -100;
	/**
	 * Last tick time while logged in, for the goal clock; 0 while logged out.
	 */
	private long lastGoalTickAt;
	private boolean runeIdsLoaded;
	private final Deque<PreEntryUse> preEntryUses = new ArrayDeque<>();

	private ScheduledFuture<?> saveFuture;
	private long lastPeriodicSave;
	private boolean viewDirty = true;
	private boolean historyDirty = true;
	private List<TripView> historyViews = Collections.emptyList();

	public TripTracker(Client client, ClientThread clientThread, MaggotKingTripTrackerConfig config,
		PriceService prices, HistoryStore store, Gson gson, ScheduledExecutorService executor,
		Consumer<PanelState> stateListener, Consumer<String> alerter, Runnable onLairEntered,
		ConfigManager configManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.prices = prices;
		this.viewBuilder = new ViewBuilder(prices);
		this.store = store;
		this.gson = gson;
		this.executor = executor;
		this.stateListener = stateListener;
		this.alerter = alerter;
		this.onLairEntered = onLairEntered;
		this.allTimeRecords = new AllTimeRecords(configManager, gson);
		this.ledger = new InventoryLedger(client);
		this.chargeCounter = new ChargeCounter(prices::isMeleeWeapon);
	}

	/**
	 * Call on the client thread after registering, to pick up a session that is already logged in.
	 */
	public void start()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			ensureAccountLoaded();
			chargeCounter.gearChanged(client.getTickCount(), gear(client.getItemContainer(InventoryID.WORN)));
		}
		pushState();
	}

	public void shutDown()
	{
		if (saveFuture != null)
		{
			saveFuture.cancel(false);
			saveFuture = null;
		}
	}

	// ---- Panel actions (call on the client thread) ----

	public void deleteTrip(String tripId)
	{
		if (history == null || readOnly)
		{
			return;
		}

		Trip trip = findTrip(tripId);
		if (trip == null)
		{
			return;
		}
		history.getTrips().remove(trip);
		if (trip == currentTrip)
		{
			currentTrip = null;
			suspendedAt = null;
			lootKill = null;
		}
		if (trip == lastEndedTrip)
		{
			lastEndedTrip = null;
		}
		if (pendingDeath != null && trip.getDeaths().contains(pendingDeath))
		{
			pendingDeath = null;
		}
		historyChanged();
		saveNow();
		pushState();
	}

	public void clearHistory()
	{
		if (history == null || readOnly)
		{
			return;
		}

		history.getTrips().clear();
		currentTrip = null;
		suspendedAt = null;
		lastEndedTrip = null;
		lootKill = null;
		pendingDeath = null;
		pendingDrops.clear();
		groundItems.clear();
		recentDespawns.clear();
		historyChanged();
		saveNow();
		pushState();
	}

	/**
	 * @return this account's full history as JSON, or null if none is loaded. Client thread.
	 */
	public String exportJson()
	{
		return history == null ? null : gson.toJson(history);
	}

	/**
	 * @return completed trips as CSV (oldest first), or null if no history is loaded. Client thread.
	 */
	public String exportCsv()
	{
		if (history == null)
		{
			return null;
		}

		StringBuilder csv = new StringBuilder("start,end,active_minutes,end_reason,kills,stomach,eggs,deaths,pet,"
			+ "loot_gp,supplies_gp,dropped_gp,death_costs_gp,net_gp,gp_per_hour,avg_kill_seconds\n");
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		for (Trip trip : history.getTrips())
		{
			if (trip.isOpen())
			{
				continue;
			}
			long net = TripMath.netProfit(trip);
			Long averageKill = TripMath.averageKillMs(Collections.singletonList(trip));
			csv.append(format.format(Instant.ofEpochMilli(trip.getStartedAt()).atZone(ZoneId.systemDefault()))).append(',')
				.append(format.format(Instant.ofEpochMilli(trip.getEndedAt()).atZone(ZoneId.systemDefault()))).append(',')
				.append(String.format(Locale.ROOT, "%.1f", trip.getActiveMs() / 60_000.0)).append(',')
				.append(trip.getEndReason()).append(',')
				.append(trip.getKills().size()).append(',')
				.append(TripMath.countChoice(trip, CorpseChoice.STOMACH)).append(',')
				.append(TripMath.countChoice(trip, CorpseChoice.EGGS)).append(',')
				.append(trip.getDeaths().size()).append(',')
				.append(trip.getKills().stream().anyMatch(Kill::isPet)).append(',')
				.append(TripMath.lootValue(trip)).append(',')
				.append(TripMath.supplyCost(trip)).append(',')
				.append(TripMath.droppedCost(trip)).append(',')
				.append(TripMath.deathCost(trip)).append(',')
				.append(net).append(',')
				.append(TripMath.gpPerHour(net, trip.getActiveMs())).append(',')
				.append(averageKill == null ? "" : String.format(Locale.ROOT, "%.1f", averageKill / 1000.0))
				.append('\n');
		}
		return csv.toString();
	}

	/**
	 * Describes what importing would do, or returns an error message starting with "!". Client thread.
	 */
	public String describeImport(AccountHistory imported)
	{
		if (history == null)
		{
			return "!Log in first so the history can be imported into your account.";
		}
		if (readOnly)
		{
			return "!This account's history is read-only.";
		}
		if (imported.getSchemaVersion() > AccountHistory.CURRENT_SCHEMA_VERSION)
		{
			return "!This file is from a newer version of the plugin.";
		}

		int added = 0;
		for (Trip trip : imported.getTrips())
		{
			if (trip.getId() != null && !trip.isOpen() && findTrip(trip.getId()) == null)
			{
				added++;
			}
		}
		String account = imported.getAccountHash() != 0 && imported.getAccountHash() != accountHash
			? " The file is from a different account" + (imported.getLastDisplayName() != null
			? " (" + imported.getLastDisplayName() + ")" : "") + "."
			: "";
		return "Add " + added + " of " + imported.getTrips().size() + " trips from the file to this account?"
			+ " Trips you already have are skipped." + account;
	}

	/**
	 * Adds trips, egg pops and polish outcomes from an export that aren't already here. Client thread.
	 */
	public void importHistory(AccountHistory imported)
	{
		if (describeImport(imported).startsWith("!"))
		{
			return;
		}

		for (Trip trip : imported.getTrips())
		{
			if (trip.getId() != null && !trip.isOpen() && findTrip(trip.getId()) == null)
			{
				history.getTrips().add(trip);
			}
		}
		history.getTrips().sort(Comparator.comparingLong(Trip::getStartedAt));

		if (imported.getEggPops() != null)
		{
			for (EggPop pop : imported.getEggPops())
			{
				boolean known = history.getEggPops().stream()
					.anyMatch(p -> p.getAt() == pop.getAt() && p.getEggItemId() == pop.getEggItemId());
				if (!known)
				{
					history.getEggPops().add(pop);
				}
			}
			history.getEggPops().sort(Comparator.comparingLong(EggPop::getAt));
		}

		// Tallies can't be told apart, so keep the larger count rather than adding (re-importing is safe)
		if (imported.getPolishOutcomes() != null)
		{
			imported.getPolishOutcomes().forEach((tarnished, outcomes) ->
			{
				Map<Integer, Integer> mine = history.getPolishOutcomes().computeIfAbsent(tarnished, k -> new LinkedHashMap<>());
				outcomes.forEach((result, count) -> mine.merge(result, count, Math::max));
			});
		}

		historyChanged();
		saveNow();
		pushState();
	}

	/**
	 * Sets the kill goal target, keeping progress if a goal is already running. 0 or less removes it.
	 */
	public void setGoal(int target)
	{
		if (history == null || readOnly)
		{
			return;
		}
		if (target <= 0)
		{
			history.setGoal(null);
		}
		else if (history.getGoal() == null)
		{
			history.setGoal(new KillGoal(target, System.currentTimeMillis(), 0));
		}
		else
		{
			history.getGoal().setTarget(target);
		}
		viewDirty = true;
		saveNow();
		pushState();
	}

	/**
	 * Restarts the goal's kill count and clock from now.
	 */
	public void resetGoal()
	{
		if (history == null || readOnly || history.getGoal() == null)
		{
			return;
		}
		history.getGoal().setStartedAt(System.currentTimeMillis());
		history.getGoal().setActiveMs(0);
		viewDirty = true;
		saveNow();
		pushState();
	}

	public void refreshView()
	{
		historyChanged();
		pushState();
	}

	// ---- Events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				ensureAccountLoaded();
				break;
			case LOGIN_SCREEN:
			case HOPPING:
				if (inLair)
				{
					suspendTrip(System.currentTimeMillis());
				}
				ledger.reset();
				chargeCounter.reset();
				lastGoalTickAt = 0;
				recentClicks.clear();
				preEntryUses.clear();
				pushState();
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		int tick = client.getTickCount();
		long now = System.currentTimeMillis();

		if (!runeIdsLoaded)
		{
			loadRuneIds();
		}
		tickGoalClock(now);

		// Attacks from the previous tick are complete, including gear switched in that tick
		chargeCounter.process(tick - 1, this::chargesUsed);

		// Changes are attributed to where the player was before any region change this tick
		Map<Integer, Long> delta = ledger.poll();
		if (!delta.isEmpty())
		{
			processDelta(delta, tick, now);
		}
		applyLootFallback(tick);
		applyPolishResolution(tick);

		int region = WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID();
		boolean nowInLair = region == MaggotKingIds.LAIR_REGION_ID;
		if (nowInLair && !inLair)
		{
			inLair = true;
			enterLair(now);
			onLairEntered.run();
		}
		else if (!nowInLair && inLair)
		{
			inLair = false;
			leaveLair(region, tick, now);
		}

		if (!inLair && currentTrip != null && suspendedAt != null
			&& now - suspendedAt > TimeUnit.MINUTES.toMillis(config.logoutGraceMinutes()))
		{
			endTrip(TripEndReason.LOGOUT, suspendedAt);
		}

		boolean goalRunning = history != null && history.getGoal() != null;
		if (((inLair && currentTrip != null) || goalRunning) && now - lastPeriodicSave > ACTIVE_SAVE_INTERVAL_MS)
		{
			lastPeriodicSave = now;
			requestSave();
		}

		prune(tick, now);
		if (viewDirty || historyDirty)
		{
			pushState();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId == InventoryID.INV || containerId == InventoryID.WORN)
		{
			ledger.markDirty();
		}
		if (containerId == InventoryID.WORN)
		{
			chargeCounter.gearChanged(client.getTickCount(), gear(event.getItemContainer()));
		}
		else if (containerId == InventoryID.BANK)
		{
			lastBankTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (InventoryLedger.RUNE_POUCH_VARBITS.contains(event.getVarbitId()))
		{
			ledger.markDirty();
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor == client.getLocalPlayer() && actor.getAnimation() != -1)
		{
			chargeCounter.animation(client.getTickCount());
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		Actor actor = event.getActor();
		if (actor != client.getLocalPlayer())
		{
			return;
		}

		Set<Integer> ids = new HashSet<>();
		for (ActorSpotAnim spotAnim : actor.getSpotAnims())
		{
			ids.add(spotAnim.getId());
		}
		chargeCounter.spotAnimsChanged(client.getTickCount(), ids);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Hitsplat hitsplat = event.getHitsplat();
		if (hitsplat.isMine() && event.getActor() instanceof NPC)
		{
			chargeCounter.hitsplat(client.getTickCount(), hitsplat.getHitsplatType(), hitsplat.getAmount());
		}
	}

	/**
	 * Charges used in the lair are supplies, priced from the item that recharges them.
	 */
	private void chargesUsed(ChargeType type, int used)
	{
		if (!inLair || currentTrip == null || dead)
		{
			return;
		}

		int chargeItemId = type == ChargeType.TOME_OF_FIRE ? config.tomePage().getItemId() : type.getChargeItemId();
		ItemEntries.merge(currentTrip.getSupplies(), ItemEntry.charges(type.getSourceItemId(), used,
			chargeItemId, prices.price(chargeItemId), type.getChargesPerItem()));
		viewDirty = true;
		requestSave();
	}

	private static ChargeCounter.Gear gear(ItemContainer worn)
	{
		if (worn == null)
		{
			return ChargeCounter.Gear.NONE;
		}
		return new ChargeCounter.Gear(
			wornId(worn, EquipmentInventorySlot.WEAPON),
			wornId(worn, EquipmentInventorySlot.SHIELD),
			wornId(worn, EquipmentInventorySlot.AMULET));
	}

	private static int wornId(ItemContainer worn, EquipmentInventorySlot slot)
	{
		Item item = worn.getItem(slot.getSlotIdx());
		return item == null ? -1 : item.getId();
	}


	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		int tick = client.getTickCount();
		String option = event.getMenuOption();
		int itemId = event.getItemId();
		recentClicks.addLast(new Click(tick, option, itemId, false));

		if (OPTION_POLISH.equals(option) && MaggotKingIds.TARNISHED_ITEMS.contains(itemId))
		{
			pendingPolishes.add(new PendingPolish(itemId, tick));
		}
		else if (MaggotKingIds.EGGS.contains(itemId) && isPopOption(option))
		{
			lastEggClickTick = tick;
		}

		NPC npc = event.getMenuEntry().getNpc();
		if (npc == null)
		{
			return;
		}

		if (npc.getId() == MaggotKingIds.CORPSE && inLair && currentTrip != null)
		{
			CorpseChoice choice = OPTION_OPEN_STOMACH.equalsIgnoreCase(option) ? CorpseChoice.STOMACH
				: OPTION_TAKE_EGGS.equalsIgnoreCase(option) ? CorpseChoice.EGGS
				: null;
			if (choice == null)
			{
				return;
			}

			if (tick - lastCorpseClickTick <= CORPSE_WINDOW_TICKS)
			{
				// Repeated clicks on the same corpse
				return;
			}

			Kill kill = lootKill != null && lootKill.getChoice() == null ? lootKill : newKill();
			kill.setChoice(choice);
			viewDirty = true;
			lastCorpseClickTick = tick;
			if (!lootReceived)
			{
				fallbackEndTick = tick + LOOT_FALLBACK_TICKS;
				fallbackGains.clear();
			}
		}
		else if (MaggotKingIds.ARANEI_DEATH_HELPERS.contains(npc.getId()) && pendingDeath != null)
		{
			graveWindowEndTick = tick + GRAVE_MOVE_WINDOW_TICKS;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = event.getMessage();
		long now = System.currentTimeMillis();
		int tick = client.getTickCount();

		if (ChatPatterns.isDeathMessage(message))
		{
			markDead(now);
			return;
		}
		if (ChatPatterns.isPetMessage(message))
		{
			petMessage(tick);
			return;
		}
		if (!inLair || currentTrip == null)
		{
			return;
		}

		Integer killCount = ChatPatterns.killCount(message, bossName());
		if (killCount != null)
		{
			recordKill(killCount, tick, now);
			return;
		}

		Long duration = ChatPatterns.fightDurationMs(message);
		if (duration != null)
		{
			if (lootKill != null && tick - lastKillTick <= 2)
			{
				lootKill.setDurationMs(duration);
				viewDirty = true;
			}
			return;
		}

	}

	/**
	 * Pet messages are shared by every pet, so they only count right after a corpse or egg interaction.
	 */
	private void petMessage(int tick)
	{
		if (inLair && currentTrip != null && lootKill != null && tick - lastCorpseClickTick <= CORPSE_WINDOW_TICKS)
		{
			lootKill.setPet(true);
			boolean listed = lootKill.getLoot().stream().anyMatch(e -> e.getItemId() == MaggotKingIds.PET_ITEM);
			if (!listed)
			{
				lootKill.getLoot().add(new ItemEntry(MaggotKingIds.PET_ITEM, 1, 0));
			}
			alertPet("Maggot King pet from the corpse!");
			viewDirty = true;
			requestSave();
		}
		else if (tick - lastEggClickTick <= EGG_WINDOW_TICKS)
		{
			if (lastEggPop != null && tick - lastEggPopTick <= EGG_WINDOW_TICKS)
			{
				eggPet(lastEggPop);
			}
			else
			{
				// The egg's removal hasn't been processed yet
				unclaimedPetMessageTick = tick;
			}
		}
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (event.getType() == LootRecordType.EVENT)
		{
			polishEvent(event);
			return;
		}
		if (!inLair || currentTrip == null || !bossName().equalsIgnoreCase(event.getName()))
		{
			return;
		}

		Kill kill = lootKillOrCreate();
		for (ItemStack stack : event.getItems())
		{
			addLoot(kill, stack.getId(), stack.getQuantity());
			alertForDrop(stack.getId(), stack.getQuantity());
		}
		lootReceived = true;
		fallbackEndTick = -1;
		fallbackGains.clear();
		viewDirty = true;
		requestSave();
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		Actor actor = event.getActor();
		if (actor == client.getLocalPlayer())
		{
			markDead(System.currentTimeMillis());
		}
		else if (actor instanceof NPC && ((NPC) actor).getId() == MaggotKingIds.BOSS)
		{
			bossDiedAt = System.currentTimeMillis();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (event.getNpc().getId() == MaggotKingIds.BOSS)
		{
			bossSpawnedAt = System.currentTimeMillis();
			bossDiedAt = null;
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		TileItem item = event.getItem();
		if (!inLair || currentTrip == null || item.getOwnership() != TileItem.OWNERSHIP_SELF)
		{
			return;
		}

		int tick = client.getTickCount();
		GroundKind kind;
		Kill kill = null;
		if (hasRecentClick(OPTION_DROP, item.getId(), tick))
		{
			kind = GroundKind.OWN_DROP;
		}
		else if (lootKill != null && tick - lastCorpseClickTick <= CORPSE_WINDOW_TICKS)
		{
			kind = GroundKind.LOOT_OVERFLOW;
			kill = lootKill;
			alertForDrop(item.getId(), item.getQuantity());
		}
		else
		{
			return;
		}

		groundItems.add(new GroundEntry(item.getId(), item.getQuantity(), event.getTile().getWorldLocation(), kind, kill, tick));
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		TileItem item = event.getItem();
		WorldPoint location = event.getTile().getWorldLocation();
		for (Iterator<GroundEntry> it = groundItems.iterator(); it.hasNext(); )
		{
			GroundEntry entry = it.next();
			if (entry.itemId == item.getId() && entry.quantity == item.getQuantity() && entry.location.equals(location))
			{
				it.remove();
				entry.tick = client.getTickCount();
				recentDespawns.add(entry);
				return;
			}
		}
	}

	// ---- Trip lifecycle ----

	private void enterLair(long now)
	{
		if (history == null)
		{
			// Picked up in onHistoryLoaded
			return;
		}

		dead = false;
		ignoreDeltasUntilTick = -1;
		pendingDeath = null;
		graveWindowEndTick = -1;
		resetKillState();

		if (currentTrip != null && suspendedAt != null
			&& now - suspendedAt > TimeUnit.MINUTES.toMillis(config.logoutGraceMinutes()))
		{
			endTrip(TripEndReason.LOGOUT, suspendedAt);
		}

		if (currentTrip != null)
		{
			// Back within the logout grace period
			suspendedAt = null;
		}
		else if (config.mergeReentries() && lastEndedTrip != null && lastEndedTrip.getEndedAt() != null
			&& now - lastEndedTrip.getEndedAt() <= TimeUnit.MINUTES.toMillis(config.mergeWindowMinutes())
			&& history.getTrips().contains(lastEndedTrip))
		{
			currentTrip = lastEndedTrip;
			currentTrip.setEndedAt(null);
			currentTrip.setEndReason(null);
		}
		else
		{
			currentTrip = new Trip();
			currentTrip.setId(UUID.randomUUID().toString());
			currentTrip.setStartedAt(now);
			history.getTrips().add(currentTrip);
		}
		lastEndedTrip = null;
		currentTrip.setSegmentStartedAt(now);
		currentTrip.setLastActiveAt(now);
		lastPeriodicSave = now;

		if (config.countPreEntrySupplies())
		{
			for (PreEntryUse use : preEntryUses)
			{
				if (now - use.at <= PRE_ENTRY_WINDOW_MS)
				{
					for (ItemEntry entry : use.items)
					{
						ItemEntries.merge(currentTrip.getSupplies(), entry);
					}
				}
			}
		}
		preEntryUses.clear();

		historyChanged();
		requestSave();
	}

	private void leaveLair(int region, int tick, long now)
	{
		TripEndReason reason = dead ? TripEndReason.DEATH
			: region == MaggotKingIds.LAIR_ENTRANCE_REGION_ID ? TripEndReason.WALKED_OUT
			: TripEndReason.TELEPORT;
		if (dead)
		{
			dead = false;
			ignoreDeltasUntilTick = tick + POST_DEATH_IGNORE_TICKS;
		}

		if (currentTrip == null)
		{
			return;
		}

		finalizeDrops();
		commitSegment(now);
		endTrip(reason, now);
	}

	private void suspendTrip(long now)
	{
		inLair = false;
		if (currentTrip == null)
		{
			return;
		}

		finalizeDrops();
		commitSegment(now);
		suspendedAt = now;
		resetKillState();
		viewDirty = true;
		saveNow();
	}

	private void endTrip(TripEndReason reason, long at)
	{
		currentTrip.setEndedAt(at);
		currentTrip.setEndReason(reason);
		currentTrip.setLastActiveAt(at);
		lastEndedTrip = currentTrip;
		currentTrip = null;
		suspendedAt = null;
		resetKillState();
		historyChanged();
		requestSave();
	}

	private void commitSegment(long now)
	{
		Long segmentStart = currentTrip.getSegmentStartedAt();
		if (segmentStart != null)
		{
			currentTrip.setActiveMs(currentTrip.getActiveMs() + Math.max(0, now - segmentStart));
			currentTrip.setSegmentStartedAt(null);
		}
		currentTrip.setLastActiveAt(now);
	}

	private void markDead(long now)
	{
		if (dead || !inLair)
		{
			return;
		}
		dead = true;
		ignoreDeltasUntilTick = Integer.MAX_VALUE;
		if (currentTrip != null)
		{
			DeathRecord death = new DeathRecord();
			death.setAt(now);
			currentTrip.getDeaths().add(death);
			pendingDeath = death;
			viewDirty = true;
			requestSave();
		}
	}

	// ---- Kills and loot ----

	private void recordKill(int killCount, int tick, long now)
	{
		Kill kill = new Kill();
		kill.setKillCount(killCount);
		kill.setEndedAt(now);
		if (bossSpawnedAt != null)
		{
			long end = bossDiedAt != null ? bossDiedAt : now;
			kill.setDurationMs(Math.max(0, end - bossSpawnedAt));
		}
		currentTrip.getKills().add(kill);
		lootKill = kill;
		lastKillTick = tick;
		lootReceived = false;
		fallbackEndTick = -1;
		fallbackGains.clear();
		viewDirty = true;
		requestSave();
	}

	/**
	 * The kill that loot should be attached to. Creates one if the kill-count message was missed.
	 */
	private Kill lootKillOrCreate()
	{
		return lootKill != null ? lootKill : newKill();
	}

	/**
	 * A kill without a kill-count message, so loot still has somewhere to go.
	 */
	private Kill newKill()
	{
		Kill kill = new Kill();
		kill.setEndedAt(System.currentTimeMillis());
		currentTrip.getKills().add(kill);
		lootKill = kill;
		lootReceived = false;
		fallbackEndTick = -1;
		fallbackGains.clear();
		return kill;
	}

	private void addLoot(Kill kill, int itemId, long quantity)
	{
		if (MaggotKingIds.TARNISHED_ITEMS.contains(itemId))
		{
			// Real value is only known once polished; see resolvePolish
			for (long i = 0; i < quantity; i++)
			{
				ItemEntry pending = new ItemEntry(itemId, 1, 0);
				pending.setPending(true);
				pending.setPendingId(UUID.randomUUID().toString());
				kill.getLoot().add(pending);
			}
		}
		else
		{
			ItemEntries.merge(kill.getLoot(), itemId, quantity, prices.price(itemId), false);
		}
	}

	private void applyLootFallback(int tick)
	{
		if (fallbackEndTick < 0 || tick <= fallbackEndTick)
		{
			return;
		}

		if (!lootReceived && lootKill != null && currentTrip != null && !fallbackGains.isEmpty())
		{
			log.debug("No Loot Tracker event for this kill; using inventory changes");
			for (Map.Entry<Integer, Long> e : fallbackGains.entrySet())
			{
				addLoot(lootKill, e.getKey(), e.getValue());
				alertForDrop(e.getKey(), e.getValue());
			}
			lootReceived = true;
			viewDirty = true;
			requestSave();
		}
		fallbackEndTick = -1;
		fallbackGains.clear();
	}

	private void resetKillState()
	{
		lootKill = null;
		lootReceived = false;
		fallbackEndTick = -1;
		fallbackGains.clear();
		lastCorpseClickTick = -100;
		groundItems.clear();
		recentDespawns.clear();
	}

	// ---- Kill goal ----

	private void tickGoalClock(long now)
	{
		KillGoal goal = history != null ? history.getGoal() : null;
		if (goal != null && lastGoalTickAt > 0)
		{
			long elapsed = now - lastGoalTickAt;
			// Ticks are 0.6s apart; a longer gap means the client was paused, so don't count it
			if (elapsed > 0 && elapsed < 5_000)
			{
				goal.setActiveMs(goal.getActiveMs() + elapsed);
			}
		}
		lastGoalTickAt = now;
	}

	private void loadRuneIds()
	{
		EnumComposition runeEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runeEnum == null)
		{
			return;
		}
		Set<Integer> ids = new HashSet<>();
		for (int id : runeEnum.getIntVals())
		{
			if (id > 0)
			{
				ids.add(id);
			}
		}
		viewBuilder.setRuneIds(ids);
		runeIdsLoaded = true;
		viewDirty = true;
	}

	// ---- Tarnished items ----

	/**
	 * The Loot Tracker's polish event lists every inventory change in that tick, which can include a potion sip
	 * or gear that was just equipped. Its items are only candidates; see {@link #applyPolishResolution}.
	 */
	private void polishEvent(LootReceived event)
	{
		int tarnishedId = tarnishedIdForName(event.getName());
		if (tarnishedId < 0)
		{
			return;
		}

		PendingPolish polish = null;
		for (PendingPolish pending : pendingPolishes)
		{
			if (pending.tarnishedId == tarnishedId && pending.eventItems.isEmpty())
			{
				polish = pending;
				break;
			}
		}
		if (polish == null)
		{
			polish = new PendingPolish(tarnishedId, client.getTickCount());
			pendingPolishes.add(polish);
		}
		for (ItemStack stack : event.getItems())
		{
			if (isPolishResultCandidate(stack.getId()))
			{
				polish.eventItems.add(stack.getId());
			}
		}
	}

	/**
	 * Net gains across inventory, equipment and rune pouch just after a Polish click. Equipping gear in the same
	 * tick nets out here, so it can't be mistaken for the result.
	 */
	private void collectPolishResults(Map<Integer, Long> gained, int tick)
	{
		for (PendingPolish polish : pendingPolishes)
		{
			if (tick - polish.tick <= CLICK_MATCH_TICKS)
			{
				for (int itemId : gained.keySet())
				{
					if (isPolishResultCandidate(itemId))
					{
						polish.netGains.add(itemId);
					}
				}
			}
		}
	}

	private void applyPolishResolution(int tick)
	{
		for (Iterator<PendingPolish> it = pendingPolishes.iterator(); it.hasNext(); )
		{
			PendingPolish polish = it.next();
			if (tick - polish.tick <= CLICK_MATCH_TICKS + 1)
			{
				continue;
			}
			it.remove();

			Integer result = choosePolishResult(polish.eventItems, polish.netGains);
			if (result != null)
			{
				resolvePolish(polish.tarnishedId, result);
			}
			else if (!polish.eventItems.isEmpty() || !polish.netGains.isEmpty())
			{
				// Leave the drop pending rather than guess
				log.debug("Ambiguous polish result for {}: event {} gains {}", polish.tarnishedId, polish.eventItems, polish.netGains);
			}
		}
	}

	/**
	 * Picks the polished item: one the Loot Tracker reported that is also a real net gain; otherwise the only
	 * net gain; otherwise the only reported item if nothing was gained. Returns null when ambiguous.
	 */
	static Integer choosePolishResult(Set<Integer> eventItems, Set<Integer> netGains)
	{
		for (int itemId : eventItems)
		{
			if (netGains.contains(itemId))
			{
				return itemId;
			}
		}
		if (netGains.size() == 1)
		{
			return netGains.iterator().next();
		}
		if (netGains.isEmpty() && eventItems.size() == 1)
		{
			return eventItems.iterator().next();
		}
		return null;
	}

	/**
	 * Records the outcome and gives the oldest pending drop of this type its real item and value.
	 */
	private void resolvePolish(int tarnishedId, int resultId)
	{
		if (history == null || readOnly)
		{
			return;
		}

		history.getPolishOutcomes().computeIfAbsent(tarnishedId, k -> new HashMap<>()).merge(resultId, 1, Integer::sum);

		ItemEntry pending = oldestPending(tarnishedId);
		if (pending != null)
		{
			pending.setItemId(resultId);
			pending.setPriceEach(prices.price(resultId));
			pending.setPending(false);
			pending.setPolishedFrom(tarnishedId);
			alertForDrop(resultId, 1);
		}
		historyChanged();
		requestSave();
	}

	private ItemEntry oldestPending(int tarnishedId)
	{
		for (Trip trip : history.getTrips())
		{
			for (Kill kill : trip.getKills())
			{
				for (ItemEntry entry : kill.getLoot())
				{
					if (entry.isPending() && entry.getItemId() == tarnishedId)
					{
						return entry;
					}
				}
			}
		}
		return null;
	}

	private int tarnishedIdForName(String name)
	{
		for (int id : MaggotKingIds.TARNISHED_ITEMS)
		{
			if (prices.name(id).equalsIgnoreCase(name))
			{
				return id;
			}
		}
		return -1;
	}

	private boolean isPolishResultCandidate(int itemId)
	{
		return itemId != ItemID.VIAL_EMPTY && itemId != ItemID.COINS
			&& !MaggotKingIds.TARNISHED_ITEMS.contains(itemId) && prices.doseInfo(itemId) == null;
	}

	// ---- Eggs ----

	private void recordEggPops(Map<Integer, Long> removed, int tick, long now)
	{
		if (history == null || readOnly || tick - lastEggClickTick > CLICK_MATCH_TICKS)
		{
			return;
		}

		for (int eggId : MaggotKingIds.EGGS)
		{
			Long quantity = removed.get(eggId);
			if (quantity == null || !hasRecentPopClick(eggId, tick))
			{
				continue;
			}
			for (long i = 0; i < quantity; i++)
			{
				EggPop pop = new EggPop(eggId, now, false);
				history.getEggPops().add(pop);
				lastEggPop = pop;
				lastEggPopTick = tick;
			}
			if (tick - unclaimedPetMessageTick <= EGG_WINDOW_TICKS)
			{
				unclaimedPetMessageTick = -100;
				eggPet(lastEggPop);
			}
			historyChanged();
			requestSave();
		}
	}

	private void eggPet(EggPop pop)
	{
		if (pop.isPet())
		{
			return;
		}
		pop.setPet(true);
		alertPet("Maggot King pet from a " + prices.name(pop.getEggItemId()) + "!");
		historyChanged();
		requestSave();
	}

	private boolean hasRecentPopClick(int eggId, int tick)
	{
		for (Click click : recentClicks)
		{
			if (click.itemId == eggId && tick - click.tick <= CLICK_MATCH_TICKS && isPopOption(click.option))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Any egg option that isn't dropping, using, examining or moving it. The pop option isn't hardcoded
	 * because it has not been confirmed in game.
	 */
	private static boolean isPopOption(String option)
	{
		if (option == null)
		{
			return false;
		}
		String o = option.toLowerCase();
		return !(o.equals("drop") || o.equals("use") || o.equals("examine") || o.equals("destroy")
			|| o.equals("cancel") || o.startsWith("deposit") || o.startsWith("withdraw") || o.startsWith("offer")
			|| o.startsWith("store") || o.startsWith("bank") || o.startsWith("take"));
	}

	// ---- Alerts ----

	private void alertForDrop(int itemId, long quantity)
	{
		if (MaggotKingRates.UNIQUES.containsKey(itemId))
		{
			if (config.alertUniques())
			{
				alerter.accept("Maggot King unique: " + prices.name(itemId) + "!");
			}
			return;
		}
		if (itemId == MaggotKingIds.PET_ITEM || MaggotKingIds.TARNISHED_ITEMS.contains(itemId))
		{
			return;
		}

		long value = quantity * prices.price(itemId);
		if (config.alertValue() > 0 && value >= config.alertValue())
		{
			alerter.accept("Maggot King drop: " + (quantity > 1 ? QuantityFormatter.formatNumber(quantity) + " x " : "")
				+ prices.name(itemId) + " (" + QuantityFormatter.quantityToStackSize(value) + " gp)");
		}
	}

	private void alertPet(String message)
	{
		if (config.alertPet())
		{
			alerter.accept(message);
		}
	}

	// ---- Inventory changes ----

	private void processDelta(Map<Integer, Long> delta, int tick, long now)
	{
		Map<Integer, Long> removed = new HashMap<>();
		Map<Integer, Long> gained = new HashMap<>();
		for (Map.Entry<Integer, Long> e : delta.entrySet())
		{
			if (e.getValue() < 0)
			{
				removed.put(e.getKey(), -e.getValue());
			}
			else
			{
				gained.put(e.getKey(), e.getValue());
			}
		}

		recordGraveMovePayment(removed, tick);

		boolean trackingTrip = inLair && currentTrip != null && !dead;
		recordDrops(removed, tick, trackingTrip);

		recordEggPops(removed, tick, now);
		collectPolishResults(gained, tick);

		// Popping eggs, polishing (tarnished items, dull ancient medals) and casting a spell on an item
		// (e.g. High Level Alchemy) convert items rather than use them up
		removed.keySet().removeAll(MaggotKingIds.EGGS);
		removed.keySet().removeAll(MaggotKingIds.TARNISHED_ITEMS);
		removeConvertedItems(removed, tick);

		if (trackingTrip)
		{
			matchPickups(gained, tick);
		}

		List<ItemEntry> used = tick <= ignoreDeltasUntilTick ? Collections.emptyList() : consumption(removed, gained);

		if (trackingTrip && fallbackEndTick >= 0 && !lootReceived && !hasRecentClick(OPTION_POLISH, -1, tick))
		{
			for (Map.Entry<Integer, Long> e : gained.entrySet())
			{
				if (e.getKey() != ItemID.VIAL_EMPTY && prices.doseInfo(e.getKey()) == null)
				{
					fallbackGains.merge(e.getKey(), e.getValue(), Long::sum);
				}
			}
		}

		if (used.isEmpty())
		{
			return;
		}

		if (trackingTrip)
		{
			for (ItemEntry entry : used)
			{
				ItemEntries.merge(currentTrip.getSupplies(), entry);
			}
			viewDirty = true;
			requestSave();
		}
		else if (!inLair && !dead && config.countPreEntrySupplies()
			&& tick - lastBankTick > CLICK_MATCH_TICKS && hasRecentConsumeClick(tick))
		{
			preEntryUses.addLast(new PreEntryUse(now, used));
		}
	}

	private void recordGraveMovePayment(Map<Integer, Long> removed, int tick)
	{
		if (pendingDeath == null || tick > graveWindowEndTick || inLair)
		{
			return;
		}

		for (int itemId : MaggotKingIds.GRAVE_MOVE_PAYMENTS)
		{
			Long quantity = removed.remove(itemId);
			if (quantity != null)
			{
				pendingDeath.setGraveMoveCost(pendingDeath.getGraveMoveCost() + quantity * prices.price(itemId));
				historyChanged();
				requestSave();
			}
		}
	}

	private void recordDrops(Map<Integer, Long> removed, int tick, boolean trackingTrip)
	{
		for (Click click : recentClicks)
		{
			if (click.consumed || !OPTION_DROP.equals(click.option) || tick - click.tick > CLICK_MATCH_TICKS)
			{
				continue;
			}
			Long quantity = removed.remove(click.itemId);
			if (quantity != null)
			{
				click.consumed = true;
				if (trackingTrip)
				{
					pendingDrops.merge(click.itemId, quantity, Long::sum);
				}
			}
		}
	}

	private void removeConvertedItems(Map<Integer, Long> removed, int tick)
	{
		for (Click click : recentClicks)
		{
			if (tick - click.tick <= CLICK_MATCH_TICKS && click.itemId > 0
				&& (OPTION_POLISH.equals(click.option) || OPTION_CAST.equals(click.option)))
			{
				removed.remove(click.itemId);
			}
		}
	}

	/**
	 * Gains that match a ground item just picked up: loot overflow becomes loot, own drops are no longer lost.
	 * Matched quantities are removed from {@code gained}.
	 */
	private void matchPickups(Map<Integer, Long> gained, int tick)
	{
		for (Iterator<GroundEntry> it = recentDespawns.iterator(); it.hasNext(); )
		{
			GroundEntry entry = it.next();
			Long available = gained.get(entry.itemId);
			if (available == null || tick - entry.tick > CLICK_MATCH_TICKS)
			{
				continue;
			}

			long taken = Math.min(available, entry.quantity);
			if (entry.kind == GroundKind.LOOT_OVERFLOW)
			{
				addLoot(entry.kill, entry.itemId, taken);
				viewDirty = true;
				requestSave();
			}
			else
			{
				pendingDrops.computeIfPresent(entry.itemId, (id, q) -> q - taken > 0 ? q - taken : null);
			}

			if (available - taken > 0)
			{
				gained.put(entry.itemId, available - taken);
			}
			else
			{
				gained.remove(entry.itemId);
			}
			entry.quantity -= taken;
			if (entry.quantity <= 0)
			{
				it.remove();
			}
		}
	}

	/**
	 * Converts removed items into supply lines. Potions are counted in doses: a Prayer potion(4) becoming
	 * a Prayer potion(3) is one dose, priced from the highest-dose variant.
	 */
	private List<ItemEntry> consumption(Map<Integer, Long> removed, Map<Integer, Long> gained)
	{
		List<ItemEntry> used = new ArrayList<>();
		Map<String, long[]> doseFamilies = new HashMap<>();

		for (Map.Entry<Integer, Long> e : removed.entrySet())
		{
			int itemId = e.getKey();
			PriceService.DoseInfo dose = prices.doseInfo(itemId);
			if (dose != null)
			{
				// [net doses used, a variant id seen, its dose count]
				long[] family = doseFamilies.computeIfAbsent(dose.getFamily(), f -> new long[]{0, itemId, dose.getDoses()});
				family[0] += e.getValue() * dose.getDoses();
			}
			else
			{
				used.add(new ItemEntry(itemId, e.getValue(), prices.price(itemId)));
			}
		}

		for (Map.Entry<Integer, Long> e : gained.entrySet())
		{
			PriceService.DoseInfo dose = prices.doseInfo(e.getKey());
			if (dose != null && doseFamilies.containsKey(dose.getFamily()))
			{
				doseFamilies.get(dose.getFamily())[0] -= e.getValue() * dose.getDoses();
			}
		}

		for (Map.Entry<String, long[]> e : doseFamilies.entrySet())
		{
			long[] family = e.getValue();
			if (family[0] > 0)
			{
				PriceService.FullDose full = prices.fullDose(e.getKey(), (int) family[1], (int) family[2]);
				ItemEntry entry = new ItemEntry(full.getItemId(), family[0], prices.pricePerDose(full));
				entry.setPerDose(true);
				used.add(entry);
			}
		}
		return used;
	}

	private void finalizeDrops()
	{
		for (Map.Entry<Integer, Long> e : pendingDrops.entrySet())
		{
			long price = prices.price(e.getKey());
			if (price >= JUNK_PRICE && e.getValue() > 0)
			{
				ItemEntries.merge(currentTrip.getDropped(), e.getKey(), e.getValue(), price, false);
			}
		}
		pendingDrops.clear();
	}

	// ---- Helpers ----

	private boolean hasRecentClick(String option, int itemId, int tick)
	{
		for (Click click : recentClicks)
		{
			if (tick - click.tick <= CLICK_MATCH_TICKS && option.equals(click.option)
				&& (itemId < 0 || click.itemId == itemId))
			{
				return true;
			}
		}
		return false;
	}

	private boolean hasRecentConsumeClick(int tick)
	{
		for (Click click : recentClicks)
		{
			if (tick - click.tick <= CLICK_MATCH_TICKS && CONSUME_OPTIONS.contains(click.option))
			{
				return true;
			}
		}
		return false;
	}

	private void prune(int tick, long now)
	{
		while (!recentClicks.isEmpty() && tick - recentClicks.peekFirst().tick > CLICK_MATCH_TICKS * 2)
		{
			recentClicks.removeFirst();
		}
		recentDespawns.removeIf(d -> tick - d.tick > CLICK_MATCH_TICKS);
		while (!preEntryUses.isEmpty() && now - preEntryUses.peekFirst().at > PRE_ENTRY_WINDOW_MS)
		{
			preEntryUses.removeFirst();
		}
		if (pendingDeath != null && !inLair && graveWindowEndTick >= 0 && tick > graveWindowEndTick)
		{
			graveWindowEndTick = -1;
		}
	}

	/**
	 * All-time records from RuneLite's Loot Tracker and Chat Commands, combined with pets this plugin tracked.
	 */
	private DrynessView.AllTime allTime()
	{
		if (history == null)
		{
			return null;
		}
		AllTimeRecords.Snapshot snapshot = allTimeRecords.read(bossName());
		if (snapshot == null)
		{
			return null;
		}

		int trackedPets = 0;
		for (Trip trip : history.getTrips())
		{
			for (Kill kill : trip.getKills())
			{
				trackedPets += kill.isPet() ? 1 : 0;
			}
		}
		for (EggPop pop : history.getEggPops())
		{
			trackedPets += pop.isPet() ? 1 : 0;
		}
		return new DrynessView.AllTime(snapshot.getLootKills(), snapshot.getKillCount(), snapshot.getFirstRecordedAt(),
			snapshot.dropped(MaggotKingIds.UNIQUES_FANG), snapshot.dropped(MaggotKingIds.UNIQUES_KISTEN),
			Math.max(snapshot.dropped(MaggotKingIds.PET_ITEM), trackedPets));
	}

	private String bossName()
	{
		if (bossName == null)
		{
			bossName = client.getNpcDefinition(MaggotKingIds.BOSS).getName();
		}
		return bossName;
	}

	private Trip findTrip(String id)
	{
		for (Trip trip : history.getTrips())
		{
			if (trip.getId().equals(id))
			{
				return trip;
			}
		}
		return null;
	}

	// ---- Accounts and persistence ----

	private void ensureAccountLoaded()
	{
		long hash = client.getAccountHash();
		if (hash == -1 || hash == accountHash)
		{
			return;
		}

		if (history != null)
		{
			if (currentTrip != null)
			{
				endTrip(TripEndReason.LOGOUT, suspendedAt != null ? suspendedAt : System.currentTimeMillis());
			}
			saveNow();
		}

		accountHash = hash;
		history = null;
		readOnly = false;
		loading = true;
		currentTrip = null;
		suspendedAt = null;
		lastEndedTrip = null;
		pendingDeath = null;
		historyViews = Collections.emptyList();
		store.load(hash, result -> clientThread.invokeLater(() -> onHistoryLoaded(hash, result)));
	}

	private void onHistoryLoaded(long hash, HistoryStore.LoadResult result)
	{
		if (hash != accountHash)
		{
			return;
		}

		history = result.getHistory();
		readOnly = result.isReadOnly();
		loading = false;
		if (readOnly)
		{
			log.warn("Maggot King history is read-only (newer format or unreadable); changes will not be saved");
		}

		Player player = client.getLocalPlayer();
		if (player != null && player.getName() != null)
		{
			history.setLastDisplayName(player.getName());
		}

		// Trips left open by a client exit: close all but the newest, which may resume within the grace period
		Trip open = null;
		for (Trip trip : history.getTrips())
		{
			if (!trip.isOpen())
			{
				continue;
			}
			if (trip.getSegmentStartedAt() != null)
			{
				long end = Math.max(trip.getSegmentStartedAt(), trip.getLastActiveAt());
				trip.setActiveMs(trip.getActiveMs() + end - trip.getSegmentStartedAt());
				trip.setSegmentStartedAt(null);
			}
			if (open != null)
			{
				open.setEndedAt(open.getLastActiveAt());
				open.setEndReason(TripEndReason.LOGOUT);
			}
			open = trip;
		}
		if (open != null)
		{
			currentTrip = open;
			suspendedAt = open.getLastActiveAt();
		}

		historyChanged();
		if (inLair)
		{
			enterLair(System.currentTimeMillis());
		}
		pushState();
	}

	private void requestSave()
	{
		if (history == null || readOnly || (saveFuture != null && !saveFuture.isDone()))
		{
			return;
		}
		try
		{
			saveFuture = executor.schedule(() -> clientThread.invokeLater(this::saveNow), SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException e)
		{
			// Plugin is shutting down
		}
	}

	private void saveNow()
	{
		if (history == null || readOnly)
		{
			return;
		}
		if (currentTrip != null && currentTrip.getSegmentStartedAt() != null)
		{
			currentTrip.setLastActiveAt(System.currentTimeMillis());
		}
		store.save(accountHash, gson.toJson(history));
	}

	private void historyChanged()
	{
		historyDirty = true;
		viewDirty = true;
	}

	private void pushState()
	{
		if (historyDirty && history != null)
		{
			List<TripView> views = new ArrayList<>();
			List<Trip> trips = history.getTrips();
			for (int i = trips.size() - 1; i >= 0; i--)
			{
				if (!trips.get(i).isOpen())
				{
					views.add(viewBuilder.trip(trips.get(i)));
				}
			}
			historyViews = Collections.unmodifiableList(views);
		}
		historyDirty = false;
		viewDirty = false;

		PanelState.Status status;
		TripView shown = null;
		if (history == null)
		{
			status = loading ? PanelState.Status.LOADING : PanelState.Status.LOGGED_OUT;
		}
		else if (currentTrip != null)
		{
			status = suspendedAt != null ? PanelState.Status.PAUSED : PanelState.Status.IN_TRIP;
			shown = viewBuilder.trip(currentTrip);
		}
		else
		{
			status = PanelState.Status.IDLE;
			shown = historyViews.isEmpty() ? null : historyViews.get(0);
		}

		PanelState state = new PanelState(
			status,
			shown,
			historyViews,
			history == null ? null : viewBuilder.lifetime(history, allTime(), config.showCurrentValue(), System.currentTimeMillis()),
			history == null ? null : viewBuilder.goal(history, lastGoalTickAt > 0, System.currentTimeMillis()),
			readOnly);
		stateListener.accept(state);
	}

	// ---- Small records ----

	@AllArgsConstructor
	private static class Click
	{
		final int tick;
		final String option;
		final int itemId;
		boolean consumed;
	}

	private enum GroundKind
	{
		OWN_DROP,
		LOOT_OVERFLOW,
	}

	@AllArgsConstructor
	private static class GroundEntry
	{
		final int itemId;
		long quantity;
		final WorldPoint location;
		final GroundKind kind;
		final Kill kill;
		int tick;
	}

	private static class PendingPolish
	{
		final int tarnishedId;
		final int tick;
		final Set<Integer> eventItems = new LinkedHashSet<>();
		final Set<Integer> netGains = new LinkedHashSet<>();

		PendingPolish(int tarnishedId, int tick)
		{
			this.tarnishedId = tarnishedId;
			this.tick = tick;
		}
	}

	@AllArgsConstructor
	private static class PreEntryUse
	{
		final long at;
		final List<ItemEntry> items;
	}
}
