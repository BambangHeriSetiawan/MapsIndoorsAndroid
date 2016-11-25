package com.mapsindoors;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;

import io.fabric.sdk.android.Fabric;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import com.mapsindoors.listener.DirectionsMenuListener;
import com.mapsindoors.positionprovider.BeaconPositionProvider;
import com.mapsindoors.positionprovider.GoogleAPIPositionProvider;
import com.mapspeople.data.LocationQuery;
import com.mapspeople.data.MPLocationsProvider;
import com.mapspeople.data.OnDataReadyListener;
import com.mapspeople.data.OnLocationsReadyListener;
import com.mapspeople.debug.dbglog;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.CategoryCollection;
import com.mapspeople.models.Floor;
import com.mapspeople.models.Location;
import com.mapspeople.models.LocationDisplayRules;
import com.mapspeople.models.MPLocation;
import com.mapspeople.models.POIType;
import com.mapspeople.models.Point;
import com.mapspeople.models.PushMessage;
import com.mapspeople.models.PushMessageCollection;
import com.mapspeople.models.AppConfig;
import com.mapspeople.models.Solution;
import com.mapspeople.models.Venue;
import com.mapspeople.models.VenueCollection;
import com.mapspeople.routing.MPRoutingProvider;
import com.mapspeople.routing.OnRouteResultListener;
import com.mapspeople.routing.Route;
import com.mapspeople.routing.TravelMode;
import com.mapspeople.ui.FloorSelector;

import com.mapsindoors.fragment.DirectionsmenuFragment;
import com.mapsindoors.fragment.MenuFragment;
import com.mapsindoors.listener.FloatingActionListener;
import com.mapsindoors.listener.MenuListener;
import com.mapsindoors.positionprovider.GPSPositionProvider;
import com.mapsindoors.positionprovider.MixedPositionProvider;
import com.mapspeople.util.Convert;
import com.mapspeople.util.ThreadUtil;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32;

public class MapsIndoorsActivity extends AppCompatActivity implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener, OnDataReadyListener, BeaconConsumer, View.OnTouchListener, MenuListener, FloatingActionListener, GoogleMap.OnMarkerClickListener, DirectionsMenuListener, BootstrapNotifier
{
	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	private MenuFragment mMenuFragment;
	private MapControl mMapControl;
	private String searchPlaces = null;
	private DirectionsmenuFragment directionsmenuFragment;
	private boolean isLocationAccessGranted;

	private Point START_POSITION = new Point();
	private String solutionId = "550c26a864617400a40f0000";

