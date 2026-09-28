# Roadmap: multi-boss expansion

This file extends PROJECT_BRIEF.md with the plan for tracking more bosses and
the next UI changes. For anything covered here, this file wins over the brief;
AGENTS.md still wins over both. Everything in the brief's "Decisions" and
"Observed in-game" sections carries over to every boss unless a boss section
below says otherwise (recorded prices, 5-minute logout grace, pre-potting
window, dropped-item rules, Filepath, charges counted from attacks, dryness
from loot kills only).

## How we work (every milestone)

- One milestone per session. Start by reading this file and the code you will
  touch.
- Milestones marked **Plan first**: reply with your plan (classes and files to
  change, data migrations, open questions) and wait for my OK before editing.
- Keep `./gradlew build` passing. Add unit tests for all pure logic: odds and
  luck math, migrations, chat parsing, supply accounting. Commit per logical
  change and push once the build passes.
- Never lose my history. Any change to the saved JSON needs a schema version
  bump, an automatic migration, a backup of the old file before it is
  rewritten, and a test that loads an old-format file.
- Don't hardcode chat text, IDs or region numbers that haven't been verified.
  Use gameval constants, take message text from my diagnostic logs, and mark
  anything unverified in a code comment and in your summary.
- Finish by telling me exactly what to test in-game (the normal path and the
  edge cases), then stop. A milestone is only done when I confirm it in-game.
  Afterwards, add what we learned to "Observed in-game" in PROJECT_BRIEF.md.

## Compliance (every boss)

Loot, supplies, costs, deaths, time and luck only. For every boss and raid:
nothing drawn on the game screen, no alerts or cues tied to boss mechanics, no
attack, prayer, phase or hazard information, and no menu changes. The plugin
only listens to game events. Don't store or show other players' names: for a
teammate's drop, record only that a teammate received the item. The README
must keep opening with the no-combat-assistance statement, covering every
supported boss.

## Milestone A: UI polish (Maggot King only, no data model changes)

1. **Stat order.** In the profit card (TripSummaryCard), order the cells left
   to right: row 1 Net profit, GP/hr, Stom / Eggs; row 2 Loot, Costs, Deaths.
2. **Two different GP/hr numbers.** The profit card's GP/hr is net per hour,
   but the Loot box's "GP/Hr" (TripDetails) is loot per hour. Label them
   "Net GP/hr" and "Loot GP/hr" (or equivalent) so they can't be confused.
3. **Pause button.** Add Pause between Set goal and Reset in the goal card,
   with all three buttons the same width.
   - Pausing stops the current trip's clock and the goal clock, so Time,
     GP/hr, KPH and TTG leave out AFK time. With no trip running, it pauses
     just the goal clock. The button then reads Resume.
   - Show a clear paused state, e.g. status "Trip paused (AFK)" and a greyed
     timer. This is separate from the existing "Trip paused (logged out)"
     state; keep both working.
   - Resume manually, or automatically when I next deal damage to the boss
     (config "Auto-resume when I attack", on by default; use my own hitsplats
     on the boss).
   - Kills, loot and supplies during a pause still count; only time is
     excluded. Store paused time on the trip so History shows active time.
     Ending a trip clears the pause.
4. **Luck tiers.** Replace Lucky / On rate / Dry with five tiers from the
   existing `DropOdds.luckPercentile`, using these exact strings:

   | Percentile   | Tier          | Colour     |
   |--------------|---------------|------------|
   | 90% and up   | LUCKY AS RUCK | gold       |
   | 65% to 90%   | Lucky         | green      |
   | 35% to 65%   | On Rate       | light grey |
   | 10% to 35%   | Dry           | orange     |
   | 10% and down | DRY AS RUCK   | red        |

   Make the boundaries symmetric, keep the thresholds in one place, and unit
   test each boundary. Show the tier as a coloured label in the Luck card's
   title row, next to "Luck", so it stays visible when the card is collapsed
   with the eye icon. Remove the separate "Luck" line from the card body.
   Update the tooltip to explain the tiers. No tier when there are no kills.
