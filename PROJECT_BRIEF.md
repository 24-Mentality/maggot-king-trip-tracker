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
- No overlays on the game screen at all.

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
