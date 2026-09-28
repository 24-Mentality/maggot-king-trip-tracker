# Maggot King Trip Tracker

> **No combat or mechanic assistance.** This plugin only tracks loot, supplies and profit.
> It has no attack, prayer or hazard cues, no tile or NPC highlighting, no phase indicators,
> and it draws nothing on the game screen. It only listens to game events and never creates
> input or menu actions.

Tracks loot, supplies used and net profit for each trip to the Maggot King in Vampyrium,
with persistent per-account history in a side panel.

## Requirements

- The core RuneLite **Loot Tracker** plugin must be enabled. This plugin reads Maggot King
  loot from the Loot Tracker's loot events. If no loot event arrives, it falls back to
  inventory changes after you open the corpse.

## Side panel

- **Kill goal** (top of the Trip tab): set a kill target and see kills per hour (logged-in
  time), kills done and left, time to goal, and a progress bar. **Reset** starts the count
  again from now. The clocks only run while you're fighting in the lair: after 30 seconds
  without dealing damage they pause (the idle time isn't counted) and restart on your next
  hit. **Pause** stops them straight away; it resumes when you press it again or, with
  **Auto-resume when I attack** on, when you next damage the boss.
- **Luck** (Trip tab): uniques received vs expected with a luck tier (LUCKY AS RUCK, Lucky,
  On Rate, Dry, DRY AS RUCK), your dry streak and when the next unique is due.
- **Trip:** the current (or last) trip's time, kills, loot value, costs, net profit, GP/hr and average kill
  time, plus item grids for loot, supplies and anything you dropped and left behind.
  Uniques have a gold border; tarnished drops waiting to be polished have a dashed border.
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
  - **Data:** export trips as CSV, export or import the account's full history as JSON
    (imports only add trips you don't already have), and clear all history.

The panel opens on the Trip tab automatically when you enter the lair
(Configuration → Display → **Open panel in the lair**; on by default).

## How trips are counted

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
- Charges used in the lair count as supplies, counted from your attacks: Amulet of blood
  fury (one per damaging melee hit, priced from blood shards at 10,000 charges each), Tome
  of fire (one per fire spell, searing or burnt pages at 20 charges each, set by **Tome of
  fire pages**) and revenant bows such as the Webweaver bow (one revenant ether per shot).
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
`~/.runelite/plugin-data/maggot-king-trip-tracker/history-<account>.json`.

**Diagnostic mode** (Configuration → Maggot King Trip Tracker → Developer) records raw
Maggot King related game events to `diagnostic.log` in the same folder, for development.
Leave it off during normal play.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
