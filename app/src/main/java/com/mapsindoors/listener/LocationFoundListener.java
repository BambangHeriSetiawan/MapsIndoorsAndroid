package com.mapsindoors.listener;

import com.mapspeople.models.Location;

/**
 * <p>Listener interface to catch a location search result.</p>
 * @author Martin Hansen
 */
public interface LocationFoundListener {
	/**
	 * Listener method to catch search events from the menu.
	 */
	void onLocationsFound( Location location );
}
