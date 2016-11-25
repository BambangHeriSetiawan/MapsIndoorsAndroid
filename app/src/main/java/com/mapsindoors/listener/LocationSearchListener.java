package com.mapsindoors.listener;

import com.mapspeople.models.Location;

import java.util.List;

/**
 * <p>Listener interface to catch location search results.</p>
 * @author Martin Hansen
 */
public interface LocationSearchListener {
	/**
	 * Listener method to catch search events from the menu.
	 */
	void onLocationsFound( List<Location> locationList );
}
