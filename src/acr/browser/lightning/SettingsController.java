/*
 * Copyright 2014 A.C.R. Development
 */
package acr.browser.lightning;

public class SettingsController {

	private static boolean clearHistory;

	/**
	 * The purpose of this class is so that I can clear the dropdown history in the main activities if the user selects
	 * to clear the history from the disk in advanced settings
	 */
	public static void setClearHistory(boolean choice) {
		clearHistory = choice;
	}

	/**
	 * return the choice
	 */
	public static boolean getClearHistory() {
		if (clearHistory) {
			clearHistory = false;
			return true;
		}
		return false;
	}
}
