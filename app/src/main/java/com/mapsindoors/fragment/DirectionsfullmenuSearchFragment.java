package com.mapsindoors.fragment;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.mapsindoors.IconTextElement;
import com.mapsindoors.IconTextListAdapter;
import com.mapsindoors.MapsIndoorsActivity;
import com.mapsindoors.R;
import com.mapsindoors.listener.LocationFoundListener;
import com.mapspeople.data.LocationQuery;
import com.mapspeople.data.MPLocationsProvider;
import com.mapspeople.data.OnLocationsReadyListener;
import com.mapspeople.debug.dbglog;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.Location;
import com.mapspeople.models.MPLocation;
import com.mapspeople.models.Point;
import com.mapspeople.models.Venue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DirectionsfullmenuSearchFragment extends Fragment
{
	private Context context;
	private LinearLayout overlayLayout;
	private TextView descriptionTextView;
	private MapControl mapControl;
	private View mainView;
	private boolean isWaiting = false;
	private MapsIndoorsActivity activity;
	private Handler searchHandler;
	private ListView mainMenuList;
	private IconTextListAdapter myAdapter;
	private LocationFoundListener listener;

	public DirectionsfullmenuSearchFragment()
	{
		super();
	}

	public void init(final Context context, final MapControl mapControl)
	{
		this.context = context;
		activity = (MapsIndoorsActivity)context;
		this.mapControl = mapControl;
		descriptionTextView = new TextView(context);
		descriptionTextView.setTextColor(Color.parseColor("#FF000000"));
		descriptionTextView.setAlpha(1f);
		mainView = ((Activity)context).findViewById(R.id.directionsfullmenuSearchfragment);
		mainView.setVisibility(View.GONE);
		mainView.findViewById(R.id.imageExit).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view)
					{
						close();
					}
				}
		);

		mainMenuList = (ListView) mainView.findViewById(R.id.itemlist);
		myAdapter = new IconTextListAdapter(context);
		mainMenuList.setAdapter(myAdapter);

		final EditText editText = (EditText) activity.findViewById(R.id.editTextSearch);
		//Note: Creating a textwatcher as it's needed for software keyboard support.
		editText.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				//Only start searching if the user wrote something to look for (other than the "Search places" start text)
				if (s != null && s.length() > 0 && !s.toString().contains(getString(R.string.search_for)) )
				{
					startSearchTimer();
				}
			}
		});
		editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean infocus)
			{
				if ( infocus )
				{
					editText.setText("");
				}
			}
		});
		//Close keyboard and search when user presses search on the keyboard:
		editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEARCH)
				{
					closeKeyboard();
					handled = true;
				}
				return handled;
			}
		});
		//Close keyboard and search when user presses enter:
		editText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View view, int keyCode, KeyEvent keyEvent)
			{
				if (keyEvent.getAction() == KeyEvent.ACTION_DOWN)
				{
					switch (keyCode)
					{
						case KeyEvent.KEYCODE_DPAD_CENTER:
						case KeyEvent.KEYCODE_ENTER:
							closeKeyboard();
							return true;
						default:
							startSearchTimer();
							break;
					}
				}
				return false;
			}
		});
	}

	//Only search after a second of delay. Any search requests before one sec should replace the seach and restart the timer.
	private void startSearchTimer()
	{
		if ( searchHandler != null )
		{
			searchHandler.removeCallbacks(searchRunner);
		}
		searchHandler = new Handler();
		searchHandler.postDelayed(searchRunner, 1000);
	}

	private Runnable searchRunner = new Runnable() {
		@Override
		public void run()
		{
			final EditText editText = (EditText) activity.findViewById(R.id.editTextSearch);
			String searchString = editText.getText().toString();
			if ( searchString.length() > 0 )
			{
				dbglog.Log("Search for: " + searchString);
				//Set up a location listener and make a location search call
				final MPLocationsProvider locationsProvider = new MPLocationsProvider(context);
				locationsProvider.setOnLocationsReadyListener(new OnLocationsReadyListener()
				{
					@Override
					public void onLocationsReady(List<Location> locations)
					{
						populateMenu(locations);
						changeWaitStatus(false);
					}

					@Override
					public void onLocationDetailsReady(Location location)
					{
					}
				});
				changeWaitStatus(true);
				LocationQuery query = new LocationQuery(MapControl.getClientId());
				query.max = 10;
				query.arg = activity.getSolutionId();
				query.orderBy = LocationQuery.OrderBy.RELEVANCE;
				query.setQuery(searchString);
				locationsProvider.getLocationsUsingQueryAsync(query, new Locale(mapControl.getLangugage()));
			}
		}
	};

	private void closeKeyboard()
	{
		InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mainView.getWindowToken(), 0);
	}

	public void open()
	{
		mainView.setVisibility(View.VISIBLE);
	}

	public void close()
	{
		InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mainView.getWindowToken(), 0);
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
			overlayLayout = (LinearLayout)inflater.inflate(R.layout.directionsfullmenu_search, container);
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

	public void clearMenu()
	{
		new Handler(context.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				ArrayList<IconTextElement> elements = new ArrayList<>();
				String estimated_position_near = context.getString(R.string.estimated_position_near);
				Location myLocation = new MPLocation(activity.getCurrentPos(), estimated_position_near);
				elements.add(new IconTextElement(myLocation.getName(), R.drawable.mylocation, myLocation, IconTextListAdapter.Objtype.LOCATION));
				//All data loaded now. Show the list.
				myAdapter.setList(elements);
				mainMenuList.setAdapter(myAdapter);
				mainMenuList.setClickable(true);
				mainMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener()
													{
														@Override
														public void onItemClick(AdapterView<?> parent, View view, int position, long id)
														{
															//Item on the viewlist selected. Inform the listener.
															IconTextListAdapter adapter = (IconTextListAdapter)parent.getAdapter();
															Location l = (Location)adapter.getItemObj(position);
															dbglog.Log("item selected: " + l.getName());
															if ( listener != null )
															{
																listener.onLocationsFound(l);
															}
														}
													}
				);
				mainMenuList.invalidate();
			}
		});
	}

	//Populate a (reset) menu with the categories and types defined in the mainmenuEntryList
	public void populateMenu(final List<Location> locations)
	{
		new Handler(context.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				ArrayList<IconTextElement> elements = new ArrayList<>();
				//Map<String, Object> imageDict = MapsIndoorsActivity.getLocationTypeNames();
				LatLng target = activity.getMapControl().getMap().getCameraPosition().target;
				//Get the latest position from our position providers made for mapcontrol earlier.
				Point from = mapControl.getCurrentPosition().getPoint();
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
						Bitmap bm = (Bitmap)MapsIndoorsActivity.getLocationTypeNames().get(typeName);
						Venue venue = mapControl.getVenueCollection().getVenue(location.getStringProperty("venue"));
						String venueName = (venue == null) ? "" : venue.getName();
						String building = location.getStringProperty("building");
						building = (building == null) ? "" : building;
						String floor = ", " + location.getFloorIndex() + " floor";
						String SubText = ( building.equalsIgnoreCase(venueName) ) ? venueName + floor : venueName + ", " + building + floor;
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
				//All data loaded now. Show the list.
				myAdapter.setList(elements);
				mainMenuList.setAdapter(myAdapter);
				mainMenuList.setClickable(true);
				mainMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener()
													{
														@Override
														public void onItemClick(AdapterView<?> parent, View view, int position, long id)
														{
															//Item on the viewlist selected. Inform the listener.
															IconTextListAdapter adapter = (IconTextListAdapter)parent.getAdapter();
															Location l = (Location)adapter.getItemObj(position);
															dbglog.Log("item selected: " + l.getName());
															if ( listener != null )
															{
																listener.onLocationsFound(l);
															}
														}
													}
				);
				mainMenuList.invalidate();
			}
		});
	}


	//Hides or shows the search view
	public void setActive(boolean active)
	{
		if (overlayLayout != null)
		{
			overlayLayout.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
		}
		if ( active )
		{
			clearMenu();
			final EditText editText = (EditText) activity.findViewById(R.id.editTextSearch);
			editText.setText(R.string.search_for);
		}
		else
		{
			closeKeyboard();
		}
	}

	public void SetOnLocationFoundhandler(LocationFoundListener listener)
	{
		this.listener = listener;
	}


}