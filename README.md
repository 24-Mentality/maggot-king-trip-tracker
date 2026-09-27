# Maggot King Trip Tracker

> **No combat or mechanic assistance.** This plugin only tracks loot, supplies and profit.
> It has no attack, prayer or hazard cues, no tile or NPC highlighting, no phase indicators,
> and it draws nothing on the game screen. It only listens to game events and never creates
> input or menu actions.

Tracks loot, supplies used and net profit for each trip to the Maggot King in Vampyrium,
with persistent per-account history in a side panel.

## Requirements

- The core RuneLite **Loot Tracker** plugin must be enabled. This plugin reads Maggot King
  loot from the Loot Tracker's loot events.

## Status

Early development. The only feature so far is an opt-in **Diagnostic mode**
(Configuration → Maggot King Trip Tracker → Developer). It records Maggot King related game
events to `~/.runelite/plugin-data/maggot-king-trip-tracker/diagnostic.log` so that loot,
supply and kill detection can be built from real data. Leave it off during normal play.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
