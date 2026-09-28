package com.maggotkingtriptracker.ui;

/**
 * What the panel's buttons do. Called on the Swing thread.
 */
public interface PanelActions
{
	/**
	 * Show this boss in all three tabs.
	 */
	void selectBoss(String bossId);

	/**
	 * @param variant variant id, or null for All
	 */
	void selectVariant(String variant);

	/**
	 * @param killCount kill count of your last unique from before tracking; null clears it
	 */
	void setLastUniqueKc(Integer killCount);

	void deleteTrip(String tripId);

	void clearHistory();

	void exportCsv();

	void exportJson();

	void importJson();

	/**
	 * @param target kills; 0 removes the goal
	 */
	void setGoal(int target);

	void resetGoal();

	/**
	 * Pause or resume the trip and goal clocks.
	 */
	void togglePause();
}
