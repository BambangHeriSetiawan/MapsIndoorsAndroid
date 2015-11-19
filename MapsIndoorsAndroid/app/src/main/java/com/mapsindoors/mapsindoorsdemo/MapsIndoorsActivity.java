package com.mapsindoors.mapsindoorsdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import io.fabric.sdk.android.Fabric;

import org.altbeacon.beacon.BeaconConsumer;

import com.mapsindoors.mapsindoorsdemo.listener.DirectionsMenuListener;
import com.mapspeople.data.LocationQuery;
import com.mapspeople.data.MPLocationsProvider;
import com.mapspeople.data.OnDataReadyListener;
import com.mapspeople.data.OnLocationsReadyListener;
import com.mapspeople.debug.dbglog;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.Floor;
import com.mapspeople.models.Location;
import com.mapspeople.models.LocationDisplayRule;
import com.mapspeople.models.LocationDisplayRules;
import com.mapspeople.models.MPLocation;
import com.mapspeople.models.POIType;
import com.mapspeople.models.Point;
import com.mapspeople.models.Solution;
import com.mapspeople.models.VenueCollection;
import com.mapspeople.position.BeaconPositionProvider;
import com.mapspeople.routing.MPRoutingProvider;
import com.mapspeople.routing.OnRouteResultListener;
import com.mapspeople.routing.Route;
import com.mapspeople.routing.TravelMode;
import com.mapspeople.ui.FloorSelector;