	public static LocationDisplayRules displayRules;
	private BeaconPositionProvider beaconPositionProvider;
	private Solution solution;
	private VenueCollection venues;
	private PushMessageCollection pushMessages;
	private AppConfig settings;
	private boolean solutionReady = false;
	private boolean venueReady = false;
	private boolean settingsReady = false;
	private boolean dataReady = false;
	private ListView mainMenuList;
	private boolean isSearching = false;
	private String pendingSearch;
	private FloatingAction floatingActionButton;
	private static Map<String, Object> locationTypeImages = new HashMap<>();
	private boolean menuInitialized;
	public static Activity mainActivity;
	private GoogleAPIPositionProvider positionProvider;
	private String categoryFilter;
	private String typeFilter;
	private SupportMapFragment mMapFragment;
	private static final Lock mutex = new ReentrantLock(true);
	private boolean initializeCalled = false;
	Thread backgroundMessageCheck;
	private RegionBootstrap regionBootstrap;
	private BeaconManager beaconManager;
	private TopSearchField topSearchField;
	private HashMap<String, CloudMessage> cloudMessageMap;
	private String startedWithMessageId;
	private static final String[] LOCATION_PERMS = {Manifest.permission.ACCESS_FINE_LOCATION};

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (beaconPositionProvider != null)
		{
			beaconPositionProvider.unbindBeaconManager();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//dbglog.useDebug(true);

		startedWithMessageId = null;
		//Check if the app is started via a notification.
		if (getIntent().getExtras() != null)
		{
			Bundle b = getIntent().getExtras();
			if (b.getBoolean("fromNotification"))
			{
				String messageId = b.getString("messageId");
				if (messageId != null)
				{
					NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
					CRC32 crc = new CRC32();
					crc.update(messageId.getBytes());
					int id = (int)crc.getValue();
					mNotificationManager.cancel(id);
					//Note this message ID for later, when push messages are actually loaded.
					startedWithMessageId = messageId;
				}
			}
		}
		Fabric.with(this, new Crashlytics());
		//Giving the Google server key to our routing provider.
		//Be sure to add your own key in: src\release\res\values\google_maps_api.xml and src\debug\res\values\google_maps_api.xml
		MPRoutingProvider.setGoogleServerKey(getString(R.string.google_server_key));
		if (getString(R.string.google_maps_key).equalsIgnoreCase("InsertYourAndroidKeyHere"))
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
			topSearchField = new TopSearchField(this)
			{
				@Override
				void onClosePressed()
				{
					mMapControl.clearMap();
				}
			};
			Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
			if (toolbar != null)
			{
				setSupportActionBar(toolbar);
				toolbar.setNavigationOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (mMenuFragment != null && !mMenuFragment.isMenuOpen())
						{
							mMenuFragment.openMenu();
						}
						else
						{
							initializeMenu();
						}
					}
				});
			}
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

	private PushMessage getPushMessage(String messageId)
	{
		PushMessage message = null;
		if (pushMessages != null)
		{
			for (PushMessage p : pushMessages.getPushMessages())
			{
				if (p.getId().equalsIgnoreCase(messageId))
				{
					message = p;
					break;
				}
			}
		}
		return message;
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
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
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
		if (mMapControl != null)
		{
			mMapControl.startPositioning();
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (mMapControl != null)
		{
			mMapControl.stopPositioning();
		}
	}

	@Override
	public void onMapLongClick(LatLng latLng)
	{
	}

	//Called when the user makes a tap gesture on the map, but only if none of the overlays of the map handled the gesture.
	@Override
	public void onMapClick(LatLng latLng)
	{
		if (floatingActionButton != null)
		{
			floatingActionButton.closeMenu();
		}
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
	public void onCategoryDataReady(CategoryCollection categoryCollection)
	{
	}

	@Override
	public void onPushMessageDataReady(PushMessageCollection newPushMessages)
	{
		pushMessages = newPushMessages;
		//If not already in place, create a background loop that continually updates push messages
		createMessageRetreiveTask();
		// wake up the app when any beacon is seen (you can specify specific id filers in the parameters below)
		if (beaconManager == null)
		{
			beaconManager = BeaconManager.getInstanceForApplication(this);
			beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		}
		//Region region = new Region("com.mapsindoors.boostrapRegion", Identifier.parse("112de479-7a0d-4b75-9bb9-28bd2862ac20"), Identifier.parse("2"), Identifier.parse("2"));
		List<PushMessage> messages = newPushMessages.getPushMessages();
		List<Region> regionList = new ArrayList<>(messages.size());
		if (messages.size() > 0)
		{
			if (cloudMessageMap == null)
			{
				cloudMessageMap = new HashMap<>(messages.size());
			}
			for (PushMessage p : messages)
			{
				String beaconId = p.getBeaconId();
				if (beaconId != null)
				{
					String[] regions = beaconId.split("-");
					String id2 = regions[regions.length - 2];
					String id3 = regions[regions.length - 1];
					String id1 = beaconId.substring(0, beaconId.length() - (id2.length() + id3.length() + 2));
					if (!cloudMessageMap.containsKey(p.getId()))
					{
						CloudMessage m = new CloudMessage();
						m.numberOfTimesShownLeft = p.getMaxPushTimes();
						cloudMessageMap.put(p.getId(), m);
					}
					regionList.add(new Region(beaconId, Identifier.parse(id1), Identifier.parse(id2), Identifier.parse(id3)));
				}
			}
			beaconManager.setBackgroundScanPeriod(1000);
			beaconManager.setBackgroundBetweenScanPeriod(1000);
			beaconManager.setBackgroundMode(true);
			regionBootstrap = new RegionBootstrap(this, regionList);
		}
		//Check if the app was started via a message. If so use that message to start a spe
		if (startedWithMessageId != null)
		{
			PushMessage message = getPushMessage(startedWithMessageId);
			if (message != null)
			{
				//TODO: Add actual intent with the message here. For now just notify the user what happened.
				Toast.makeText(getApplicationContext(), "Started using message: " + message.getId(), Toast.LENGTH_LONG).show();
				//LatLng latLng = new LatLng(message.getGeometry())
				//mMapControl.setMapPosition(new Point(latLng.latitude, latLng.longitude), 19, true);
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Started using unknown messageId: " + startedWithMessageId, Toast.LENGTH_LONG).show();
			}
			startedWithMessageId = null;
		}
	}

	//Creates a background task that continually updates push messages from the server
	private void createMessageRetreiveTask()
	{
		if (backgroundMessageCheck == null)
		{
			backgroundMessageCheck = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					while (true)
					{
						new ThreadUtil().sleep(10 * 60 * 1000);
						mMapControl.loadPushMessagesData();
					}
				}
			});
			backgroundMessageCheck.start();
		}
	}

	private void showMessage(String messageId)
	{
		PushMessage message = null;
		for (PushMessage p : pushMessages.getPushMessages())
		{
			if (p.getId().equalsIgnoreCase(messageId))
			{
				message = p;
				break;
			}
		}
		if (message != null)
		{
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setSmallIcon(R.drawable.start_icon);
			builder.setContentTitle(message.getTitle());
			builder.setContentText(message.getContent());
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			inboxStyle.setBigContentTitle("Event tracker details:");
			inboxStyle.setBigContentTitle(message.getTitle());
			inboxStyle.setSummaryText(message.getContent());
			// Moves the expanded layout object into the notification object.
			builder.setStyle(inboxStyle);
			// This intent is fired when notification is clicked
			PackageManager pm = this.getPackageManager();
			Intent intent = pm.getLaunchIntentForPackage("com.mapsindoors");
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.putExtra("fromNotification", true);
			intent.putExtra("messageId", messageId);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
			// Set the intent that will fire when the user taps the notification.
			builder.setContentIntent(pendingIntent);
			NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			Notification n = builder.build();
			CRC32 crc = new CRC32();
			crc.update(messageId.getBytes());
			int id = (int)crc.getValue();
			mNotificationManager.notify(id, n);
		}
	}

	@Override
	public void onVenueDataReady(final VenueCollection venueCollection)
	{
		venues = venueCollection;
		venueReady = true;
		new Handler(getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				START_POSITION = venues.getDefaultVenue().getPosition();
				mMapControl.selectFloor(venues.getDefaultVenue().getDefaultFloor());
				mMapControl.setMapPosition(START_POSITION, 17, false);
			}
		});
		onDataReady();
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
		solutionReady = true;
		onDataReady();
	}

	@Override
	public void onAppConfigDataReady(AppConfig settings)
	{
		this.settings = settings;
		settingsReady = true;
		onDataReady();
	}

	private void onDataReady()
	{
		if (solutionReady && venueReady && settingsReady && !dataReady)
		{
			dataReady = true;
			new Handler(getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					initializeMenu();
				}
			});
		}
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
			mMenuFragment.init(this, this, mMapControl);
		}
		if (dataReady)
		{
			floatingActionButton = new FloatingAction(this, this, settings);
			new Handler(getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					mainMenuList = mMenuFragment.initMenu(solution, settings, venues, venues.getCurrentVenue().getName());
					if (mainMenuList != null)
					{
						menuInitialized = true;
					}
				}
			});
		}
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
	private synchronized void setUpMapIfNeeded()
	{
		if (initializeCalled)
			return;
		boolean doWork = false;
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null && isLocationAccessGranted)
		{
			initializeCalled = true;
			doWork = true;
		}
		if (doWork)
		{
			final FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fabSearch);
			fab.setAlpha(0f);
			TextView fabText = (TextView)findViewById(R.id.TextAct1);
			fabText.setAlpha(0f);
			// Try to obtain the map from the SupportMapFragment.
			mMapFragment = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.mapfragment));
			final MapsIndoorsActivity activity = this;
			mMapFragment.getMapAsync(new OnMapReadyCallback()
			{
				@Override
				public void onMapReady(GoogleMap googleMap)
				{
					mMap = googleMap;
					if (mMap == null)
					{
						Toast.makeText(getApplicationContext(), "Unable to open Google map. Unable to continue", Toast.LENGTH_LONG).show();
						return;
					}
					try
					{
						//For customizing styles tweak res/raw/style_json.json  https://mapstyle.withgoogle.com/
						boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.style_json));
						if (!success)
						{
							dbglog.Log("Style parsing failed.");
						}
					} catch (Exception e)
					{
						dbglog.Log("Style parsing failed.");
					}
					mMapControl = new MapControl(activity, mMapFragment, mMap);
					mMapControl.showBuildingNames(false);
					mMapControl.setOnDataReadyListener(activity);
					//Select tile size and cache scheme
					mMapControl.setTileSize(512);
					mMapControl.setCacheScheme(MapControl.TILE_CACHESCHEME_PERSISTENT);
					//Initialize the map and set a position and floor so we have a proper starting point
					String sId = getAppSchemeQueryParam("solutionId");
					sId = (sId == null) ? solutionId : sId;
					mMapControl.initMap(sId);
					mMapControl.showUserPosition(true);
					mMapControl.setOnMarkerClickListener(activity);
					//mMapControl.setCurrentPosition(START_POSITION, 0);
					//Set the color of the floorselector
					FloorSelector.DEFAULT_COLOR = ContextCompat.getColor(activity, R.color.white);
					FloorSelector.DEFAULT_COLOR_ACTIVE = ContextCompat.getColor(activity, R.color.primary);
					//Set the listeners you want to use here
					mMap.setOnMapLongClickListener(activity);
					mMap.setOnMapClickListener(activity);
					directionsmenuFragment = (DirectionsmenuFragment)getSupportFragmentManager().findFragmentById(R.id.directionsmenufragment);
					directionsmenuFragment.init(activity, mMap, activity);
					mMapControl.setOnFloorUpdateListener(directionsmenuFragment);
					//Add a position provider in able to track the user's position.
					if (positionProvider == null)
					{
						//Add a position provider in able to track the user's position.
						positionProvider = new GoogleAPIPositionProvider(activity);
					}
					//Telling map control about our provider
					mMapControl.addPositionProvider(positionProvider);
					mMapControl.startPositioning();
					//mMapControl.startTrackPosition();
					mMapControl.showUserPosition(true);
				}
			});
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

	@Override
	public void onBeaconServiceConnect()
	{
		if (beaconPositionProvider != null)
		{
			beaconPositionProvider.onBeaconServiceConnect();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		return false;
	}

	@Override
	public void onMenuShowRoute(Location location)
	{
//		positionProvider.getLatestPosition();
		mMapControl.clearMap();
		Floor floor = mMapControl.getCurrentBuildingFloor();
		LatLng target = mMap.getCameraPosition().target;
		if (floor != null && target != null)
		{
			//Get the latest position from our position providers made for mapcontrol earlier.
			directionsmenuFragment.route(new MPLocation(getCurrentPos(), "Start"), location, TravelMode.TRAVEL_MODE_WALKING, new String[]{}, null, null);
			//Alternative: Use the mapcontrol route for rendering
			//mMapControl.route(new MPLocation(from, "Start"), location, TravelMode.TRAVEL_MODE_WALKING, new String[]{}, null, null);
		}
		//When the menu calls this it's because a direction view is about to be shown. The action button should now be hidden until that view is no longer active.
		floatingActionButton.setActive(false);
		mMenuFragment.closeMenu();
		//Change the top field text to 'search for <name of location>'
		topSearchField.setSearchText(null);
	}

	public Point getCurrentPos()
	{
		LatLng target = mMap.getCameraPosition().target;
		Point result = mMapControl.getCurrentPosition().getPoint();
		//If the latest position is at 0,0 it's not updated yet. Using the camera position instead.
		//If you want a default position before we get any signals from our position providers, use setCurrentPosition() on mapscontrol.
		if (result.lat() == 0 && result.lng() == 0)
		{
			result = new Point(target.latitude, target.longitude, mMapControl.getCurrentFloorIndex());
		}
		return result;
	}

	@Override
	public void onMenuVenueSelect(String venueId)
	{
		if (venues.selectVenue(venueId))
		{
			mMapControl.selectVenue(venues.getCurrentVenue().getVenueId());
			//reinitialize the menu with the new venue settings
			menuInitialized = false;
			initializeMenu();
		}
	}

	@Override
	public void onMenuShowLocation(Location location)
	{
		mMapControl.setMapPosition(location.getPoint(), 19, true);
		List<Location> loc = new ArrayList<>(1);
		loc.add(location);
		mMapControl.selectFloor(location.getFloorIndex());
		mMapControl.displaySearchResults(loc, false);
//		mMapControl.showInfoSnippet(location);
//		mMapControl.displaySingleLocation(location);
		mMenuFragment.closeMenu();
		//Change the top field text to 'search for <name of location>'
		topSearchField.setSearchText(location.getName());
	}

	//Callback from the menu stating that the user wants to search for locations containing a given name.
	//If no name is given we should list the location types instead
	@Override
	public void onMenuSearch(String searchString, boolean finalSearch)
	{
		if (searchString == null || searchString.length() < 1)
		{
			//If the searching string is empty show all types defined in the solution instead.
			isSearching = false;
			pendingSearch = null;
			typeFilter = null;
			categoryFilter = null;
			mMenuFragment.populateMenu();
			mMenuFragment.setExitbuttonActive(false);
			return;
		}
		if (searchPlaces == null)
		{
			searchPlaces = getResources().getString(R.string.search_places);
		}
		if (!searchString.equalsIgnoreCase(searchPlaces))
		{
			//Find the location with the current type and category filters
			findLocations(searchString);
		}
	}

	@Override
	public void onMenuSelect(Object selected, IconTextListAdapter.Objtype objtype)
	{
		if (objtype == IconTextListAdapter.Objtype.CATEGORY)
		{
			//A location type is selected. Find all locations with that category.
			typeFilter = null;
			categoryFilter = (String)selected;
			findLocations("");
			mMenuFragment.setExitbuttonActive(true);
		}
		else if (objtype == IconTextListAdapter.Objtype.LANGUAGE)
		{
			String languageCode = (String)selected;
			mMapControl.setLangugage(languageCode);
			solutionReady = false;
			venueReady = false;
			settingsReady = false;
			dataReady = false;
			menuInitialized = false;
			mMenuFragment.closeMenu();
			mMenuFragment = null;
			mMapControl.initMap(solutionId);
		}
		else if (objtype == IconTextListAdapter.Objtype.TYPE)
		{
			//A location type is selected. Find all locations with that type.
			typeFilter = ((POIType)selected).name;
			categoryFilter = null;
			findLocations("");
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
							mMenuFragment.openLocationMenu(location);
							mMenuFragment.openMenu();
						}
					});
				}
			});
			locationsProvider.getLocationsDetailsAsync(solutionId, location.getId());
		}
	}

	//Finds and shows a locations based on a search string and optionally with optional type and category filters (can be null)
	private void findLocations(String searchString)
	{
		final LocationQuery query = new LocationQuery();
		query.arg = solutionId;
		query.orderBy = LocationQuery.OrderBy.RELEVANCE;
		query.max = 50;
		query.near = venues.getCurrentVenue().getPosition();
		if (searchString != null && searchString.length() > 0)
			query.setQuery(searchString);
		if (typeFilter != null)
		{
			List<String> list = new ArrayList<>(1);
			list.add(typeFilter);
			query.types = list;
		}
		if (categoryFilter != null)
		{
			List<String> list = new ArrayList<>(1);
			list.add(categoryFilter);
			query.categories = list;
		}
		mMenuFragment.setExitbuttonActive(true);
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
						locationsProvider.getLocationsUsingQueryAsync(query, new Locale(mMapControl.getLangugage()));
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
								LatLng target = mMap.getCameraPosition().target;
								//Get the latest position from our position providers made for mapcontrol earlier.
								Point from = mMapControl.getCurrentPosition().getPoint();
								//If the latest position is at 0,0 it's not updated yet. Using the camera position instead.
								//If you want a default position before we get any signals from our position providers, use setCurrentPosition() on mapscontrol.
								if (from.lat() == 0 && from.lng() == 0)
								{
									from = new Point(target.latitude, target.longitude);
								}
								for (Location location : locations)
								{
									if (location != null)
									{
										String typeName = location.getType();
										Bitmap bm = (Bitmap)locationTypeImages.get(typeName);
										Venue venue = venues.getVenue(location.getStringProperty("venue"));
										String venueName = (venue == null) ? "" : venue.getName();
										String building = location.getStringProperty("building");
										building = (building == null) ? "" : building;
										String floor = ", Level " + location.getFloorName();
										String SubText = (building.equalsIgnoreCase(venueName)) ? venueName + floor : venueName + ", " + building + floor;
										double airDistance = location.getPoint().distanceTo(from);
										if (bm != null)
										{
											elements.add(new IconTextElement(location.getName(), SubText, airDistance, bm, location, IconTextListAdapter.Objtype.LOCATION));
										}
										else
										{
											elements.add(new IconTextElement(location.getName(), SubText, airDistance, com.mapspeople.mapssdk.R.drawable.step, location, IconTextListAdapter.Objtype.LOCATION));
										}
									}
								}
								adapter.setList(elements);
								isSearching = false;
								mMenuFragment.changeWaitStatus(false);
								if (pendingSearch != null)
								{
									String searchString = pendingSearch;
									pendingSearch = null;
									onMenuSearch(searchString, true);
								}
							}
						}
					});
				}

				@Override
				public void onLocationDetailsReady(Location location) {}
			});
			//Do the actual search call
			locationsProvider.getLocationsUsingQueryAsync(query, new Locale(mMapControl.getLangugage()));
		}
		else
		{
			//Already waiting for a response. Store the query for later use.
			pendingSearch = searchString;
		}
	}

	@Override
	public void onFABSelect(final String selectedCategory)
	{
		topSearchField.setSearchText(selectedCategory);
		LocationQuery query = new LocationQuery();
		query.arg = solutionId;
		query.near = getCurrentPos();
		query.radius = 1000;
		query.max = 10;
		query.orderBy = LocationQuery.OrderBy.RELEVANCE;
		List<String> list = new ArrayList<>(1);
		list.add(selectedCategory);
		query.categories = list;
		final MPLocationsProvider locationsProvider = new MPLocationsProvider(this);
		final Context context = this;
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
						if (locations.size() > 0)
						{
							mMapControl.selectFloor(locations.get(0).getFloorIndex());
							mMapControl.displaySearchResults(locations, true);
						}
						else
						{
							Toast.makeText(context, getString(R.string.No_POIs_found) + " " + selectedCategory, Toast.LENGTH_LONG).show();
						}
					}
				});
			}

			@Override
			public void onLocationDetailsReady(Location location) {}
		});
		//Do the actual search call
		locationsProvider.getLocationsUsingQueryAsync(query, new Locale(mMapControl.getLangugage()));
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
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	@Override
	public boolean onMarkerClick(Marker marker)
	{
		mMapControl.clearMap();
		topSearchField.setSearchText(null);
		String title = marker.getTitle();
		if (title != null)
		{
			LatLng latLng = marker.getPosition();
			Point position = new Point(latLng.latitude, latLng.longitude, mMapControl.getCurrentFloorIndex());
			//Find the location for the given marker
			final LocationQuery query = new LocationQuery();
			query.arg = solutionId;
			query.setQuery(title);
			query.radius = 1;
			query.near = position;
			//Set up a location listener and make a location search call
			final MPLocationsProvider locationsProvider = new MPLocationsProvider(this);
			locationsProvider.setOnLocationsReadyListener(new OnLocationsReadyListener()
			{
				@Override
				public void onLocationsReady(final List<Location> locations)
				{
					if (locations == null || !dataReady)
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
						locationsProvider.getLocationsUsingQueryAsync(query, new Locale(mMapControl.getLangugage()));
						return;
					}
					new Handler(getMainLooper()).post(new Runnable()
					{
						@Override
						public void run()
						{
							if (locations.size() == 1)
							{
								onMenuSelect(locations.get(0), IconTextListAdapter.Objtype.LOCATION);
							}
						}
					});
				}

				@Override
				public void onLocationDetailsReady(Location location) {}
			});
			//Do the actual search call
			locationsProvider.getLocationsUsingQueryAsync(query, new Locale(mMapControl.getLangugage()));
			//Set the camera to the marker and show info
			mMapControl.setMapPosition(new Point(latLng.latitude, latLng.longitude), 19, true);
			marker.showInfoWindow();
		}
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
	//Altbeacon BootstrapNotifier methods

	@Override
	public void didEnterRegion(Region region)
	{
		dbglog.Log("didEnterRegion" + region.toString());
		if (region.getId1() != null)
		{
			if (pushMessages != null)
			{
				List<PushMessage> msgList = this.pushMessages.getPushMessages();
				for (int i = 0; i < msgList.size(); i++)
				{
					PushMessage p = msgList.get(i);
					String UUID = region.getId1().toString() + "-" + region.getId2().toString() + "-" + region.getId3().toString();
					if (p != null && p.getBeaconId().equalsIgnoreCase(UUID))
					{
						{
							String messageId = p.getId();
							if (cloudMessageMap.containsKey(messageId))
							{
								CloudMessage m = cloudMessageMap.get(p.getId());
								double timeDelta = ((double)(System.currentTimeMillis() - m.lastShown)) / 1000;
								if (m.numberOfTimesShownLeft > 0 && p.getPushInterval() < timeDelta)
								{
									m.numberOfTimesShownLeft--;
									m.lastShown = System.currentTimeMillis();
									showMessage(messageId);
								}
							}
						}
					}
				}
			}
//			// This call to disable will make it so the activity below only gets launched the first time a beacon is seen (until the next time the app is launched)
//			// if you want the Activity to launch every single time beacons come into view, remove this call.
//			regionBootstrap.disable();
//			Intent intent = new Intent(this, MapsIndoorsActivity.class);
//			// IMPORTANT: in the AndroidManifest.xml definition of this activity, you must set android:launchMode="singleInstance" or you will get two instances
//			// created when a user launches the activity manually and it gets launched from here.
//			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			this.startActivity(intent);
		}
	}

	@Override
	public void didExitRegion(Region region)
	{
		dbglog.Log("didExitRegion" + region.toString());
	}

	@Override
	public void didDetermineStateForRegion(int i, Region region)
	{
		dbglog.Log("didDetermineStateForRegion" + region.toString());
	}

	public class ResultActivity extends AppCompatActivity
	{

		public ResultActivity()
		{
			super();
		}
	}

	public class CloudMessage
	{
		public int numberOfTimesShownLeft;
		public long lastShown = -1;
	}

	public String getSolutionId()
	{
		return solutionId;
	}


}