5. **Fit at sidebar width.** At the default panel width, the Luck progress
   label is cut off ("61% of dro...") and the "50% / 90%" row wraps onto two
   lines. Make every label fit without ellipses or wrapping, including large
   values like "-12.4M" and "KC 12,345", on all three tabs.

Done when I've checked all five on a Maggot King trip.

## Milestone B: multi-boss foundation and boss selector (Plan first)

Restructure so each boss is a definition, with Maggot King as the only enabled
boss. Maggot King must look and behave exactly as before, and my history must
survive.

1. **Boss definitions.** Move everything Maggot King-specific (MaggotKingIds,
   MaggotKingRates, chat patterns, corpse choice, eggs, tarnished items, the
   aranei scout, the Stom / Eggs cell, the panel icon, the empty-state text)
   behind a per-boss definition that covers at least:
   - id, display name, icon item, and variants (e.g. ToB Entry/Normal/Hard)
   - detection: template region IDs, NPC IDs, and how a trip starts and ends
   - trip model: kills inside an instance (Maggot King, Nightmare) or one raid
     per trip (ToB)
   - kill or completion detection (chat patterns from diagnostic logs)
   - loot sources (Loot Tracker names and types, ground spawns, chests)
   - uniques with the player's chance per kill as a function, since ToB and
     the regular Nightmare depend on mode and team size; plus pets and
     tertiaries for the Expected / Received card
   - death and item-reclaim cost rules
   - supply rules, including items acquired inside that are free (ToB)
   - the boss-specific third cell in the profit card
   - all-time sources: the core Loot Tracker record key and the Chat Commands
     kill-count key(s), as AllTimeRecords already reads for Maggot King

   Adding another boss later should mostly mean a new definition and its
   tests.
2. **Data.** Schema v2 keeps trips, goals, egg pops, polish tallies and any
   other per-boss data under a boss id. Migrate v1 files automatically to v2
   under the Maggot King id, backing up the v1 file first. Export and import
   must accept both versions.
