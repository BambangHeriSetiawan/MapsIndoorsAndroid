package com.mapsindoors.listener;

import com.mapsindoors.IconTextListAdapter;
import com.mapspeople.models.Location;

/**
 * <p>Listener interface to catch data context fetching events.</p>
 * @author Martin Hansen
 */
public interface MenuListener {
	/**
	 * Listener method to catch search events from the menu.
	 */
	void onMenuSearch(String searchString, boolean finalSearch );
	/**
	 * Listener method to catch search events from the menu.
	 */
	void onMenuSelect(Object selectedObject, IconTextListAdapter.Objtype objtype );
	/**
	 * If called, the callee want to show a location on the map
	 */
	void onMenuShowLocation(Location location);
	/**
	 * If called, the callee want to show a route to a location on the map
	 */
	void onMenuShowRoute(Location location);
	/**
	 * If called, the callee want to change venue
	 */
	void onMenuVenueSelect(String venueId);
}
