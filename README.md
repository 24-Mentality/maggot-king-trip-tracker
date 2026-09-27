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

- **Current Trip:** trip time, kills, loot value, costs, net profit, GP/hr and average kill
  time, plus item grids for loot, supplies and anything you dropped and left behind.
  Uniques have a gold border; tarnished drops waiting to be polished have a dashed border.
- **History:** one card per completed trip. Click to expand it, right-click to delete it.
- **Lifetime:** totals across all trips, the Open-stomach / Take-eggs split, a profit-per-trip
  chart, and a button to clear all history for the account.

## How trips are counted

- A trip starts when you enter the lair and ends when you leave it: walking out,
  teleporting, dying, or logging out and not returning within the grace period
  (5 minutes by default).
- **Merge re-entries** (off by default) counts leaving and re-entering within the merge
  window as one trip.
- A kill is counted from the game's kill-count message. Kill time comes from the game's
  "Fight duration" message.
- Loot includes items that overflow onto the ground, once you pick them up.
- Supplies are everything used up in the lair, across inventory, equipment and rune pouch.
  Potions are counted per dose. Gear switches don't count. With **Count supplies used
  before entry** (on by default), food, potions and spells used in the 60 seconds before
  entering are added to the trip too.
- Charges used in the lair count as supplies: Amulet of blood fury (priced from blood
  shards, 10,000 charges each), Tome of fire (searing or burnt pages, 20 charges each,
  set by **Tome of fire pages**) and revenant weapons such as the Webweaver bow (one
  revenant ether per shot).
- Items you drop in the lair and don't pick up again are a "Dropped" cost. Zero-value items
  such as empty vials are ignored.
- Gravestone moves paid to the aranei scout are recorded as death costs.
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
