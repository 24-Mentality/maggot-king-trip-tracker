# Maggot King Trip Tracker — Project Brief

My repo is already cloned in this folder (github.com/24-Mentality/maggot-king-trip-tracker),
created from runelite/example-plugin. Plugin name: Maggot King Trip Tracker.
Author: 24-Mentality. I use VS Code on macOS (Apple Silicon, macOS 13) with
Temurin Java 11. Give run and debug instructions for VS Code, never IntelliJ.
Git is already configured with my GitHub noreply email; never change git config.

Save this whole brief as PROJECT_BRIEF.md and add a line `@PROJECT_BRIEF.md`
to the existing CLAUDE.md so both it and AGENTS.md load every session.
Do not edit AGENTS.md. Where AGENTS.md and this brief disagree, AGENTS.md wins.

## Goal
A RuneLite Plugin Hub plugin that tracks loot, supplies used, and net profit
per trip at the Maggot King boss, with persistent per-account history and a
clean, native-looking side panel. I do all in-game testing; you write the code.
You must never control or interact with the game client yourself.

## References (patterns only, do not copy)
- https://github.com/camjewell11/Mokha-Loot-Tracker (Doom of Mokhaiotl
  tracker, BSD-2): per-account JSON persistence, supply tracking across
  inventory/equipment/rune pouch/quiver, potion dose normalization. Reuse
  ideas, not code. If any non-trivial code is reused, keep its copyright notice.
- https://github.com/randytkrx/maggot-king (existing Maggot King plugin with
  session stats and highlights). We must not duplicate it; our focus is
  per-trip loot, supplies, profit and lifetime history.

## Boss facts (verify IDs against net.runelite.api.gameval constants)
- Boss NPC 15742, corpse NPC 15741. Solo instanced boss in Vampyrium.
- On death the corpse offers "Open-stomach" (normal drop table) or
  "Take-eggs" (pet-focused). Boss respawns after corpse interaction.
  Multiple kills per trip are normal.
- Loot goes directly into inventory; overflow drops to the ground in the arena.
- Uniques: Elder venator fang 1/340, Crimson kisten 1/520 (any unique 1/205.6).
  Pet (Maggot marquess) 1/3,500 on Open-stomach.
- Take-eggs yields nothing, one of 6 maggot egg tiers, or a stymphike tartare
  + dull ancient medal supply drop. Egg pet rates when popped: base 1/3000,
  sickly 1/2500, warm 1/1250, pulsating 1/500, wriggling 1/10, writhing 1/2.
  Overall pet chance per egg-take 1/1,502.4.
- Tertiary: elite clue 1/40 (1/38 with elite CA tier), brimstone key 1/50 on
  Konar tasks.
- Death: respawn at the Vampyrium portal, gravestone outside the lair.
  Reclamation fee reduced 75% until 5 KC. The aranei scout can move the grave
  for 50,000 coins, 1 vial of blood, or 150 stymphike feathers.

## Loot detection
- The core RuneLite Loot Tracker DOES record Maggot King kills. Subscribe to
  net.runelite.client.plugins.loottracker.LootReceived filtered to the boss as
  the primary loot source. Fallback: snapshot inventory on the corpse click and
  diff on the following ItemContainerChanged, plus ItemSpawned ground items in
  the arena for overflow. Never count supplies consumed in that window as loot.
  README must state the core Loot Tracker has to be enabled.
- Pet messages are generic to all pets; only attribute them right after a
  corpse interaction or an egg Pop click:
  "You have a funny feeling like you're being followed." (follower)
  "You feel something weird sneaking into your backpack." (inventory)
  "You have a funny feeling like you would have been followed..." (Probita;
  unverified for this boss)
  Egg pop fail: "The egg pops and reveals a dead maggot."
  Track Pop clicks on all egg tiers anywhere, not just in the lair.
- Tarnished items (spear, ring, bracelet, necklace, amulet, battleaxe,
  longsword, halberd, 2h sword): record as PENDING at drop time. When the
  player clicks "Polish" on a tarnished item anywhere, the item that replaces
  it is the real drop. Resolve FIFO against the oldest pending entry of that
  type, value it at GE, and update that trip. Keep a local tally of polish
  outcomes per type. Never hardcode outcome lists.

## Core model
- Trip = entering the lair until leaving it (walk out, teleport, death, or
  logout after a configurable grace period). Config option to merge
  re-entries within N minutes into one trip. Use
  WorldPoint.fromLocalInstance(...) to get the template region ID, since the
  instance region changes every entry.
