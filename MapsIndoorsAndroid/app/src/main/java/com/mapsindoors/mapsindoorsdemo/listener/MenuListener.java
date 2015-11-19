package com.mapsindoors.mapsindoorsdemo.listener;

import com.mapsindoors.mapsindoorsdemo.IconTextListAdapter;
import com.mapspeople.models.Location;

/**
 * <p>Listener interface to catch data context fetching events.</p>
 * @author Martin Hansen
 */
public interface MenuListener {
	/**
	 * Listener method to catch search events from the menu.
	 */
	void onSearch( String searchString, boolean finalSearch );
	/**
	 * Listener method to catch search events from the menu.
	 */
	void onSelect( Object selectedObject, IconTextListAdapter.Objtype objtype );
	/**
	 * If called, the callee want to show a location on the map
	 */
	void onShowLocation(Location location);
	/**
	 * If called, the callee want to show a route to a location on the map
	 */
	void onShowRoute(Location location);
}