3. **Boss selector.** A dropdown at the very top of the panel, above the Trip,
   History and Lifetime tabs, with each boss's icon and name. It controls what
   all three tabs show.
   - Entering a tracked boss's area selects that boss and opens the Trip tab
     (the existing "open panel on entry" setting). Show a small live dot next
     to the boss with a trip in progress.
   - Anywhere else, the dropdown keeps my last choice and lets me browse any
     boss's trips, lifetime stats and luck. A live trip keeps tracking in the
     background while I browse another boss.
   - Bosses with variants get a chip row under the dropdown: Theatre of Blood
     [All | Normal | Hard] and Nightmare [All | Phosani's | Regular]. "All"
     uses each kill's own rate for luck.
   - Kill goals are per boss.
   - Only show bosses that are finished; at the end of this milestone that's
     just Maggot King.
4. **Dry streak start.** Per boss, an optional "KC of my last unique" (e.g. by
   right-clicking the Luck card) for uniques from before tracking began.
   Without it, the dry streak counts from when tracking began, as now.
5. **Name.** Change the display name to "Boss Trip Tracker", with a matching
   description, in the PluginDescriptor and runelite-plugin.properties. Keep
   internalName, the config group, the package and the data folder unchanged
   so saved settings and history aren't affected. Add tags for each boss as it
   ships.

Done when Maggot King looks and works exactly as before, my existing history
and all-time numbers survive the migration, and tests cover it.

## Milestone C: share card

A camera button in the panel header, next to the boss dropdown.

- It renders a dedicated share image for the selected boss and variant, not a
  raw screenshot of the panel: boss icon and name, my RSN (config "Show my
  name on share cards", on by default), KC, uniques received vs expected with
  the luck tier, current dry streak, the count of each unique, lifetime loot,
  costs, net profit and GP/hr, and my last 5 trips (date, kills, net).
- It copies the image to the clipboard, ready to paste into Discord, saves a
  PNG where AGENTS.md allows (RuneLite's screenshot utilities if permitted,
  otherwise the plugin's data folder), and confirms with a game chat message.
  Nothing is uploaded anywhere.
- Match the panel's look, stay readable when Discord shrinks the image, and
  keep it under about 800 px wide.

Done when I can paste a Maggot King card into Discord.

## Milestone D: Nightmare, Phosani's and Regular (Plan first)

**Step 1, then stop:** add a "Log everywhere" developer option so diagnostic
mode records before regions are known, and have it list the core Loot Tracker
and Chat Commands record keys it finds for these bosses. Then tell me what to
do on my diagnostic runs. Suggested runs:
- Phosani's: one trip of 2 to 3 kills. Drink a pre-pot just before entering,
  use normal supplies, and leave by teleport. If I die, I pay Sister Senga as
  normal.
- Regular: one solo kill, plus a group kill with clanmates if I can arrange
  one (to see party size, MVP and how loot is split).

**Step 2, after I send the logs:** implement both as variants of one Nightmare
boss.

Facts from the OSRS Wiki (verify, and prefer the wiki where it differs):

- **Phosani's Nightmare.** Solo, entered by drinking from the Pool of
  Nightmares in the Sisterhood Sanctuary. Several kills per trip are normal;
  a trip runs from entering the dream until leaving it. On death, I respawn by
  the Pool of Nightmares and reclaim items from Sister Senga for 60,000 coins
  (record the fee when paid; an unsafe death deletes the items).
  - Uniques per kill: Inquisitor's/staff table 1/140, orb table about 1/533,
    any unique about 1/111.
  - Per item: Nightmare staff 69/35,000; each Inquisitor's armour piece 1/700;
    Inquisitor's mace 31/35,000; each orb 1/1,600.
  - Tertiary: Little Nightmare 1/1,400, Jar of Dreams 1/4,000, Parasitic egg
    1/200, elite clue 1/35. Slepey tablet is 1/25 and guaranteed by 25 KC, so
    it doesn't count toward luck.
- **The Nightmare (regular).** A group boss that can also be done solo. On
  death, items are reclaimed from Shura for 60,000 coins. Players must reach a
  damage threshold for loot; the MVP gets big bones and 10% more common loot.
  - Team unique rolls per kill: Inquisitor's/staff table 1/83.33 and orb table
    1/320 (any about 1/66.67), plus a second independent roll with chance
    (party size - 5)%, clamped to 0-75%. A rolled unique goes to one player,
    weighted by damage.
  - Per item at base rate: staff 1/300, each armour piece 1/420, mace 1/750,
    each orb 1/960.
  - Pet: 1/800 solo, 1/1,600 for 2, 1/2,400 for 3, 1/3,200 for 4, 1/4,000 for
    5 or more (team size counted at the start of the fight). Jar of Dreams
    1/1,900 for the MVP and 1/2,000 otherwise; elite clue 1/190 and 1/200.
  - For luck, my chance per kill is the team chance divided by party size.
    That assumes equal damage; say so in the tooltip.
- **Supplies:** the same rules as Maggot King (pre-pots within 60 s of
  entering, inventory, equipment and rune pouch use, charges). Use the
  Phosani's strategies page to find commonly used gear with charges.
- **Third profit cell:** Uniques this trip.
- **All-time:** read the Loot Tracker and Chat Commands records for both
  bosses, as for Maggot King. For past regular kills, where party size is
  unknown, use a setting "Typical party size for past kills" (default 5).

Done when I've confirmed a Phosani's trip, and a regular kill, track
correctly.

## Milestone E: Theatre of Blood (Plan first)

**Step 1, then stop:** extend diagnostic mode to Ver Sinhaza and the Theatre
(every room, the supply chests, the vault, and the reward chest by the bank),
list the record keys it finds, and tell me what to do on my diagnostic raids.
Suggested runs:
- One Normal raid with my usual team: pre-pot in the lobby just before
  entering, buy at least one item from a supply chest, drop the salve amulet
  after Bloat as usual, and claim loot at the vault chest.
- If convenient, once leave the loot unclaimed and claim it from the chest by
  the Ver Sinhaza bank instead.
- One Hard Mode raid if I'm running HM.
- If a wipe happens naturally, keep that log. Don't wipe on purpose.

Teammates' names will be in the logs; I'll replace them with PLAYER1 to
PLAYER4 consistently before sharing.

**Step 2, after I send the logs:** implement.

Facts from the OSRS Wiki (verify):

- **Trip = one raid**, from entering the Theatre until leaving it, or until the
  raid ends on a wipe. Detect the mode (Entry, Normal, Hard) per raid.
  Completion comes from the completion-count chat message (text from my logs).
- **Loot** is shown in my Monumental chest in Verzik's vault (the core Loot
  Tracker records it). Unclaimed rewards can be claimed from the chest outside
  the Ver Sinhaza bank and are lost on logout, so capture loot from either
  chest.
- **Uniques (purples).** The team's chance per deathless raid is 1/9.1 in
  Normal and 1/7.7 in Hard, whatever the team size; Entry mode has none. The
  item goes to one player, weighted by deaths and damage (the MVP is most
  likely). Deaths lower the chance.
  - Normal weights: Avernic defender hilt 8/19; Ghrazi rapier, Sanguinesti
    staff and each Justiciar piece 2/19; Scythe of Vitur 1/19.
  - Hard weights: hilt 7/18; rapier, staff and each Justiciar piece 2/18;
    scythe 1/18.
- **My purples and team purples.** Track both: mine from my own chest, the
  team's from the teammate drop broadcast (exact text from my logs). For luck,
  my chance per raid is the team chance divided by the team size at the start
  of the raid (assumes equal contribution and no deaths; say so in the
  tooltip). For past raids from the Loot Tracker record, use a setting
  "Typical team size for past raids" (default 4). Entry mode raids count for
  profit but not for luck.
- **Tertiary:** Lil' Zik 1/650 in Normal and 1/500 in Hard; elite clue 1/25
  Entry, 3/25 Normal, 3.5/25 Hard; Hard only: Holy ornament kit 1/100,
  Sanguine ornament kit 1/150, Sanguine dust 1/275. All of these scale down
  with personal performance. Common loot is 80% lower in Entry and 15% higher
  in Hard (another 15% under the target time), and deaths reduce it.
- **Supplies (what I paid for):** pre-pots in the 60 s before entering; potions,
  food, runes (rune pouch and divine rune pouch) and ammo (arrows, bolts,
  darts, black chinchompas, Dizana's quiver) that I brought in; and charges
  used, for example Toxic blowpipe scales and darts, Scythe of Vitur, crystal
  items (shards are untradeable: 0 gp, but show the count), Amulet of blood
  fury, Eye of Ayak, Sanguinesti staff, tridents, Tumeken's shadow, Toxic
  staff of the dead and Serpentine helm. Take the full list from the wiki's
  Theatre of Blood/Strategies page. Eye of Ayak and Scythe charges are still
  open from Phase 3; build them here if they aren't done yet (Maggot King and
  the Nightmare benefit too).
- **Never a cost:** anything obtained inside the raid, especially supply-chest
  purchases (bought with in-raid points after Bloat and Sotetseg). Per item
  type, only charge for use beyond what I acquired inside that raid, per dose
  for potions. A brew bought from the chest costs nothing, whether or not I
  drink it.
- **Drops:** dropping gear mid-raid is normal (salve amulet after Bloat, a book
  after Maiden). Dropped equipment is not a cost; dropped consumables count as
  used.
- **Deaths:** dying in a room costs nothing (I rejoin when the team clears the
  room). A full team wipe ends the raid, and items are reclaimed from the
  chest for 100,000 coins. Record the fee when it's paid, or add 100,000 on a
  wipe if no payment is seen. Show my deaths per raid.
- **Third profit cell:** Purples, as mine / team.

Done when I've confirmed a Normal raid, and a Hard raid if I run them, track
correctly.
