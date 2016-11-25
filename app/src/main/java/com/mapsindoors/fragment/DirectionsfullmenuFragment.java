package com.mapsindoors.fragment;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.mapsindoors.MapsIndoorsActivity;
import com.mapsindoors.listener.DirectionsMenuListener;
import com.mapsindoors.R;
import com.mapsindoors.listener.LocationFoundListener;
import com.mapspeople.data.LocationQuery;
import com.mapspeople.data.LocationsProvider;
import com.mapspeople.data.MPLocationsProvider;
import com.mapspeople.data.OnLocationsReadyListener;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.mapcontrol.OnFloorUpdateListener;
import com.mapspeople.models.Building;
import com.mapspeople.models.Floor;
import com.mapspeople.models.Location;
import com.mapspeople.models.LocationProperty;
import com.mapspeople.models.MPLocation;
import com.mapspeople.models.Point;
import com.mapspeople.routing.DirectionsRenderer;
import com.mapspeople.routing.MPDirectionsRenderer;
import com.mapspeople.routing.MPRoutingProvider;
import com.mapspeople.routing.OnLegSelectedListener;
import com.mapspeople.routing.OnRouteResultListener;
import com.mapspeople.routing.Route;
import com.mapspeople.routing.RouteLeg;
import com.mapspeople.routing.RouteStep;
import com.mapspeople.routing.RoutingProvider;
import com.mapspeople.routing.TravelMode;
import com.mapspeople.util.Convert;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DirectionsfullmenuFragment extends Fragment implements OnLegSelectedListener, OnFloorUpdateListener
{
	private Context context;
	private DirectionsMenuListener menuListener;
	private boolean isKeyboardActive;
	private LinearLayout overlayLayout;
	private TextView descriptionTextView;
	private MapControl mapControl;
	private View mainView;
	private boolean isWaiting = false;
	// Rendering object used to draw routes on top of the google map.
	private DirectionsRenderer directionsRenderer;
	private Route currentRoute;
	private int currentSelectedLegId;
	private int maxLegIndex;
	private int currentLegIndex;
	private DirectionsfullmenuSearchFragment mDirectionsfullmenuSearchFragment;
	private int[] actionFileId = { R.drawable.subfab_elevator, R.drawable.subfab_steps, R.drawable.subfab_steps, R.drawable.subfab_steps };
	private String[] actionName = {"elevator","escalator","steps","travelator"};
	private HashMap<String, String> contextDescriptions;
	private Location origin;
	private Location destination;
	private enum vehicle {walk, bicycle, transit, car}
	private vehicle currentVehicle = vehicle.walk;

	public DirectionsfullmenuFragment()
	{
		super();
		isKeyboardActive = false;
	}

	public void init(final Context context, DirectionsMenuListener menuListener, final MapControl mapControl)
	{
		this.context = context;
		final MapsIndoorsActivity activity = (MapsIndoorsActivity)context;
		this.menuListener = menuListener;
		this.mapControl = mapControl;
		Point startPos = activity.getCurrentPos();
		String estimated_position = context.getString(R.string.you_are_here);
		origin = new MPLocation(startPos, estimated_position);

		descriptionTextView = new TextView(context);
		descriptionTextView.setTextColor(Color.parseColor("#FF000000"));
		descriptionTextView.setAlpha(1f);
		contextDescriptions = new HashMap<>();
		contextDescriptions.put("OutsideOnVenue", activity.getResources().getString(R.string.context_OutsideOnVenue) );
		contextDescriptions.put("InsideBuilding", activity.getResources().getString(R.string.context_InsideBuilding) );
		mainView = ((Activity)context).findViewById(R.id.directionsfullmenufragment);
		mainView.setVisibility(View.GONE);
		int primaryColor = ContextCompat.getColor(context, R.color.primary);
		int accentColor = ContextCompat.getColor(context, R.color.accent);
		directionsRenderer = new MPDirectionsRenderer(context, this);
		directionsRenderer.setPrimaryColor(primaryColor);
		directionsRenderer.setAccentColor(accentColor);
		directionsRenderer.setTextColor(ContextCompat.getColor(context, R.color.white));
		initVehicleSelector();

		mainView.findViewById(R.id.imageExit).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view)
					{
						close();
					}
				}
		);
		mainView.findViewById(R.id.showonmap).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view)
					{
						//First close the routing from the full menu. The small one will show the route now.
						closeRouting();
						DirectionsmenuFragment directionsmenuFragment = (DirectionsmenuFragment)activity.getSupportFragmentManager().findFragmentById(R.id.directionsmenufragment);
						directionsmenuFragment.setStartEndLocations(origin, destination);
						directionsmenuFragment.route(currentRoute);
						//Close the side menu
						MenuFragment mMenuFragment = (MenuFragment)activity.getSupportFragmentManager().findFragmentById(R.id.menufragment);
						mMenuFragment.closeMenu();
					}
				}
		);
		final DirectionsfullmenuFragment fragment = this;
		Switch avoidSwitch = (Switch)mainView.findViewById(R.id.switchAvoidStairs);
		avoidSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b)
			{
				fragment.updateList();
			}
		});
		mainView.findViewById(R.id.imageSwitchSrcDst).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View ButtonView)
					{
						//swap the location and update the list (with the new route)
						fragment.swapLocations();
						fragment.updateList();
					}
				}
		);
		//Set up a location search object so the user can search for locations
		mDirectionsfullmenuSearchFragment = (DirectionsfullmenuSearchFragment)((FragmentActivity)context).getSupportFragmentManager().findFragmentById(R.id.directionsfullmenuSearchfragment);
		mDirectionsfullmenuSearchFragment.init(context, mapControl);
		TextView fromView = (TextView)mainView.findViewById(R.id.editTextFrom);
		fromView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				mDirectionsfullmenuSearchFragment.setActive(true);
				mDirectionsfullmenuSearchFragment.SetOnLocationFoundhandler(new LocationFoundListener()
				{
					@Override
					public void onLocationsFound(Location location)
					{
						//User selected a location.
						origin = location;
						updateList();
						mDirectionsfullmenuSearchFragment.setActive(false);
					}
				});
			}
		});

		TextView toView = (TextView)mainView.findViewById(R.id.editTextTo);
		toView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				mDirectionsfullmenuSearchFragment.setActive(true);
				mDirectionsfullmenuSearchFragment.SetOnLocationFoundhandler(new LocationFoundListener()
				{
					@Override
					public void onLocationsFound(Location location)
					{
						//User selected a location.
						destination = location;
						updateList();
						mDirectionsfullmenuSearchFragment.setActive(false);
					}
				});
			}
		});

	}

	//Clears the route list, calculate a new route and show that.
	private void updateList()
	{
		new Handler(context.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				resetLegs();
				//Update the textviews (to and from)
				TextView fromView = (TextView)mainView.findViewById(R.id.editTextFrom);
				fromView.setText(getOrigin().getName());
				TextView toView = (TextView)mainView.findViewById(R.id.editTextTo);
				toView.setText(getDestination().getName());
				changeWaitStatus(true);
				String[] avoid = avoidStairs() ? new String[]{"stairs"} : new String[]{};
				route(getOrigin(), getDestination(), getTravelMode(), avoid, null, null);
			}
		});
	}

	private void swapLocations()
	{
		Location swap = origin;
		origin = destination;
		destination = swap;
	}

	private Location getOrigin()
	{
		return origin;
	}

	private Location getDestination()
	{
		return destination;
	}

	private boolean avoidStairs()
	{
		Switch avoidSwitch = (Switch)mainView.findViewById(R.id.switchAvoidStairs);
		return avoidSwitch.isChecked();
	}

	public void open( Location destination )
	{
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
		this.origin.setPoint(from);
		origin.setName(getString(R.string.estimated_position_near));
		this.destination = destination;
		double distance = this.origin.getPoint().distanceTo(destination.getPoint());
		if ( distance > 5000 )
		{
			currentVehicle = vehicle.car;
			updateVehicleSelector();
		}
		updateList();
		mainView.setVisibility(View.VISIBLE);
	}

	public void close()
	{
		closeRouting();
		mainView.setVisibility(View.GONE);
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
		if ( overlayLayout == null)
		{
			overlayLayout = (LinearLayout)inflater.inflate(R.layout.directionsfullmenu, container);
		}
		return overlayLayout;
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
	public void onHiddenChanged(boolean hidden)
	{
	}

	public void route(final Location origin, final Location destination, final String travelMode, String[] avoids, Calendar departure, Calendar arrival)
	{
		//TODO: get a location description based on the startPos
		changeWaitStatus(true);
		closeRouting();
		if (origin == null || destination == null)
		{
			return;
		}
		if (this.currentRoute != null)
		{
			this.currentRoute.removeFromMap();
		}

		String destinationName = " " + destination.getProperty("name").toString();
		TextView textView = (TextView)((Activity)context).findViewById(R.id.directions_toolbar_text);
		textView.setText(getResources().getString(R.string.directions_routeto) + destinationName);
		TextView cancelView = (TextView)((Activity)context).findViewById(R.id.directions_toolbar_cancel);
		final MenuFragment menuFragment = (MenuFragment)((FragmentActivity)context).getSupportFragmentManager().findFragmentById(R.id.menufragment);
		cancelView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//setActive(false);
				MapsIndoorsActivity activity = (MapsIndoorsActivity)context;
				//Find and close the directions menu (small one) and open the menu again - presenting the full directions menu again.
				DirectionsmenuFragment directionsmenuFragment = (DirectionsmenuFragment)activity.getSupportFragmentManager().findFragmentById(R.id.directionsmenufragment);
				directionsmenuFragment.closeRouting();
				directionsmenuFragment.setActive(false);
				menuFragment.openMenu();
			}
		});
		RoutingProvider routingProvider = new MPRoutingProvider();
		routingProvider.setOnRouteResultListener(new OnRouteResultListener()
		{
			@Override
			public void onRouteResult(final Route route)
			{
				//final MapControl _self = this;
				new Handler(context.getMainLooper()).post(new Runnable()
				{
					@Override
					public void run()
					{
						currentRoute = route;
						directionsRenderer.setRoute(route);
						directionsRenderer.setMap(mapControl.getMap());
						directionsRenderer.setAlpha(255);
						currentSelectedLegId = 0;
						maxLegIndex = route.getRouteLegs().size()-1;
						render(route);
						changeWaitStatus(false);
					}
				});
			}

			@Override
			public void onRoutingInfoResult(Object info)
			{
			}
		});
		routingProvider.setTravelMode(travelMode);
		routingProvider.clearRouteRestrictions();
		if (avoids != null)
		{
			for (String avoid : avoids)
			{
				routingProvider.addRouteRestriction(avoid);
			}
		}
		if (travelMode.equalsIgnoreCase(TravelMode.TRAVEL_MODE_TRANSIT))
		{
			if (arrival != null)
			{
				routingProvider.setDateTime(arrival, false);
			}
			else if (departure != null)
			{
				routingProvider.setDateTime(departure, true);
			}
		}
		routingProvider.query(origin.getPoint(), destination.getPoint());
	}

	/**
	 * Close the current routing control.
	 */
	public void closeRouting()
	{
		if (directionsRenderer != null)
		{
			directionsRenderer.clear();
		}
		if (mapControl != null)
		{
			mapControl.clearMap();
		}
		if ( menuListener != null )
		{
			menuListener.onDirectionsMenuClose();
		}
	}

	//Populate the directions view with the legs from a route
	private void render( Route route )
	{
		resetLegs();
		final List<RouteLeg> legs = route.getRouteLegs();
		View forground = overlayLayout.findViewById(R.id.directionslayout_forground);
		View background = overlayLayout.findViewById(R.id.directionslayout_background);
		forground.setVisibility(View.VISIBLE);
		background.setVisibility(View.VISIBLE);
		String estimated_position_near = context.getString(R.string.estimated_position_near);
		boolean isOriginMyPos = origin.getName().contains(estimated_position_near);
//		String srcDesc = isOriginMyPos ? estimated_position_near : origin.getName();
		String srcDesc = origin.getName();
		int srcIcon = isOriginMyPos ? R.drawable.mylocation : R.drawable.start_icon;
		boolean isDestinationMyPos = destination.getName().contains(estimated_position_near);
		String dstDesc;
		int dstIcon;
		String speachString = "";
		for (int i = 0; i < legs.size(); i++)
		{
			RouteStep currentFirstStep = legs.get(i).getSteps().get(0);
			RouteStep currentLastStep = legs.get(i).getSteps().get(legs.get(i).getSteps().size()-1);
			RouteStep nextFirstStep = (i+1 < legs.size()) ? legs.get(i+1).getSteps().get(0) : null;
			srcIcon = (i == 0) ? srcIcon : R.drawable.empty;
			dstIcon = R.drawable.empty;

			if ( nextFirstStep == null || i == legs.size()-1 )
			{
				srcDesc = getStepInfo(currentLastStep, currentLastStep);
				dstDesc = isDestinationMyPos ? destination.getName() : context.getString(R.string.destination);
				srcIcon = R.drawable.empty;
				dstIcon = isDestinationMyPos ? R.drawable.mylocation : R.drawable.dest_icon;
				addLeg(srcDesc, srcIcon, dstDesc, dstIcon, i, true);
				speachString += srcDesc + ". " + dstDesc + ". ";
			}
			else
			{
				//If the level of the last step of this leg differs from the first step of the next leg, take a note.
				final int startLevel = currentFirstStep.getEndPoint().getZIndex();
				int endLevel = nextFirstStep.getEndPoint().getZIndex();
				boolean isLevelChange = (startLevel != endLevel);
				String highway = nextFirstStep.getHighway();
				dstIcon = R.drawable.empty;
				if (isLevelChange && highway != null)
				{
					for (int idx = 0; idx < actionName.length; idx++)
					{
						if ( highway.equalsIgnoreCase(actionName[idx]))
						{
							dstIcon = actionFileId[idx];
							break;
						}
					}
				}
//				steps = legs.get(i).getSteps();
				srcDesc = ( i > 0 ) ? getStepInfo(currentFirstStep, currentLastStep) : srcDesc;
				dstDesc = getStepInfo(currentLastStep, nextFirstStep);
				//dstDesc = steps.size() > 1 ? steps.get(1).getHtmlInstructions() : legs.get(i).getEndAddress();
				//boolean lastEntry = (i >= legs.size()-1);
				//dstDesc = (!lastEntry) ? "Walk " + legs.get(i).getDistance() + " meters" : legs.get(i).getEndAddress();
				addLeg(srcDesc, srcIcon, dstDesc, dstIcon, i, false);
				speachString += srcDesc + ". " + dstDesc + ". ";
				srcIcon = dstIcon;
			}
		}
//		speakHelper.speak(speachString);
		setActive(true);
	}

	private String getStepInfo(RouteStep step, RouteStep nextStep )
	{
		String result = ""; //step.getHtmlInstructions();
		if ( nextStep != null )
		{
			if ( nextStep.getAbutters().length() > 0 && step.getAbutters().length() > 0 && !nextStep.getAbutters().equalsIgnoreCase(step.getAbutters()) )
			{
				//the context is about to change
				if (step.getAbutters().equalsIgnoreCase("InsideBuilding") && nextStep.getAbutters().equalsIgnoreCase("OutsideOnVenue"))
				{
					result = getString(R.string.walk_outside);
				}
				else if (step.getAbutters().equalsIgnoreCase("OutsideOnVenue") && nextStep.getAbutters().equalsIgnoreCase("InsideBuilding"))
				{
					result = getString(R.string.walk_inside);
				}
			}
//			else if (nextStep.getEndPoint().z() > step.getEndPoint().z())
//			{
//				result = "Walk up";
//			}
//			else if (nextStep.getEndPoint().z() < step.getEndPoint().z())
//			{
//				result = "Walk down";
//			}
		}
		if ( result.length() < 1)
		{
			//normal case: write level for the current step
			result = step.getEndFloorname();
			if ( result == null || result.length() < 1 )
			{
				result = "Level " + step.getEndPoint().getZIndex();
			}
			else
			{
				result = "Level " + result;
			}
		}
		return result;
	}

	//Adds a leg to the (buttom) direction menu
	//If it's selected draw it with alpha 1 - otherwise render it a bit transparent.
	//If it's the first icon draw 'mylocation' in full size and ignore the given image
	//If it's the last icon dont connect it to the next leg (thus don't draw the 'dotted line' after the last icon).
	private void addLeg(String srcDesc, int srcIcon, String dstDesc, int dstIcon, final int legIndex, boolean lastEntry)
	{
		if ( srcDesc == null )
		{
			srcDesc = "";
		}
		if ( dstDesc == null )
		{
			dstDesc = "";
		}
		int primaryColor = ContextCompat.getColor(context, R.color.primary);
		LinearLayout forground = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_forground);
		LinearLayout background = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_background);
		int viewheight = 128;
		int topMargin = Convert.getPixels(viewheight * 0.75f);
		background.setPadding(0,topMargin,0,0);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout forgroundItem = (LinearLayout)inflater.inflate(R.layout.directionsfullmenu_twolineitem, null, true);
		ImageButton bFrom = (ImageButton)forgroundItem.findViewById(R.id.fromIconImageButton);
		bFrom.setImageResource(srcIcon);
		ImageButton bTo = (ImageButton)forgroundItem.findViewById(R.id.toIconImageButton);
		bTo.setImageResource(dstIcon);
		ImageView blueLine = (ImageView)forgroundItem.findViewById(R.id.imageViewBlueLine);
		TextView textFrom = (TextView)forgroundItem.findViewById(R.id.fromEditText);
		TextView textTo = (TextView)forgroundItem.findViewById(R.id.toEditText);
		textFrom.setText(Html.fromHtml(srcDesc));
		textTo.setText(Html.fromHtml(dstDesc));

		//If this leg is selected it should be alpha 1.0
		float alpha = (legIndex == currentSelectedLegId) ? 1.0f : 0.5f;
		blueLine.setAlpha(alpha);

		forgroundItem.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onLegSelected(legIndex);
			}
		});
		forground.addView(forgroundItem);

		//Create the dotted line in the background unless this is the last entry
		if (!lastEntry)
		{
			ImageView line = new ImageView(context);
			line.setImageResource(R.drawable.dottedline_v);
			line.setScaleType(ImageView.ScaleType.FIT_CENTER);
			line.setColorFilter(primaryColor);
			line.setBackgroundResource(android.R.color.transparent);
			line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,Convert.getPixels(64)));
			int leftmargin = Convert.getPixels(25);
			line.setPadding(leftmargin,0,0,0);
			background.addView(line);
			ImageView space = new ImageView(context);
			space.setImageResource(R.drawable.dottedline_v);
			line.setScaleType(ImageView.ScaleType.FIT_CENTER);
			space.setVisibility(View.INVISIBLE);
			space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,Convert.getPixels(64)));
			background.addView(space);
		}
	}

	//Removed all legs from the view leaving it blank.
	private void resetLegs()
	{
		LinearLayout forground = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_forground);
		LinearLayout background = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_background);
		forground.removeAllViewsInLayout();
		background.removeAllViewsInLayout();
	}

	//A waiting spinner will appear is set to true and be removed again on false.
	private void changeWaitStatus(final boolean isWaiting)
	{
		if ( isWaiting != this.isWaiting)
		{
			this.isWaiting = isWaiting;
			new Handler(context.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					final ProgressBar waitingProgressBar = (ProgressBar)mainView.findViewById(R.id.workingProgressBar);
					ValueAnimator va = isWaiting ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
					va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
					{
						public void onAnimationUpdate(ValueAnimator animation)
						{
							float animVal = ((Float)animation.getAnimatedValue());
							waitingProgressBar.setAlpha(animVal);
						}
					});
					va.start();
				}
			});
		}
	}


	//Hides or shows the direction view
	public void setActive(boolean active)
	{
		if (overlayLayout != null)
		{
			overlayLayout.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
		}
		if (!active)
		{
			closeRouting();
		}
	}

	@Override
	public void onFloorUpdate(Building building, int floor)
	{
	}

	@Override
	public void onLegSelected(int newlegIndex)
	{
		currentLegIndex = Math.max(0,Math.min(newlegIndex, maxLegIndex));
		directionsRenderer.setRouteLegIndex(currentLegIndex);
		mapControl.selectFloor(directionsRenderer.getLegFloor());
		directionsRenderer.setAlpha(255);
		if ( currentLegIndex != currentSelectedLegId )
		{
			currentSelectedLegId = currentLegIndex;
			render(currentRoute);
		}
		//Set the scroll view to the selected leg
		setScrollview(currentLegIndex);
	}

	private void setScrollview(int legIndex)
	{
		final ScrollView sv = (ScrollView)((Activity)context).findViewById(R.id.verticalScrollView);
		ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
		final int startPos = sv.getScrollY();
		final int destPos = (int)(Convert.getPixels(128) *(legIndex-0.5f));
		va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			public void onAnimationUpdate(ValueAnimator animation)
			{
				float animVal = ((Float)animation.getAnimatedValue());
				float newPos = (startPos * (1 - animVal)) + (destPos * animVal);
				sv.scrollTo(sv.getLeft(), (int)newPos);
			}
		});
		va.setDuration(500);
		va.start();
	}

	private void initVehicleSelector()
	{
		final DirectionsfullmenuFragment fragment = this;
		mainView.findViewById(R.id.imageViewWalk).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				if ( currentVehicle != vehicle.walk)
				{
					currentVehicle = vehicle.walk;
					updateVehicleSelector();
					fragment.updateList();
				}
			}
		});
		mainView.findViewById(R.id.imageViewBicycle).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				if ( currentVehicle != vehicle.bicycle)
				{
					currentVehicle = vehicle.bicycle;
					updateVehicleSelector();
					fragment.updateList();
				}
			}
		});
		mainView.findViewById(R.id.imageViewTransit).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				if ( currentVehicle != vehicle.transit)
				{
					currentVehicle = vehicle.transit;
					updateVehicleSelector();
					fragment.updateList();
				}
			}
		});
		mainView.findViewById(R.id.imageVehicleCar).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				if ( currentVehicle != vehicle.car)
				{
					currentVehicle = vehicle.car;
					updateVehicleSelector();
					fragment.updateList();
				}
			}
		});
	}

	private void updateVehicleSelector()
	{
		mainView.findViewById(R.id.imageViewWalk).setAlpha( currentVehicle==vehicle.walk ? 1f : 0.5f );
		mainView.findViewById(R.id.imageViewBicycle).setAlpha( currentVehicle==vehicle.bicycle ? 1f : 0.5f );
		mainView.findViewById(R.id.imageViewTransit).setAlpha( currentVehicle==vehicle.transit ? 1f : 0.5f );
		mainView.findViewById(R.id.imageVehicleCar).setAlpha( currentVehicle==vehicle.car ? 1f : 0.5f );
	}

	private String getTravelMode()
	{
		switch (currentVehicle)
		{
			case bicycle:
				return TravelMode.TRAVEL_MODE_BICYCLING;
			case transit:
				return TravelMode.TRAVEL_MODE_TRANSIT;
			case car:
				return TravelMode.TRAVEL_MODE_DRIVING;
			default:
				return TravelMode.TRAVEL_MODE_WALKING;
		}
	}
}