package com.maggotkingtriptracker.diagnostic;

import com.google.common.collect.ImmutableSet;
import com.maggotkingtriptracker.MaggotKingIds;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.util.Filepath;

/**
 * Records raw game events around the Maggot King to diagnostic.log so detection logic can be built
 * from real data. Only listens to events; it never draws anything or creates input.
 * <p>
 * Recording is active while diagnostic mode is enabled and the player is in the lair or the region just
 * outside it, or for {@link #CLICK_WINDOW_TICKS} ticks after clicking the corpse, a maggot egg, a tarnished
 * item, or the aranei scout that handles death recovery.
 */
public class DiagnosticRecorder
{
	private static final int CLICK_WINDOW_TICKS = 10;

	private static final Set<Integer> RUNE_POUCH_VARBITS = ImmutableSet.of(
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6,
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
	);

	private final Client client;
	private final ItemManager itemManager;
	private final DiagnosticLogWriter writer;

	private boolean enabled;
	private boolean inLair;
	private boolean nearLair;
	private int templateRegionId = -1;
	private int clickWindowEndTick = -1;
	private final Map<Integer, Map<Integer, Integer>> containerSnapshots = new HashMap<>();

	public DiagnosticRecorder(Client client, ItemManager itemManager, Callable<Filepath> directorySupplier)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.writer = new DiagnosticLogWriter(directorySupplier);
	}

	public void shutDown()
	{
		if (enabled)
		{
			record("SESSION", "diagnostic recording stopped (plugin shut down)");
		}
		writer.shutDown();
	}

	public void setEnabled(boolean enabled)
	{
		if (this.enabled == enabled)
		{
			return;
		}

		if (!enabled)
		{
			record("SESSION", "diagnostic mode disabled");
		}
		this.enabled = enabled;
		if (enabled)
		{
			record("SESSION", "diagnostic mode enabled; lair template region " + MaggotKingIds.LAIR_REGION_ID);
			if (inLair)
			{
				recordContainerSnapshots();
			}
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

		LocalPoint localPoint = player.getLocalLocation();
		int region = WorldPoint.fromLocalInstance(client, localPoint).getRegionID();
		if (region != templateRegionId)
		{
			int previous = templateRegionId;
			templateRegionId = region;
			if (isRecording() || region == MaggotKingIds.LAIR_REGION_ID)
			{
				record("REGION", "template region " + previous + " -> " + region
					+ " instance=" + client.getTopLevelWorldView().isInstance()
					+ " actual region=" + WorldPoint.fromLocal(client, localPoint).getRegionID());
			}
		}

		boolean nowNearLair = region == MaggotKingIds.LAIR_ENTRANCE_REGION_ID;
		if (nowNearLair != nearLair)
		{
			nearLair = nowNearLair;
			record("ENTRANCE", nearLair ? "entered region outside the lair" : "left region outside the lair");
		}

		boolean nowInLair = region == MaggotKingIds.LAIR_REGION_ID;
		if (nowInLair != inLair)
		{
			inLair = nowInLair;
			record("LAIR", inLair ? "entered" : "left");
			if (inLair)
			{
				recordContainerSnapshots();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (isRecording())
		{
			record("GAMESTATE", state.name());
		}

		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			if (inLair)
			{
				record("LAIR", "left (" + state.name() + ")");
			}
			inLair = false;
			nearLair = false;
			templateRegionId = -1;
			clickWindowEndTick = -1;
			containerSnapshots.clear();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		MenuEntry entry = event.getMenuEntry();
		NPC npc = entry.getNpc();
		int itemId = event.getItemId();

		boolean trigger = (npc != null && (npc.getId() == MaggotKingIds.CORPSE
				|| MaggotKingIds.ARANEI_DEATH_HELPERS.contains(npc.getId())))
			|| MaggotKingIds.EGGS.contains(itemId)
			|| MaggotKingIds.TARNISHED_ITEMS.contains(itemId);
		if (trigger)
		{
			clickWindowEndTick = client.getTickCount() + CLICK_WINDOW_TICKS;
		}

		if (!isRecording())
		{
			return;
		}

		StringBuilder sb = new StringBuilder()
			.append("option=\"").append(event.getMenuOption()).append('"')
			.append(" target=\"").append(event.getMenuTarget()).append('"')
			.append(" action=").append(event.getMenuAction())
			.append(" id=").append(event.getId())
			.append(" itemId=").append(itemId)
			.append(" itemOp=").append(event.getItemOp())
			.append(" param0=").append(event.getParam0())
			.append(" param1=").append(event.getParam1());
		if (npc != null)
		{
			sb.append(" npcId=").append(npc.getId()).append(" npcName=\"").append(npc.getName()).append('"');
		}
		if (trigger)
		{
			sb.append(" [opens ").append(CLICK_WINDOW_TICKS).append("-tick window]");
		}
		record("MENU", sb.toString());
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!isRecording())
		{
			return;
		}

		record("CHAT", "type=" + event.getType()
			+ " name=\"" + event.getName() + '"'
			+ " sender=\"" + event.getSender() + '"'
			+ " message=\"" + event.getMessage() + '"');
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId != InventoryID.INV && containerId != InventoryID.WORN)
		{
			return;
		}

		Map<Integer, Integer> current = toQuantities(event.getItemContainer());
		Map<Integer, Integer> previous = containerSnapshots.put(containerId, current);
		if (!isRecording() || previous == null)
		{
			return;
		}

		String diff = describeDiff(previous, current);
		if (!diff.isEmpty())
		{
			record(containerName(containerId), diff);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (!isRecording() || !RUNE_POUCH_VARBITS.contains(event.getVarbitId()))
		{
			return;
		}

		record("RUNEPOUCH", "varbit=" + event.getVarbitId() + " value=" + event.getValue());
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		recordGroundItem("spawned", event.getItem(), event.getTile().getLocalLocation());
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		recordGroundItem("despawned", event.getItem(), event.getTile().getLocalLocation());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		recordEncounterNpc("spawned", event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		recordEncounterNpc("despawned", event.getNpc());
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!isRecording())
		{
			return;
		}

		Actor actor = event.getActor();
		if (actor == client.getLocalPlayer())
		{
			record("DEATH", "local player died");
		}
		else if (actor instanceof NPC && ((NPC) actor).getId() == MaggotKingIds.BOSS)
		{
			record("DEATH", "boss died");
		}
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		if (!isRecording())
		{
			return;
		}

		StringBuilder sb = new StringBuilder()
			.append("name=\"").append(event.getName()).append('"')
			.append(" type=").append(event.getType())
			.append(" combatLevel=").append(event.getCombatLevel())
			.append(" amount=").append(event.getAmount())
			.append(" items=[");
		boolean first = true;
		for (ItemStack stack : event.getItems())
		{
			if (!first)
			{
				sb.append(", ");
			}
			first = false;
			sb.append(describeItem(stack.getId(), stack.getQuantity()));
		}
		sb.append(']');
		record("LOOT", sb.toString());
	}

	private void recordGroundItem(String what, TileItem item, LocalPoint localPoint)
	{
		if (!enabled || !(inLair || nearLair))
		{
			return;
		}

		WorldPoint location = WorldPoint.fromLocalInstance(client, localPoint);
		record("GROUND", what + " " + describeItem(item.getId(), item.getQuantity())
			+ " ownership=" + item.getOwnership()
			+ " at=" + location.getX() + "," + location.getY() + "," + location.getPlane());
	}

	private void recordEncounterNpc(String what, NPC npc)
	{
		int id = npc.getId();
		if (isRecording() && (id == MaggotKingIds.BOSS || id == MaggotKingIds.CORPSE))
		{
			record("NPC", what + " id=" + id + " name=\"" + npc.getName() + "\" index=" + npc.getIndex());
		}
	}

	private boolean isRecording()
	{
		return enabled && (inLair || nearLair || client.getTickCount() <= clickWindowEndTick);
	}

	private void recordContainerSnapshots()
	{
		if (!enabled)
		{
			return;
		}

		for (int containerId : new int[]{InventoryID.INV, InventoryID.WORN})
		{
			ItemContainer container = client.getItemContainer(containerId);
			Map<Integer, Integer> quantities = toQuantities(container);
			containerSnapshots.put(containerId, quantities);
			StringBuilder sb = new StringBuilder("snapshot [");
			boolean first = true;
			for (Map.Entry<Integer, Integer> e : quantities.entrySet())
			{
				if (!first)
				{
					sb.append(", ");
				}
				first = false;
				sb.append(describeItem(e.getKey(), e.getValue()));
			}
			record(containerName(containerId), sb.append(']').toString());
		}
	}

	private void record(String category, String details)
	{
		if (!enabled)
		{
			return;
		}

		String line = "tick=" + client.getTickCount()
			+ " region=" + templateRegionId
			+ " inLair=" + inLair
			+ ' ' + category + ' ' + details;
		writer.append(line.replace("\r", "\\r").replace("\n", "\\n"));
	}

	private String describeDiff(Map<Integer, Integer> previous, Map<Integer, Integer> current)
	{
		Set<Integer> ids = new TreeSet<>(previous.keySet());
		ids.addAll(current.keySet());

		StringBuilder sb = new StringBuilder();
		for (int id : ids)
		{
			int delta = current.getOrDefault(id, 0) - previous.getOrDefault(id, 0);
			if (delta == 0)
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(delta > 0 ? '+' : '-').append(describeItem(id, Math.abs(delta)));
		}
		return sb.toString();
	}

	private String describeItem(int itemId, int quantity)
	{
		return itemManager.getItemComposition(itemId).getName() + " (" + itemId + ") x" + quantity;
	}

	private static Map<Integer, Integer> toQuantities(ItemContainer container)
	{
		Map<Integer, Integer> quantities = new HashMap<>();
		if (container == null)
		{
			return quantities;
		}

		for (Item item : container.getItems())
		{
			if (item.getId() >= 0)
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return quantities;
	}

	private static String containerName(int containerId)
	{
		return containerId == InventoryID.INV ? "INVENTORY" : "EQUIPMENT";
	}
}