- Kill = confirmed by the kill-count chat message.
- Supplies = everything consumed while in the lair. My setup is magic + melee
  from the wiki Maggot King/Strategies page: rune pouch runes (wrath, air,
  fire or sunfire), anglerfish, sanfew serum, divine super combat, surge
  potion. Later phase: Tome of fire pages (burnt vs searing, configurable),
  Eye of Ayak, Scythe of Vitur, and Webweaver bow charges.
- Costs also include death fees and aranei scout grave moves.
- Values from ItemManager GE prices, per-dose for potions.

## Features by phase
Phase 0 (FIRST SESSION ONLY, then stop): plan, template renaming, VS Code
config, diagnostic mode (details below).
Phase 1: trips, kills, per-kill and per-trip loot, supplies, net profit,
GP/hr, deaths and death costs, stomach vs eggs split, persistent per-account
JSON history under RuneLite.RUNELITE_DIR with a schema version field, clear
data controls.
Phase 2: dryness card (kills since last unique, per-unique KC, expected vs
actual, egg pet-chance tracker), loot alerts, CSV export of trip history,
account-scoped JSON export/import, tarnished polish tracking.
Phase 3: charged weapon and Tome page costs, historical edit mode.

## UI
- Native RuneLite look: ColorScheme, FontManager, PluginPanel,
  MaterialTabGroup. Tabs: Current Trip | History | Lifetime.
- Current Trip header card with large numbers: trip timer, kills, loot value,
  supply cost, net profit (green/red), GP/hr, average kill time.
- Loot and supplies as item-icon grids with quantity and value tooltips.
  Uniques get a gold border; pending tarnished items look distinct.
- History: one compact card per trip, expandable to its item grid.
- Lifetime: totals, profit-per-trip trend drawn with plain Swing, dryness card.
- No overlays on the game screen, except the optional goal / trip-time
  overlays (decision 13).

## Compliance
Loot, supply and profit tracking only. No boss mechanic aids of any kind: no
attack or prayer cues, hazard or tile marking, phase indicators, larvae
callouts, or anything drawn during the fight. The plugin only listens to game
events and never creates input or menu actions. The README must open with a
clear statement that it contains no combat or mechanic assistance, because
RuneLite does not accept new high-end PvM helper plugins.

## Phase 0 details
1. Read the template, AGENTS.md, and both reference repos. Reply with a
   package/class plan, the JSON schema, and your open questions.
2. Rename everything from the template per AGENTS.md (package, classes,
   config group "maggotkingtriptracker" or similar, build.gradle group,
   settings.gradle, runelite-plugin.properties with author 24-Mentality).
3. Make ./gradlew run start the client with --developer-mode and -ea, and
   create .vscode/launch.json and .vscode/settings.json for Java 11 at
   /Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home.
4. Diagnostic mode: a config toggle, off by default. While in the lair, or
   within 10 ticks of any corpse, egg, or tarnished-item click, append to
   .runelite/maggot-king-trip-tracker/diagnostic.log: every ChatMessage (type
   and raw text including color tags), MenuOptionClicked (option, target,
   ids), the template region ID, ItemContainerChanged diffs for inventory and
   equipment, ItemSpawned in the arena, and LootReceived events. Write the
   file off the client thread.
5. Run ./gradlew build until it passes, commit with a clear message, and push.
6. Then STOP and tell me exactly how to launch, where the diagnostic setting
   is, what to do on my test trip, and where the log file will be.

## Decisions (answers to Phase 0 open questions)
1. Prices: store the GE price recorded at the time of the drop/use. The
   Lifetime tab may show today's value as an optional extra.
2. Logout grace period: 5 minutes. Merging re-entries: off by default
   (one trip = one inventory); when turned on, N defaults to 5 minutes.
3. Pre-potting counts. Config option "Count supplies used before entry",
   on by default, covering the 60 seconds before entering the lair.
4. Dropped items: ignore zero-value junk (empty vials etc.). If a real
   supply is dropped (e.g. food to make room for loot) and not picked back
   up before leaving, record it as a "Dropped" cost line on the trip.
5. Never hardcode item display names; get them from ItemManager at runtime.
   (The medal's in-game name is "Dull ancient medal"; gameval constant is
   DULL_ZAROSIAN_MEDAL.)
6. Testing uses a non-Jagex account that logs in directly; skip the Jagex
   Accounts login step. The user runs ./gradlew run themselves.
7. File IO goes through Filepath (AGENTS.md), so plugin files live in
   ~/.runelite/plugin-data/maggot-king-trip-tracker/.
8. Charge costs (blood fury, Tome of fire, Webweaver bow) were moved ahead of
   Phase 2 at the user's request (2026-09-27). The CHARGES_*_QUANTITY varbits
   turned out not to update, so charges are counted from attacks (see
   ChargeCounter) and only in the lair. Tome pages default to searing. Eye of
   Ayak and Scythe stay in Phase 3.
