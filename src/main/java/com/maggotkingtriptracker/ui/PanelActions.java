package com.maggotkingtriptracker.ui;

/**
 * What the panel's buttons do. Called on the Swing thread.
 */
public interface PanelActions
{
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