import com.mapsindoors.mapsindoorsdemo.fragment.DirectionsmenuFragment;
import com.mapsindoors.mapsindoorsdemo.fragment.MenuFragment;
import com.mapsindoors.mapsindoorsdemo.listener.FloatingActionListener;
import com.mapsindoors.mapsindoorsdemo.listener.MenuListener;
import com.mapsindoors.mapsindoorsdemo.positionprovider.GPSPositionProvider;
import com.mapsindoors.mapsindoorsdemo.positionprovider.MixedPositionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsIndoorsActivity extends AppCompatActivity implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener, OnDataReadyListener, BeaconConsumer, View.OnTouchListener, MenuListener, FloatingActionListener, GoogleMap.OnMarkerClickListener, DirectionsMenuListener
{
	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	private MenuFragment mMenuFragment;
	private MapControl mMapControl;
	private String searchPlaces = null;
	private DirectionsmenuFragment directionsmenuFragment;
	private boolean isLocationAccessGranted;

	private final String networkId = "rtx";
	private final Point START_POSITION = new Point(57.0859564, 9.9569432);
	private String clientId = "550c26a864617400a40f0000";

	public static LocationDisplayRules displayRules;
	private BeaconPositionProvider beaconPositionProvider;
	private Solution solution;
	private ListView mainMenuList;
	private boolean isSearching = false;
	private String pendingSearch;
	private FloatingAction floatingActionButton;
	private static Map<String, Object> locationTypeImages = new HashMap<>();
	private boolean menuInitialized;
	public static Activity mainActivity;
	private MixedPositionProvider mixedPositionProvider;

	private static final String[] LOCATION_PERMS = {Manifest.permission.ACCESS_FINE_LOCATION};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Fabric.with(this, new Crashlytics());
		//Giving the Google server key to our routing provider.
		//Be sure to add your own key in: src\release\res\values\google_maps_api.xml and src\debug\res\values\google_maps_api.xml
		MPRoutingProvider.setGoogleServerKey(getString(R.string.google_server_key));
		if ( getString(R.string.google_maps_key).equalsIgnoreCase("InsertYourAndroidKeyHere"))
			Toast.makeText(getApplicationContext(), "WARNING: Google key not found! The map will not work.", Toast.LENGTH_LONG).show();
		menuInitialized = false;
		if (!isGMSavailable())
		{
			downloadGMS();
		}
		else
		{
			isLocationAccessGranted = false;
			setContentView(R.layout.activity_maps_indoors);
			ActionBar ac = getActionBar();
			if (ac != null)
			{
				ac.setHomeButtonEnabled(true);
			}
			floatingActionButton = new FloatingAction(this, this);
			Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
			setSupportActionBar(toolbar);
			toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
			toolbar.setNavigationIcon(R.drawable.ic_dehaze_white_24dp);
			toolbar.setNavigationOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (!mMenuFragment.isMenuOpen())
					{
						mMenuFragment.openMenu();
					}
				}
			});
			mainActivity = this;
			obtainLocationPermission();
			setUpMapIfNeeded();
		}
	}

	private boolean isGMSavailable()
	{
		// See if google play services are installed.
		boolean services;
		try
		{
			getPackageManager().getApplicationInfo("com.google.android.gms", 0);
			services = true;
		} catch (PackageManager.NameNotFoundException e)
		{
			services = false;
		}
		return services;
	}

	private void downloadGMS()
	{
		//Toast.makeText(this, "Play Services not installed!\nPlease download it from the Play Store before using this app.", Toast.LENGTH_LONG).show();
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		// set dialog message
		alertDialogBuilder
				.setTitle("Google Play Services")
				.setMessage("The map requires Google Play Services to be installed.")
				.setCancelable(false)
				.setPositiveButton("Install", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						dialog.dismiss();
						// Try the new HTTP method (I assume that is the official way now given that google uses it).
						try
						{
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.gms"));
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
							intent.setPackage("com.android.vending");
							startActivity(intent);
						} catch (ActivityNotFoundException e)
						{
							// Ok that didn't work, try the market method.
							try
							{
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"));
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								intent.setPackage("com.android.vending");
								startActivity(intent);
							} catch (ActivityNotFoundException f)
							{
								// Ok, weird. Maybe they don't have any market app. Just show the website.
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.google.android.gms"));
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
								startActivity(intent);
							}
						}
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						dialog.cancel();
					}
				})
				.create()
				.show();
	}

	//From API 23 and up we need to ask for permission on runtime if needed.
	//http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-runtime-permissions.
	@TargetApi(23)
	private void obtainLocationPermission()
	{
		//If not on marshmellow location is allowed
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
		{
			isLocationAccessGranted = true;
			return;
		}
		//Request permission if not given already
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			requestPermissions(LOCATION_PERMS, 0);
		}
		else
		{
			isLocationAccessGranted = true;
		}
	}

	@TargetApi(23)
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
		{
			//User accepted. Start the app.
			isLocationAccessGranted = true;
			setUpMapIfNeeded();
		}
		else
		{
			//Still not allowed. Ask again.
			requestPermissions(LOCATION_PERMS, 0);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		//Detects if the burger menu button at the top bar has been called
		if (item.getItemId() == android.R.id.home)
		{
			if (mMenuFragment.isMenuOpen())
			{
				mMenuFragment.closeMenu();
			}
			else
			{
				mMenuFragment.openMenu();
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu)
	{
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		setUpMapIfNeeded();
	}

	@Override
	public void onMapLongClick(LatLng latLng)
	{
		/*
		Floor floor = mMapControl.getCurrentBuildingFloor();
		LatLng target = mMap.getCameraPosition().target;
		if (floor != null && target != null)
		{
			if (to != null)
			{
				from = to;
			}
			else
			{
				from = new Point(target.latitude, target.longitude, floor.getZIndex());
			}
			to = new Point(latLng.latitude, latLng.longitude, floor.getZIndex());
			mMapControl.route(new MPLocation(from, "Start"), new MPLocation(to, "Destination"), TravelMode.TRAVEL_MODE_WALKING, new String[]{}, null, null);
		}
		*/
	}

	//Called when the user makes a tap gesture on the map, but only if none of the overlays of the map handled the gesture.
	@Override
	public void onMapClick(LatLng latLng)
	{
		/*
		PositionIndicator myPos = mMapControl.getCurrentPosition();
		Point point = myPos.getPoint();
		if (from != null)
		{
			Floor floor = mMapControl.getCurrentBuildingFloor();
			to = new Point(latLng.latitude, latLng.longitude, floor.getZIndex());
			mMapControl.route(new MPLocation(from, "Start"), new MPLocation(to, "Destination"), TravelMode.TRAVEL_MODE_WALKING, new String[]{}, null, null);
		}
		*/
		floatingActionButton.closeMenu();
	}

	@Override
	public void onLocationDataReady()
	{
	}

	@Override
	public void onAppDataReady()
	{
	}

	@Override
	public void onVenueDataReady(VenueCollection vc)
	{
	}

	@Override
	public void onSolutionDataReady(final Solution solution)
	{
		this.solution = solution;
		List<POIType> types = solution.getTypes();
		for (POIType t : types)
		{
			loadIcon(t.name, t.icon);
		}
		initializeMenu();
	}

	private void initializeMenu()
	{
		if (menuInitialized)
		{
			return;
		}
		if (mMenuFragment == null)
		{
			mMenuFragment = (MenuFragment)getSupportFragmentManager().findFragmentById(R.id.menufragment);
			if (mMenuFragment == null)
			{
				return;
			}
			mMenuFragment.init(this, this);
		}
		new Handler(getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				mainMenuList = mMenuFragment.initMenu(solution);
				if (mainMenuList != null)
				{
					menuInitialized = true;
				}
			}
		});
	}

	/**
	 * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
	 * installed) and the map has not already been instantiated.
	 * <p/>
	 * If it isn't installed {@link SupportMapFragment} (and
	 * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
	 * install/update the Google Play services APK on their device.
	 * <p/>
	 * A user can return to this FragmentActivity after following the prompt and correctly
	 * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
	 * have been completely destroyed during this process (it is likely that it would only be
	 * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
	 * method in {@link #onResume()} to guarantee that it will be called.
	 */
	private void setUpMapIfNeeded()
	{
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null && isLocationAccessGranted)
		{
			// Try to obtain the map from the SupportMapFragment.
			SupportMapFragment mMapFragment = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.mapfragment));
			mMapControl = new MapControl(this, mMapFragment);
			mMapControl.setOnDataReadyListener(this);
			mMapControl.setTileSize(512);
			mMap = mMapFragment.getMap();
			// Check if we were successful in obtaining the map.
			// Might be null if Google Play services APK is not available.
			if (mMap != null)
			{
				Point center = new Point(START_POSITION.lat(), START_POSITION.lng(), 0);
				String sId = getAppSchemeQueryParam("solutionId");
				String nId = getAppSchemeQueryParam("networkId");
				sId = (sId == null) ? clientId : sId;
				nId = (nId == null) ? networkId : nId;
				//Initialize the map and set a position and floor so we have a proper starting point
				mMapControl.initMap(sId, nId);
				mMapControl.setMapPosition(center, 18, false);
				mMapControl.selectFloor(0);
				mMapControl.showUserPosition(true);
				//mMapControl.setCurrentPosition(START_POSITION, 0);
				//Display rule can be added if needed. Here we set an indicator icon in the closest building.
				displayRules = new LocationDisplayRules();
				displayRules.add(new LocationDisplayRule("Auditorium", 10f, false));
				displayRules.add(new LocationDisplayRule("Ciff", 10f, false));
				displayRules.add(new LocationDisplayRule("Conference", 10f, false));
				displayRules.add(new LocationDisplayRule("Entrance", 10f, false));
				displayRules.add(new LocationDisplayRule("Halls", 18f, true));
				displayRules.add(new LocationDisplayRule("Parkering", 10f, false));
				displayRules.add(new LocationDisplayRule("Restaurant", 10f, false));
				displayRules.add(new LocationDisplayRule("Restroom", 18f, false));
				displayRules.add(new LocationDisplayRule("Stairs", 18f, false));
				mMapControl.addDisplayRules(displayRules);
				//Set the color of the floorselector
				FloorSelector.DEFAULT_COLOR = ContextCompat.getColor(this, R.color.white);
				FloorSelector.DEFAULT_COLOR_ACTIVE = ContextCompat.getColor(this, R.color.primary);
				if (mixedPositionProvider == null)
				{
					//Add a position provider in able to track the user's position.
					GPSPositionProvider gpsProvider = new GPSPositionProvider(this);
					//For beacon layout check : https://altbeacon.github.io/android-beacon-library/javadoc/org/altbeacon/beacon/BeaconParser.html#setBeaconLayout(java.lang.String)
					//GELO UUID: "11E44F09-4EC4-407E-9203-CF57A50FBCE0"
					beaconPositionProvider = new BeaconPositionProvider(this, "");
					beaconPositionProvider.setBeaconParserLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
					//beaconPositionProvider.addBeaconsToMap(mMap, true);
					beaconPositionProvider.useTrilateration(false);
					mixedPositionProvider = new MixedPositionProvider(gpsProvider, beaconPositionProvider);
				}
				//Telling map control about our provider
				mMapControl.addPositionProvider(mixedPositionProvider);
				mMapControl.startPositioning();
				//mMapControl.startTrackPosition();
				mMapControl.showUserPosition(true);
				mMapControl.getFloorSelector();
				//Finally set the listeners you want to use here
				mMap.setOnMapLongClickListener(this);
				mMap.setOnMapClickListener(this);
				mMapControl.setOnMarkerClickListener(this);
				directionsmenuFragment = (DirectionsmenuFragment)getSupportFragmentManager().findFragmentById(R.id.directionsmenufragment);
				directionsmenuFragment.init(this, mMap, this);
				mMapControl.setOnFloorUpdateListener(directionsmenuFragment);
			}
			//addMenuButton();
		}
	}

	private String getAppSchemeQueryParam(String paramName)
	{
		String param = null;
		Uri data = getIntent().getData();
		if (data != null)
		{
			List<String> paths = data.getPathSegments();
			if (paths.size() > 0)
			{
				param = data.getQueryParameter(paramName);
			}
		}
		return param;
	}