9. Phase 2 (2026-09-27): dryness counts Open-stomach kills only, since uniques
   and the kill pet only come from Open-stomach. Egg pops are detected as an
   egg leaving the inventory right after any click on it except drop / use /
   examine / destroy / banking, because the Pop option name is unconfirmed.
   Imports merge: only trips not already present are added, polish tallies
   take the larger count.
10. Trip timing (2026-09-28, Milestone A feedback; replaces "trip = entering
    the lair until leaving it"):
    - The trip clock and the goal clock (KPH/TTG) only run while fighting in
      the lair. After 30 s in the lair without dealing a hitsplat (configurable),
      both pause retroactively to the last hit and restart on the next hit.
      The goal clock never runs just because you're logged in.
    - Walking out to the region just outside the lair (10618) keeps the trip
      open and paused for 5 minutes (configurable). Re-entering continues it;
      leaving that region or the grace period expiring ends it at the moment
      you left the lair. Teleports and deaths still end the trip immediately.
      Supplies used while waiting outside count toward the open trip.
    - The Pause button stays for manual pauses (in the lair on a trip only).
11. Trips that end with no kills, no deaths and no dropped items are
    discarded (e.g. walking in and straight back out). Existing ones in the
    saved history are left alone.

12. Milestone B (2026-09-28): the dropdown is shown even with one boss, each
    option with the boss's icon. A dry streak from an entered "KC of my last
    unique" counts the kills between that KC and the start of tracking by kill
    count (Take-eggs kills included, since they can't be told apart), then
    tracked Open-stomach kills; a newer tracked unique wins. Export file names
    carry a date and time stamp; CSV is per boss, JSON covers every boss.

13. Overlay (2026-09-28, replaces "No overlays on the game screen at all"):
    one optional box, off by default, styled like RuneLite's XP tracker box:
    the boss icon, then three rows picked from dropdowns (goal: KPH / TTG /
    kills done / kills left; trip: current kill / trip time / kills / average
    kill / PB; loot: net profit / net GP/hr; each can be Nothing), and an
    optional goal progress bar (kills done, %, goal). "Only during a trip"
    is on by default. Nothing about the boss or its mechanics is ever drawn.

## Observed in-game (diagnostic test trips, 2026-09-27)
These come from diagnostic.log and take precedence over the assumptions above.
- Lair template region is always 11645; the real instance region changes per
  entry. Walking out via "Exit" on Darkwood trees (object 61049) lands in
  region 10618 (just outside the lair). Death respawn lands in region 10106.
- Kill-count message (GAMEMESSAGE), same tick as boss despawn / corpse spawn:
  `Your Maggot King kill count is: <col=ff0000>2,565</col>.` followed by
  `Fight duration: <col=ff0000>1:48.00</col>. Personal best: 1:19.20`.
  Use the game's fight duration for kill time.
- Corpse options: "Open-stomach" = NPC_FIRST_OPTION, "Take-eggs" =
  NPC_THIRD_OPTION.
- LootReceived (name "Maggot King", type NPC) fires about 2 ticks after
  Open-stomach, but it only contains items that went into the inventory.
  With a full inventory, overflow (e.g. 3 x Stymphike tartare) spawned on the
  ground and was NOT in LootReceived. Always capture ground spawns
  (ItemSpawned, ownership=1) after a corpse click as loot, not only as a
  fallback.
- The stymphike tartare + dull ancient medal supply drop came from
  Open-stomach, not Take-eggs. Treat it as possible from either option.
- Take-eggs with no result: chat "The eggs pop as you try to take them." and
  LootReceived name "Maggot King", type UNKNOWN, empty items.
- Polish immediately fires LootReceived type EVENT named after the tarnished
  item, containing the result (Tarnished spear -> Adamant spear, Tarnished
  necklace -> Jade necklace). Chat: "You rub the tarnished <item> on your
  clothes and are surprised to find a shine underneath the grime."
- The player's own drops also spawn with ownership=1; the only distinguishing
  signal is a "Drop" menu click on that item in the same tick.
- Rune pouch: only RUNE_POUCH_QUANTITY_n varbits change during casting; the
  TYPE varbits don't fire, so read rune types directly at trip start.
- Gear switches are frequent (magic/melee); combined inventory + equipment
  snapshots net them out to zero as intended.
- Tome of Fire, Webweaver bow and Amulet of blood fury charges are not
  visible in item containers (Phase 3 charge tracking).
- Polishing a Dull ancient medal destroys it ("The medal releases a trace of
  ancient magic as it falls apart."); it is not a supply.
- The pre-entry, rune pouch, overflow pickup, own-drop re-pickup and logout
  grace logic matched the diagnostic log on the first Phase 1 test trip.
- Casting a spell on an item (e.g. High Level Alchemy) is a MENU click with
  option "Cast", action WIDGET_TARGET_ON_WIDGET and itemId = the target item.
  The target item is converted, not a supply; the spell's runes still are.
- The CHARGES_*_QUANTITY varbits (blood fury, Tome of fire, wilderness weapon)
  never changed during a kill, even after "Check"; charges must be counted
  from attacks instead.
- The Loot Tracker's polish EVENT can include unrelated inventory changes from
  the same tick (a Prayer potion(2) from a sip appeared alongside the Rune
  halberd). Phase 2 polish resolution must take only the replacement item.
- Charge counting validated against in-game Check messages on one kill
  (blood fury 2,113 -> 2,096, bow 591 -> 587, tome 6,590 -> 6,553):
  Tome = FIRESURGE_CASTING spotanim on the player with the tome worn (37);
  bow = WILD_CAVE_BOW_ARROW_LAUNCH02 spotanim (4); blood fury = DAMAGE_ME /
  DAMAGE_MAX_ME hitsplats with damage > 0 while the amulet is worn and the
  last attack was melee (17). Each elder maul attack produced 2-3 hitsplats
  here, and each used a charge. Gear must be read at the end of the tick:
  switches can arrive after the attack animation in the same tick. The log
  is kept as a test fixture (charge-test-kill.log).
- Animation 420 is HUMAN_STAFFORB_BLOCK (a block, not an attack); hitsplat 43
  is DAMAGE_MAX_ME.
- The Loot Tracker's polish EVENT lists every inventory change in that tick:
  polishing a Tarnished necklace while equipping the Inquisitor's great helm
  reported [Inquisitor's great helm, Diamond necklace]. The polish result must
  be a net gain across inventory + equipment (gear swaps net to zero).
- Empty vials have a 2 gp GE price; dropped items under 100 gp each are junk.
- Deaths (2026-09-27, KC 2,633 and 2,636): items vanish in the respawn tick
  (region 10106). The Aranei scout's "Retrieve-gravestone" option (NPC 15750)
  cost nothing ("The scout collects your gravestone and leaves it nearby.").
  No coins left the inventory, so looting your own gravestone was free; a
  reclaim fee would only apply at Death after the grave expires.
- RuneLite core plugins keep all-time records in the RS profile config: Loot
  Tracker `loottracker` / `drops_NPC_Maggot King` = {kills, first, drops:[id,
  qty, ...]} (2,630 kills since 2026-07-30: 6 fangs, 5 kistens, no pet) and
  Chat Commands `killcount` / `maggot king` = 2636. The drop chances and luck
  cards read these through ConfigManager for all-time expected vs received.
- Milestone A confirmed in game (2026-09-28): profit card order, Net vs Loot
  GP/hr labels, Pause / Resume / auto-resume, luck tiers ("Luck Status:") and
  sidebar fit all passed. Timing check on a 26-kill trip: 58.1 min of fighting
  time recorded against 67.7 min wall-clock (idle and outside time left out).
  Walking out to region 10618 and back 2.5 min later, and logging out outside
  and back, both continued the same trip; leaving 10618 (to region 14642)
  ended it at the moment of walking out.
- Entering and immediately leaving the lair creates a trip with 0 kills; it is
  kept (open, then ended by the grace rules) and shows in History.
- Milestone B confirmed in game (2026-09-28): the v1 history migrated to
  schema 2 on first login with a v1 backup written next to it; all 23
  finished trips, the goal and the polish tallies carried over unchanged, and
  the Lifetime tab and Luck card (6 fangs / 5 kistens all-time) matched the
  numbers from before. The boss dropdown (icon, live-trip dot), a normal trip
  (kills, loot, supplies, Stom / Eggs, pause, walk out and back, teleport),
  the right-click "KC of my last unique" (set and clear) and timestamped
  CSV / JSON exports and re-import all passed. The last unique before
  tracking was at KC 1,920, entered through the Luck card.
- The game's "Fight duration" is exactly the ticks from the boss spawning
  (NpcSpawned) to the kill-count message, on every kill in the diagnostic
  logs; the live kill timer counts from the spawn.
- The player's name is not available when the history loads at LOGGED_IN;
  it is picked up on the first game tick where the local player has one.
- Milestone C confirmed in game (2026-09-28): the share card copies to the
  clipboard, saves to screenshots/<name>/Boss Trip Tracker/ via ImageCapture,
  confirms in chat, pastes readably into Discord, and shows or hides the name
  per the setting. The luck tier on the card uses the all-time Loot Tracker
  record (uniques received vs expected), separate from the dry streak. The
  trip row's live kill timer (Current / Last) and PB (this trip's fastest)
  passed.
