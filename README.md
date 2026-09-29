# Boss Trip Tracker

> **No combat or mechanic assistance, for any supported boss.** This plugin only tracks loot,
> supplies, costs, deaths, time and luck. It has no attack, prayer, phase or hazard cues, no
> tile or NPC highlighting, and no alerts tied to boss mechanics. The only thing it can draw on
> the game screen is an optional box, off by default, with your kill goal progress, trip
> times and profit; it never shows anything about the boss or its mechanics. It only listens to game
> events and never creates input or menu actions.

Tracks loot, supplies used and net profit for each boss trip, with persistent per-account
history in a side panel. Formerly **Maggot King Trip Tracker**.

**Supported bosses:** the Maggot King in Vampyrium, and Phosani's Nightmare. The regular
Nightmare is next; its kills aren't tracked yet. More bosses are planned.

## Requirements

- The core RuneLite **Loot Tracker** plugin must be enabled. This plugin reads Maggot King
  loot from the Loot Tracker's loot events. If no loot event arrives, it falls back to
  inventory changes after you open the corpse.

## Side panel

- **Share card** (camera button next to the boss dropdown): makes an image of the shown
  boss's stats (kill count, uniques received vs expected with the luck tier, kills since
  your last unique and how far past the drop rate you are, each unique, the loot, costs, net
  profit and GP/hr of the kills tracked since the plugin was installed (with the kill count
  tracking began at), and your last 5 trips), copies it
  to the clipboard ready to paste into Discord, and saves it to your RuneLite screenshots
  folder under "Boss Trip Tracker". Nothing is uploaded. Your name is on the card unless you
  turn off **Show my name on share cards** (Configuration → Display).
- **Overlay** (off by default): a small box on the game screen built like RuneLite's XP
  tracker box, with the boss icon and up to three rows: a kill goal stat (KPH, TTG, kills
  done or left) with an optional progress bar, a trip time (current kill, trip time, kills,
  average kill, PB) and the trip's net profit or net GP/hr. Right-click the goal card, the
  trip time card or the profit card and choose **Add to canvas** (or **Remove from
  canvas**), or use Configuration → Overlay, where each row has a Show toggle and a dropdown.
  Hold Alt and drag the box to move it. By default it only shows during a trip.
- **Boss dropdown** (top of the panel): picks the boss shown in all three tabs. Entering a
  tracked boss's area selects it; otherwise your last choice is kept, so you can browse any
  boss's trips while a trip keeps tracking in the background. A green dot marks the boss with
  a trip in progress. Bosses with modes get a row of chips under the dropdown.
- **Kill goal** (top of the Trip tab): set a kill target and see kills per hour (logged-in
  time), kills done and left, time to goal, and a progress bar. **Reset** starts the count
  again from now. The clocks only run while you're fighting in the lair: after 30 seconds
  without dealing damage they pause (the idle time isn't counted) and restart on your next
  hit. **Pause** stops them straight away; it resumes when you press it again or, with
  **Auto-resume when I attack** on, when you next damage the boss.
- **Luck** (Trip tab): uniques received vs expected with a luck tier (LUCKY AS RUCK, Lucky,
  On Rate, Dry, DRY AS RUCK), kills since your last unique, how far past the drop rate you
  are, the rate and a count of each unique and the pet, laid out like the share card. The eye
  icon collapses it to the title row, which keeps the tier. **Luck card** (Configuration →
  Display) switches to the Classic card, which adds the chance by now, the next unique's kill
  count and a progress bar. On either card, right-click
  the card to enter the kill count of your last unique from before you installed the plugin;
  the dry streak then counts from there (kills before tracking began are counted from your
  kill count) until the plugin tracks a newer unique.
- **Trip:** the current (or last) trip's time, kills, average kill time, fastest kill (PB)
  and a live timer for the kill in progress (it counts from the boss spawning, like the
  game's "Fight duration", and shows the last kill's time between kills), plus loot value,
  costs, net profit and GP/hr, plus item grids for loot, supplies and anything you dropped and left behind.
  Uniques have a gold border; tarnished drops waiting to be polished have a dashed border.
  The profit card's eye icon collapses it to just net profit and net GP/hr (green or red).
  Loot shows kills, loot per kill, loot per hour and net profit; Supplies shows the cost of
  charges, runes, potions, food and anything else.
- **History:** one card per completed trip. Click to expand it, right-click to delete it.
- **Lifetime:** totals across all trips, the Open-stomach / Take-eggs split, a profit-per-trip
  chart, and:
  - **Dryness:** Open-stomach kills since your last unique and the chance of being that dry,
    the kill counts of each Elder venator fang and Crimson kisten, and expected vs actual
    uniques and pets.
  - **Drop chances** (Trip tab): Expected / Received bars for any unique, each unique and the pet,
    using your all-time kills and drops from RuneLite's Loot Tracker (and your kill count from
    Chat Commands) when available, otherwise the kills this plugin tracked.
  - **Eggs popped:** eggs popped per tier (anywhere, not just in the lair), pets from eggs,
    and your total pet chance from the eggs popped so far.
  - **Polish results:** what each type of tarnished item has polished into.
  - **Data:** export the shown boss's trips as CSV, export or import the account's full
    history (every boss) as JSON (imports only add trips you don't already have; exports from
    older versions import too), and clear the shown boss's history. Export file names include
    the date and time.

