package com.maggotkingtriptracker.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Kill
{
	/**
	 * Kill count from the game's kill-count message; null if the message was missed.
	 */
	private Integer killCount;
	private long endedAt;
	/**
	 * Fight duration reported by the game, in milliseconds; null if unknown.
	 */
	private Long durationMs;
	private CorpseChoice choice;
	private List<ItemEntry> loot = new ArrayList<>();
	private boolean pet;
}