//	private void addMenuButton()
//	{
//		ViewGroup vg = (ViewGroup)mMapFragment.getView();
//		LinearLayout container = new LinearLayout(this);
//		LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//		cLp.gravity = Gravity.TOP;
//		container.setGravity(Gravity.TOP);
//		container.setOrientation(LinearLayout.VERTICAL);
//		ImageButton ib = new ImageButton(this);
//		ib.setImageResource(R.drawable.ic_dehaze_black_24dp);
//		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Convert.getPixels(24, this), Convert.getPixels(24, this));
//		lp.setMargins(Convert.getPixels(4, this), 0, 0, Convert.getPixels(4, this));
//		lp.gravity = Gravity.LEFT | Gravity.TOP;
//		ib.setLayoutParams(lp);
//		ib.setBackgroundColor(Color.TRANSPARENT);
//		ib.setOnClickListener(new View.OnClickListener()
//		{
//			@Override
//			public void onClick(View v)
//			{
//				mMenuFragment.openMenu();
//			}
//		});
//		container.addView(ib);
//		vg.addView(container);
//	}

	@Override
	public void onBeaconServiceConnect()
	{
		beaconPositionProvider.onBeaconServiceConnect();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		return false;
	}

	@Override
	public void onShowRoute(Location location)
	{
//		mixedPositionProvider.getLatestPosition();
		Floor floor = mMapControl.getCurrentBuildingFloor();
		LatLng target = mMap.getCameraPosition().target;
		if (floor != null && target != null)
		{
			//Get the latest position from our position providers made for mapcontrol earlier.
			Point from = mMapControl.getCurrentPosition().getPoint();
			//If the latest position is at 0,0 it's not updated yet. Using the camera position instead.
			//If you want a default position before we get any signals from our position providers, use setCurrentPosition() on mapscontrol.
			if (from.lat() == 0 && from.lng() == 0)
			{
				from = new Point(target.latitude, target.longitude, floor.getZIndex());
			}
			directionsmenuFragment.route(new MPLocation(from, "Start"), location, TravelMode.TRAVEL_MODE_WALKING, new String[]{}, null, null);
			//Alternative: Use the mapcontrol route for rendering
			//mMapControl.route(new MPLocation(from, "Start"), location, TravelMode.TRAVEL_MODE_WALKING, new String[]{}, null, null);
		}
		//When the menu calls this it's because a direction view is about to be shown. The action button should now be hidden until that view is no longer active.
		floatingActionButton.setActive(false);
		mMenuFragment.closeMenu();
	}

	@Override
	public void onShowLocation(Location location)
	{
		mMapControl.setMapPosition(location.getPoint(), 19, true);
		List<Location> loc = new ArrayList<>(1);
		loc.add(location);
		mMapControl.selectFloor(location.getFloorIndex());
		mMapControl.displaySearchResults(loc, false);
//		mMapControl.showInfoSnippet(location);
//		mMapControl.displaySingleLocation(location);
		mMenuFragment.closeMenu();
	}

	//Callback from the menu stating that the user wants to search for locations containing a given name.
	//If no name is given we should list the location types instead
	@Override
	public void onSearch(String searchString, boolean finalSearch)
	{
		if (searchString == null || searchString.length() < 1)
		{
			//If the searching string is empty show all types defined in the solution instead.
			isSearching = false;
			pendingSearch = null;
			IconTextListAdapter adapter = (IconTextListAdapter)mainMenuList.getAdapter();
			ArrayList<IconTextElement> elements = new ArrayList<>();
			for (POIType type : solution.getTypes())
			{
				String typeName = type.name;
				Bitmap bm = (Bitmap)locationTypeImages.get(typeName);
				if (bm != null)
				{
					elements.add(new IconTextElement(typeName, bm, type, IconTextListAdapter.Objtype.TYPE));
				}
				else
				{
					elements.add(new IconTextElement(typeName, com.mapspeople.mapssdk.R.drawable.step, type, IconTextListAdapter.Objtype.TYPE));
				}
			}
			adapter.setList(elements);
			mMenuFragment.setExitbuttonActive(false);
			return;
		}
		if (searchPlaces == null)
		{
			searchPlaces = getResources().getString(R.string.search_places);
		}
		if (searchString.equalsIgnoreCase(searchPlaces))
		{
			return;
		}
		findLocations(searchString, null);
	}

	@Override
	public void onSelect(Object selected, IconTextListAdapter.Objtype objtype)
	{
		if (objtype == IconTextListAdapter.Objtype.TYPE)
		{
			//A location type is selected. Find all locations with that type.
			findLocations("", ((POIType)selected).name);
			mMenuFragment.setExitbuttonActive(true);
		}
		else if (objtype == IconTextListAdapter.Objtype.LOCATION)
		{
			//A specific location is selected. Get detailed data from it and launch the POI detail menu
			Location location = (Location)selected;
			MPLocationsProvider locationsProvider = new MPLocationsProvider(this);
			locationsProvider.setOnLocationsReadyListener(new OnLocationsReadyListener()
			{
				@Override
				public void onLocationsReady(final List<Location> locations) {}

				@Override
				public void onLocationDetailsReady(final Location location)
				{
					new Handler(getMainLooper()).post(new Runnable()
					{
						@Override
						public void run()
						{
							//Location data aquired. Find the route from current position to that location.
							MPRoutingProvider routingProvider = new MPRoutingProvider();
							routingProvider.setTravelMode(TravelMode.TRAVEL_MODE_WALKING);
							routingProvider.setOnRouteResultListener(new OnRouteResultListener()
							{
								@Override
								public void onRouteResult(Route route)
								{
									//Launching location details menu here
									mMenuFragment.openLocationMenu(location, route);
								}

								@Override
								public void onRoutingInfoResult(Object info)
								{
								}
							});
							Point myPos = mMapControl.getCurrentPosition().getPoint();
							if (myPos.lng() == 0 || myPos.lat() == 0)
							{
								myPos = new Point(START_POSITION);
							}
							routingProvider.query(myPos, location.getPoint());
						}
					});
				}
			});
			locationsProvider.getLocationsDetailsAsync(clientId, location.getId());
		}
	}

	//Finds and shows a locations based on a search string and optionally with a type filter (can be null)
	private void findLocations(String searchString, String type)
	{
		final LocationQuery query = new LocationQuery();
		query.arg = clientId;
		query.setQuery(searchString);
		if (type != null)
		{
			List<String> list = new ArrayList<>(1);
			list.add(type);
			query.types = list;
		}
		//Clear the list before searching
		IconTextListAdapter adapter = (IconTextListAdapter)mainMenuList.getAdapter();
		adapter.setList(new ArrayList<IconTextElement>());
		if (!isSearching)
		{
			isSearching = true;
			//Set up a location listener and make a location search call
			final MPLocationsProvider locationsProvider = new MPLocationsProvider(this);
			locationsProvider.setOnLocationsReadyListener(new OnLocationsReadyListener()
			{
				@Override
				public void onLocationsReady(final List<Location> locations)
				{
					if (locations == null)
					{
						//An error occured. Retry in 1 second.
						try
						{
							synchronized (this)
							{
								this.wait(1000);
							}
						} catch (InterruptedException ie)
						{
							dbglog.Log("Timer error");
						}
						locationsProvider.getLocationsUsingQueryAsync(query, new Locale("en"));
						return;
					}
					new Handler(getMainLooper()).post(new Runnable()
					{
						@Override
						public void run()
						{
							if (isSearching)
							{
								IconTextListAdapter adapter = (IconTextListAdapter)mainMenuList.getAdapter();
								ArrayList<IconTextElement> elements = new ArrayList<>();
								for (Location location : locations)
								{
									if (location != null)
									{
										String typeName = location.getType();
										Bitmap bm = (Bitmap)locationTypeImages.get(typeName);
										if (bm != null)
										{
											elements.add(new IconTextElement(location.getName(), bm, location, IconTextListAdapter.Objtype.LOCATION));
										}
										else
										{
											elements.add(new IconTextElement(location.getName(), com.mapspeople.mapssdk.R.drawable.step, location, IconTextListAdapter.Objtype.LOCATION));
										}
									}
								}
								adapter.setList(elements);
								isSearching = false;
								if (pendingSearch != null)
								{
									String searchString = pendingSearch;
									pendingSearch = null;
									onSearch(searchString, true);
								}
							}
						}
					});
				}

				@Override
				public void onLocationDetailsReady(Location location) {}
			});
			//Do the actual search call
			locationsProvider.getLocationsUsingQueryAsync(query, new Locale("en"));
		}
		else
		{
			//Already waiting for a response. Store the query for later use.
			pendingSearch = searchString;
		}
	}

	@Override
	public void onFABSelect(String selectedType)
	{
		LocationQuery query = new LocationQuery();
		query.arg = clientId;
		query.near = new Point(0, 0);
		query.max = 10;
		List<String> list = new ArrayList<>(1);
		list.add(selectedType);
		query.types = list;
		final MPLocationsProvider locationsProvider = new MPLocationsProvider(this);
		locationsProvider.setOnLocationsReadyListener(new OnLocationsReadyListener()
		{
			@Override
			public void onLocationsReady(final List<Location> locations)
			{
				if (locations == null)
				{
					//An error occured.
					return;
				}
				new Handler(getMainLooper()).post(new Runnable()
				{
					@Override
					public void run()
					{
						mMapControl.displaySearchResults(locations, true);
					}
				});
			}

			@Override
			public void onLocationDetailsReady(Location location) {}
		});
		//Do the actual search call
		locationsProvider.getLocationsUsingQueryAsync(query, new Locale("en"));
	}

	public static Map<String, Object> getLocationTypeNames()
	{
		return locationTypeImages;
	}

	//Loads an image from a URL and adds them to the locationTypeImages dictionary (if found)
	public static void loadIcon(final String typeName, final String iconURL)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(iconURL).getContent());
					if (bitmap != null)
					{
						locationTypeImages.put(typeName, bitmap);
					}
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	@Override
	public boolean onMarkerClick(Marker marker)
	{
		LatLng latLng = marker.getPosition();
		mMapControl.setMapPosition(new Point(latLng.latitude, latLng.longitude), 19, true);
		marker.showInfoWindow();
		return true;
	}

	public MapControl getMapControl()
	{
		return mMapControl;
	}

	@Override
	public void onDirectionsMenuClose()
	{
		//When the directionsMenu is being closed, make sure to show the action button again.
		floatingActionButton.setActive(true);
	}
}
