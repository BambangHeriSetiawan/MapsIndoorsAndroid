package com.mapsindoors.listener;

/**
 * <p>Listener interface to catch floating action button events.</p>
 * @author Martin Hansen
 */
public interface FloatingActionListener {
	/**
	 * Listener method to catch search events from the menu.
	 */
	void onFABSelect( String selectedType );
}
