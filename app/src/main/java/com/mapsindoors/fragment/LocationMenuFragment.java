package com.mapsindoors.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.mapsindoors.IconTextElement;
import com.mapsindoors.IconTextListAdapter;
import com.mapsindoors.listener.DirectionsMenuListener;
import com.mapsindoors.listener.MenuListener;
import com.mapsindoors.R;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.DataField;
import com.mapspeople.models.Floor;
import com.mapspeople.models.Location;
import com.mapspeople.models.Point;
import com.mapspeople.routing.MPRoutingProvider;
import com.mapspeople.routing.OnRouteResultListener;
import com.mapspeople.routing.Route;
import com.mapspeople.routing.TravelMode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class LocationMenuFragment extends Fragment implements DirectionsMenuListener
{
	private ListView mainMenuList;
	private Context context;
	private MenuListener menuListener;
	private com.mapspeople.models.Location location;
	private boolean isKeyboardActive;
	private LinearLayout headerView;
	private TextView titleTextView;
	private TextView descriptionTextView;
	private IconTextListAdapter myAdapter;
	private MapControl mapControl;
	private DirectionsfullmenuFragment mDirectionsfullmenuFragment;

	public LocationMenuFragment()
	{
		super();
		isKeyboardActive = false;
	}

	public void init(Context context, MenuListener menuListener, MapControl mapControl)
	{
		this.context = context;
		this.menuListener = menuListener;
		this.mapControl = mapControl;
		descriptionTextView = new TextView(context);
		descriptionTextView.setTextColor(Color.parseColor("#FF000000"));
		descriptionTextView.setAlpha(1f);
		View view = ((Activity)context).findViewById(R.id.locationmenufragment);
		view.setVisibility(View.GONE);
		mDirectionsfullmenuFragment = (DirectionsfullmenuFragment)((FragmentActivity)context).getSupportFragmentManager().findFragmentById(R.id.directionsfullmenufragment);
		mDirectionsfullmenuFragment.init(context, this, mapControl);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if ( headerView == null)
		{
			headerView = (LinearLayout)inflater.inflate(R.layout.locationmenu, container);
		}
		return headerView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStart()
	{
		super.onStart();

	}

	@Override
	public void onResume()
	{
		super.onResume();
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
	}

	public void openMenu()
	{
		View view = ((Activity)context).findViewById(R.id.locationmenufragment);
		view.setVisibility(View.VISIBLE);
		view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.menu_enter));
	}

	public void closeMenu()
	{
		View view = ((Activity)context).findViewById(R.id.locationmenufragment);
		view.setVisibility(View.GONE);
		view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.menu_exit));
	}

	public ListView initMenu()
	{
		titleTextView = (TextView)((Activity)context).findViewById(R.id.locationTitleId);
		ImageButton menuButton = (ImageButton) headerView.findViewById(R.id.locations_backbutton);
		menuButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				closeMenu();
			}
		});
		Button showOnMapButton = (Button) headerView.findViewById(R.id.locations_gotobutton);
		showOnMapButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				menuListener.onMenuShowLocation(location);
			}
		});
		final Button showRouteButton = (Button) headerView.findViewById(R.id.locations_routebutton);
		showRouteButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//if ( showRouteButton.getText().equals(context.getString(R.string.directions)))
				if(true)
				{
					//Too far away to just show the route directly. Open the directions menu.
					mDirectionsfullmenuFragment.open(location);

				}
				else
				{
					//Just show the route. Let the listener know this.
					menuListener.onMenuShowRoute(location);
				}
			}
		});

		buttonColorSetup();
		mainMenuList = (ListView) headerView.findViewById(R.id.itemlist);
		mainMenuList.addHeaderView(descriptionTextView);
		mainMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener()
											{
												@Override
												public void onItemClick(AdapterView<?> parent, View view, int position, long id)
												{
													//The header will count as 1 item. Disregard that.
													position -= 1;
													if (id >= 0)
													{
														//As we have added a header the returned object will be a HeaderViewListAdapter with out IconTextListAdapter wrapped inside. Unwrap it:
														IconTextListAdapter adapter = ((IconTextListAdapter)((HeaderViewListAdapter)parent.getAdapter()).getWrappedAdapter());
														menuListener.onMenuSelect(adapter.getItemObj(position), adapter.getObjType(position));
													}
												}
											}
		);
		return mainMenuList;
	}

	public void buttonColorSetup()
	{
		final Button showRouteButton = (Button) headerView.findViewById(R.id.locations_routebutton);
		showRouteButton.getBackground().setColorFilter(ContextCompat.getColor(context, R.color.accent), PorterDuff.Mode.MULTIPLY);
		Button showOnMapButton = (Button) headerView.findViewById(R.id.locations_gotobutton);
		showOnMapButton.getBackground().setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.DST);
	}

	public void setLocation( Location location, Bitmap bitmap, Bitmap logo )
	{
		setLocation( location, bitmap, logo, null);
	}

	private void setLocation( final Location location, final Bitmap bitmap, final Bitmap logo, final Route route )
	{
		this.location = location;
		((Button)headerView.findViewById(R.id.locations_routebutton)).setText(context.getString(R.string.directions));
		ImageView logoImage = (ImageView) headerView.findViewById(R.id.imageViewLogo);
		logoImage.setImageBitmap(logo);
		descriptionTextView.setText(location.getStringProperty("description"));
		titleTextView.setText(location.getName());
		String openinghours = getFieldValue("openinghours", location);
		String phone = getFieldValue("phone", location);
		String website = getFieldValue("website", location);
		String imageUrl = getFieldValue("imageurl", location);
		if ( imageUrl == null || imageUrl.length() == 0 )
		{
			setTopImageBitmap((ImageView) headerView.findViewById(R.id.topimage), bitmap);
		}
		else
		{
			loadTopImage(imageUrl);
		}
		ArrayList<IconTextElement> elements = new ArrayList<>();
		if ( route == null )
		{
			double distance = location.getPoint().distanceTo(mapControl.getCurrentPosition().getPoint());
			boolean isWalking = distance < 5000;
			int vehicleIcon = isWalking ? R.drawable.locationmenu_walk : R.drawable.ic_directions_car;
			addElement(elements, getString(R.string.finding_route), vehicleIcon, IconTextListAdapter.Objtype.ROUTE);

			//Location data aquired. Find the route from current position to that location.
			MPRoutingProvider routingProvider = new MPRoutingProvider();
			routingProvider.setTravelMode(isWalking ? TravelMode.TRAVEL_MODE_WALKING : TravelMode.TRAVEL_MODE_DRIVING);
			routingProvider.setOnRouteResultListener(new OnRouteResultListener()
			{
				@Override
				public void onRouteResult(Route r)
				{
					final Route route = r;
					new Handler(context.getMainLooper()).post(new Runnable()
					{
						@Override
						public void run()
						{
							setLocation(location, bitmap, logo, route);
						}
					});
				}

				@Override
				public void onRoutingInfoResult(Object info)
				{
				}
			});
			Point from = mapControl.getCurrentPosition().getPoint();
			from.setZ(mapControl.getCurrentBuildingFloor().getZIndex());

			//If the latest position is at 0,0 it's not updated yet. Using the camera position instead.
			//If you want a default position before we get any signals from our position providers, use setCurrentPosition() on mapscontrol.
			if (from.lat() == 0 && from.lng() == 0)
			{
				LatLng target = mapControl.getMap().getCameraPosition().target;
				Floor floor = mapControl.getCurrentBuildingFloor();
				from = new Point(target.latitude, target.longitude, floor.getZIndex());
			}
			routingProvider.query(from, location.getPoint());
		}
		else
		{
			Location currentLocation = mapControl.getCurrentPosition();
			double distance = location.getPoint().distanceTo(currentLocation.getPoint());
			double WALKSPEED = 1.3f; //Average walk speed in m/s. (copenhagen it's 1,73 m/s; Bern (Switzerland): 1,05 m/s ) http://www.richardwiseman.com/quirkology/pace_home.htm
			boolean isWalking = (distance < 5000);
			double duration = route.getDuration();
			int vehicleIcon = isWalking ? R.drawable.locationmenu_walk : R.drawable.ic_directions_car;
			String routeText = getTimeStr(duration);
			routeText = isWalking ? routeText + " " + getString(R.string.by_walk) : routeText + " " + getString(R.string.by_drive);
			addElement(elements, routeText, vehicleIcon, IconTextListAdapter.Objtype.ROUTE);
		}

		String buildingText = location.getStringProperty("building");
		String level = getString(R.string.level);
		String locationPlace = buildingText != null ? getString(R.string.building) + " " + buildingText + " "+level+" " + location.getFloorName() : level + " " + location.getFloorName();

		addElement(elements, openinghours, R.drawable.locationmenu_clock, IconTextListAdapter.Objtype.OPENINGHOURS);
		addElement(elements, phone, R.drawable.locationmenu_phone, IconTextListAdapter.Objtype.PHONE);
		addElement(elements, website, R.drawable.locationmenu_www, IconTextListAdapter.Objtype.URL);
		addElement(elements, locationPlace, R.drawable.locationmenu_city, IconTextListAdapter.Objtype.PLACE);
		myAdapter = new IconTextListAdapter(context);
		myAdapter.setTint("@color/primary");
		myAdapter.setList(elements);
		mainMenuList.setAdapter(myAdapter);
	}

	private String getTimeStr(double seconds)
	{
		int minutes = (int) ((seconds / (60)) % 60);
		int hours   = (int) ((seconds / (60*60)) % 24);
		String minTxt = context.getString(R.string.minutes);
		String hourTxt = context.getString(R.string.hour);
		return hours > 0 ? ""+hours+" "+hourTxt+" "+minutes+" "+minTxt : ""+minutes+" "+minTxt;
	}

	public Location getLocation()
	{
		return this.location;
	}

	private void addElement( ArrayList<IconTextElement> elements, String text, Integer imgId, IconTextListAdapter.Objtype type)
	{
		if ( text != null && text.length() > 0 )
			elements.add(new IconTextElement(text, imgId, text, type ));
	}

	private String getFieldValue(String fieldName, com.mapspeople.models.Location location)
	{
		DataField data = location.getField(fieldName);
		return data == null ? "" : data.getValue();
	}

	//Gets an image using an iconURL and sets the icon to the resulting image.
	public void loadTopImage(final String imageURL)
	{
		final ImageView image = (ImageView) headerView.findViewById(R.id.topimage);
		if ( imageURL != null)
		{
			//ImageUrl seems to contain a link. Load it in a new thread and change the image when loaded.
			new Thread()
			{
				@Override
				public void run()
				{
					try
					{
						Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(imageURL).getContent());
						if (bitmap != null)
						{
							setTopImageBitmap(image, bitmap);
						}
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	private void setTopImageBitmap(final ImageView image, final Bitmap bitmap)
	{
		float dpi = 320;
		final float scale = (dpi / 600f);

		final Bitmap loadedImage = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()* scale), (int)(bitmap.getHeight() * scale), true);
		//Only allowed to change views from the thread that originally created it - aka the main loop:
		new Handler(context.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				image.setImageBitmap(loadedImage);
			}
		});
	}

	//Called when the directions menu closes.
	@Override
	public void onDirectionsMenuClose()
	{
	}

}