The panel opens on the Trip tab automatically when you enter a tracked boss's area
(Configuration → Display → **Open panel on entry**; on by default).

## How trips are counted

For Phosani's Nightmare: a trip runs from drinking from the Pool of Nightmares until you
leave the dream (through the barrier, which works like walking out of the Maggot King's lair,
by teleport, or by dying). Several kills per trip are normal. Loot comes from the Loot
Tracker's event (it lands on the floor), the kill timer counts like the game's fight clock
from when the Nightmare awakens, and Sister Senga's fee is recorded from the bank payment
message after you collect your items. Luck uses Phosani's rates (any unique about 1/111).

For the Maggot King:

- A trip starts when you enter the lair. It ends when you teleport out, die, walk out and
  don't come back within 5 minutes while staying just outside, or log out and don't return
  within 5 minutes (both grace periods are configurable).
- **Merge re-entries** (off by default) counts leaving and re-entering within the merge
  window as one trip.
- A kill is counted from the game's kill-count message. Kill time comes from the game's
  "Fight duration" message.
- Loot includes items that overflow onto the ground, once you pick them up.
- Supplies are everything used up in the lair, across inventory, equipment and rune pouch.
  Potions are counted per dose. Gear switches don't count. With **Count supplies used
  before entry** (on by default), food, potions and spells used in the 60 seconds before
  entering are added to the trip too.
- Charges used during a trip count as supplies, counted from your attacks: Amulet of blood
  fury (one per damaging melee hit, priced from blood shards at 10,000 charges each), Tome
  of fire (one per fire spell, searing or burnt pages at 20 charges each, set by **Tome of
  fire pages**), revenant bows such as the Webweaver bow (one revenant ether per shot),
  Scythe of Vitur (one per attack unless every hit misses; a vial of blood and 200 blood runes
  per 100 charges), Tumeken's shadow (one per cast; 2 soul runes and 5 chaos runes each),
  Sanguinesti staff (2 blood runes a cast), Trident of the swamp (a death, a chaos, 5 fire
  runes and a Zulrah's scale a cast) and of the seas (the same runes and 10 coins), Eye of
  Ayak (a demon tear, or 2 death and a chaos rune, a cast: **Eye of Ayak charged with**) and
  the Toxic blowpipe (2 Zulrah's scales every 3 shots, plus the darts lost: an Ava's
  assembler or Dizana's quiver saves 80%, an accumulator 72%, an attractor 60%; the dart
  priced is set by **Blowpipe darts**). This applies at every supported boss.
- Items you drop in the lair and don't pick up again are a "Dropped" cost. Zero-value items
  such as empty vials are ignored.
- Gravestone moves paid to the aranei scout are recorded as death costs.
- Tarnished drops are pending until you polish them (anywhere, any time later). The
  result replaces the oldest pending drop of that type and is valued at the GE price then.
- **Loot alerts** (Configuration → Loot alerts) notify you for uniques, the pet, or any drop
  worth at least a set amount. Nothing is drawn on the game screen.
- Values use the GE price at the time of the drop or use. The Lifetime tab can also show
  loot at today's prices (**Show today's value**).

## Data

History is saved per account to
`~/.runelite/plugin-data/maggot-king-trip-tracker/history-<account>.json` (the folder keeps
its original name so existing history carries over). When a file from an older version is
upgraded, the old file is kept next to it as `history-<account>.json.v1-backup-<date>`.

**Diagnostic mode** (Configuration → Boss Trip Tracker → Developer) records raw boss related
game events to `diagnostic.log` in the same folder, for development. **Log everywhere**
records everywhere rather than only around supported bosses, for collecting data on new
ones. The log rotates at 10 MB, keeping `diagnostic.log.1` to `.3`. Leave both off during
normal play.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
