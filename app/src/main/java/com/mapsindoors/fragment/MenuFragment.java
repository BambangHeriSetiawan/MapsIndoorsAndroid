package com.mapsindoors.fragment;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;
import com.mapsindoors.IconTextElement;
import com.mapsindoors.IconTextListAdapter;
import com.mapsindoors.MapsIndoorsActivity;
import com.mapsindoors.VenueListAdapter;
import com.mapsindoors.listener.DirectionsMenuListener;
import com.mapsindoors.listener.MenuListener;
import com.mapsindoors.R;
import com.mapspeople.data.LocationQuery;
import com.mapspeople.data.MPLocationsProvider;
import com.mapspeople.data.OnLocationsReadyListener;
import com.mapspeople.debug.dbglog;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.models.Category;
import com.mapspeople.models.DataField;
import com.mapspeople.models.Location;
import com.mapspeople.models.MenuInfo;
import com.mapspeople.models.POIType;
import com.mapspeople.models.AppConfig;
import com.mapspeople.models.Solution;
import com.mapspeople.models.Venue;
import com.mapspeople.models.VenueCollection;
import com.mapspeople.routing.Route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MenuFragment extends Fragment implements MenuListener
{
	private ListView mainMenuList;
	private Context context;
	private MapsIndoorsActivity activity;
	private Solution solution;
	private VenueCollection venues;
	private MenuListener menuListener;
	private boolean isKeyboardActive;
	private RelativeLayout menuLayout;
	private DrawerLayout drawerLayout;
	private LinearLayout locationMenuLayout;
	private LocationMenuFragment mLocationMenuFragment;
	private ActionBarDrawerToggle mDrawerToggle;
	private Spinner mSpinner;
	private static boolean isMenuLocked;
	private String searchText = "";
	private IconTextListAdapter myAdapter;
	private Handler searchHandler;
	private boolean isWaiting = false;
	private MapControl mapControl;


	public MenuFragment()
	{
		super();
		isKeyboardActive = false;
	}

	public void init(Context context, MenuListener menuListener, MapControl mapControl)
	{
		this.context = context;
		activity = (MapsIndoorsActivity)context;
		this.menuListener = menuListener;
		this.mapControl = mapControl;
		mLocationMenuFragment = (LocationMenuFragment)((FragmentActivity)context).getSupportFragmentManager().findFragmentById(R.id.locationmenufragment);
		mLocationMenuFragment.init(context, this, mapControl);

		//Create the adapter used for the menu
		drawerLayout = (DrawerLayout)((FragmentActivity)context).findViewById(R.id.drawer);
		mainMenuList = (ListView) drawerLayout.findViewById(R.id.itemlist);
		myAdapter = new IconTextListAdapter(context);
		ArrayList<IconTextElement> elements = new ArrayList<>();

		mainMenuList.setAdapter(myAdapter);

		drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
			@Override
			public void onDrawerSlide(View drawerView, float slideOffset)
			{
			}

			@Override
			public void onDrawerOpened(View drawerView)
			{
			}

			@Override
			public void onDrawerClosed(View drawerView)
			{
			}

			@Override
			public void onDrawerStateChanged(int newState)
			{
			}
		});
		//For "ipad sized" devices, the menu should be present at all times

		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		DisplayMetrics dm = new DisplayMetrics();
		((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
		float width = (size.x/(float)dm.densityDpi);
		float height = (size.y/(float)dm.densityDpi);
		//"For devices with more than 4 inch wide and 2 inch tall: lock the menu"
		isMenuLocked = ( width > 4 && height > 2);
		if( isMenuLocked )
		{
			new Handler(context.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					drawerLayout.setScrimColor(Color.TRANSPARENT);
					drawerLayout.setClickable(false);
				}
			});
			//drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
		}

		locationMenuLayout = (LinearLayout) ((Activity)context).findViewById(R.id.locationmenufragment);

		final EditText editText = (EditText) activity.findViewById(R.id.mainEditTextSearch);
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
				if (s != null && s.length() > 0 && !s.toString().contains(getString(R.string.search_places)) )
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
					setExitbuttonActive(true);
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

//		try
//		{
//			//Customize how far from the screen the side swipe should react.
//			Field mDragger = drawerLayout.getClass().getDeclaredField("mLeftDragger");//mRightDragger for right obviously
//			mDragger.setAccessible(true);
//			ViewDragHelper draggerObj = (ViewDragHelper)mDragger.get(drawerLayout);
//			Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
//			mEdgeSize.setAccessible(true);
//			int edge = mEdgeSize.getInt(draggerObj);
//			mEdgeSize.setInt(draggerObj, edge * 5); //optimal value as for me, you may set any constant in dp
//		}
//		catch ( Exception e) {}
	}

	private void closeKeyboard()
	{
		InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(menuLayout.getWindowToken(), 0);
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
			final EditText editText = (EditText) activity.findViewById(R.id.mainEditTextSearch);
			final String searchString = editText.getText().toString();
			if ( searchString.length() > 0 && !searchString.equalsIgnoreCase(getString(R.string.search_places)))
			{
				dbglog.Log("Search for: " + searchString);
				changeWaitStatus(true);
				menuListener.onMenuSearch(searchString, true);
			}
		}
	};

	//A waiting spinner will appear is set to true and be removed again on false.
	public void changeWaitStatus(final boolean isWaiting)
	{
		if ( isWaiting != this.isWaiting)
		{
			this.isWaiting = isWaiting;
			new Handler(context.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					final ProgressBar waitingProgressBar = (ProgressBar)menuLayout.findViewById(R.id.workingProgressBar);
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

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if ( menuLayout == null)
			menuLayout = (RelativeLayout)inflater.inflate(R.layout.mainmenu, container);
		return menuLayout;
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

	public boolean isMenuOpen()
	{
		return (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START));
	}

	public void openMenu()
	{
		if ( !isMenuLocked )
		{
			new Handler(context.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					drawerLayout.setClickable(true);
					drawerLayout.openDrawer(GravityCompat.START);
					//Tint the buttons in the colors required.
					mLocationMenuFragment.buttonColorSetup();
					//Tell the menu listener to open the menu in a reset state.
					menuListener.onMenuSearch(null, true);
				}
			});
		}
	}

	public void closeMenu()
	{
		//User closed the menu while the the keyboard was active. Close it too.
		closeKeyboard();
		if ( !isMenuLocked)
		{
			new Handler(context.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					drawerLayout.closeDrawer(GravityCompat.START);
					drawerLayout.setClickable(false);
				}
			});
		}
	}

	public void openLocationMenu(final Location location)
	{
		new Handler(context.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				//User selected a specific location. Requesting focus to the exit button which in effect will close keyboard and the search.
				Button exitButton = (Button) drawerLayout.findViewById(R.id.type_exiticon);
				exitButton.requestFocus();
				DataField field = location.getField("imageurl");
				Bitmap bitmap = null;
				Bitmap logo = null;
				if ( location.getCategories().length > 0)
				{
					if (field == null || field.getValue() == null || field.getValue().length() == 0)
					{
						List<MenuInfo> mainmenuEntryList = MapControl.getAppConfig().getMenuInfo("mainmenu");
						for (int i = 0; i < mainmenuEntryList.size(); i++)
						{
							MenuInfo element = mainmenuEntryList.get(i);
							String[] cat = location.getCategories();
							if (cat != null && cat.length > 0)
							{
								for ( String category : cat )
								{
									if (element.getCategoryKey().equalsIgnoreCase(category))
									{
										bitmap = element.getMenuImage();
										logo = element.getIcon();
										i = mainmenuEntryList.size();
										break;
									}
								}
							}
						}
					}
				}
				if ( bitmap == null )
				{
					//No category specific image found. Use the current venue image instead.
					ImageView image = (ImageView)drawerLayout.findViewById(R.id.topimage);
					bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
				}
				mLocationMenuFragment.setLocation(location, bitmap, logo) ;
				locationMenuLayout.setVisibility(View.VISIBLE);
				locationMenuLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.menu_enter));
			}
		});
	}

	public void closeLocationsMenu()
	{
		if (locationMenuLayout.getVisibility() != View.GONE)
		{
			locationMenuLayout.setVisibility(View.GONE);
			new Handler(context.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					locationMenuLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.menu_exit));
				}
			});
		}
	}

	public ListView initMenu(final Solution solution, AppConfig settings, VenueCollection venues, String venueName)
	{
		this.solution = solution;
		this.venues = venues;
		mLocationMenuFragment.initMenu();
		ImageView image = (ImageView) drawerLayout.findViewById(R.id.topimage);
		image.setImageBitmap(settings.getVenueImage(venueName));
		image.setScaleType(ImageView.ScaleType.FIT_XY);

		//Add content to the venue selector
		if ( mSpinner == null )
		{
			mSpinner = (Spinner)((FragmentActivity)context).findViewById(R.id.venue_spinner);
			VenueListAdapter spinnerAdapter = new VenueListAdapter(context);
			ArrayList<IconTextElement> elements = new ArrayList<>();
			for (Venue v : venues.getVenues())
			{
				elements.add(new IconTextElement(v.getVenueInfo().name, R.drawable.dot_black, v, IconTextListAdapter.Objtype.VENUE));
			}
			spinnerAdapter.setList(elements);
			mSpinner.setAdapter(spinnerAdapter);
			mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
			{
				//onItemSelected is called on view creation (not by user interaction). Ignore that call.
				boolean firstSelect = true;

				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
				{
					if (!firstSelect)
					{
						VenueListAdapter spinnerAdapter = (VenueListAdapter)parent.getAdapter();
						Venue v = (Venue)spinnerAdapter.getItemObj(pos);
						onMenuVenueSelect(v.getVenueId());
					}
					firstSelect = false;
				}

				@Override
				public void onNothingSelected(AdapterView<?> adapterView)
				{
				}
			});

			ImageView dropdownButton = (ImageView)((FragmentActivity)context).findViewById(R.id.venue_button);
			dropdownButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view)
				{
					mSpinner.performClick();
				}
			});
		}
		//SearchIcon
		final ImageView headerImage = (ImageView) drawerLayout.findViewById(R.id.searchicon);
		headerImage.setImageResource(R.drawable.seachicon);
		final InputMethodManager keyboard = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		//Add a listener to get a callback for keyboard status changes
		drawerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				ViewGroup vg = (ViewGroup)getView();
				if (vg != null && vg.isFocused())
				{
					int rootheight = vg.getRootView().getHeight();
					int containerheight = vg.getHeight();
					if (containerheight > 0)
					{
						int heightDiff = rootheight - vg.getHeight();
						// if more than a quarter of the height is used - its probably a keyboard...
						isKeyboardActive = (heightDiff > (rootheight / 4));
					}
				}
			}
		});

		//Code for the exit button. Pressing this will get the user back to the default list of types
		Button exitButton = (Button) drawerLayout.findViewById(R.id.type_exiticon);
		exitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//Exit button pressed. Close the keyboard and go back to default - (viewing types)
				menuListener.onMenuSearch(null, true);
				final EditText editText = (EditText) activity.findViewById(R.id.mainEditTextSearch);
				editText.setText(getResources().getString(R.string.search_places));
				closeKeyboard();
			}
		});
		exitButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b)
			{
				if ( b == true )
				{
					//Exit button pressed. Close the keyboard and go back to default - (viewing types)
					menuListener.onMenuSearch(null, true);
					final EditText editText = (EditText) activity.findViewById(R.id.mainEditTextSearch);
					editText.setText(getResources().getString(R.string.search_places));
					closeKeyboard();
				}
			}
		});
		mDrawerToggle = new ActionBarDrawerToggle((Activity)context,drawerLayout,R.string.desc,R.string.desc)
		{

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				closeKeyboard();
				super.onDrawerClosed(view);
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
			}

			public void onDrawerSlide(View drawerView, float slideOffset)
			{
				super.onDrawerSlide(drawerView, slideOffset);
			}

		};
		// Set the drawer toggle as the DrawerListener
		drawerLayout.setDrawerListener(mDrawerToggle);
		//Finally populate the menu list
		return populateMenu();
	}

	//Find a type based on a name
	private POIType getType(String typeName)
	{
		if ( solution != null )
			for (POIType type : solution.getTypes())
				if (type != null && typeName.equalsIgnoreCase(type.name))
					return type;
		return null;
	}

	//Populate a (reset) menu with the categories and types defined in the mainmenuEntryList
	public ListView populateMenu()
	{
		mainMenuList = (ListView) drawerLayout.findViewById(R.id.itemlist);
		ArrayList<IconTextElement> elements = new ArrayList<>();
		Map<String, Object> imageDict = MapsIndoorsActivity.getLocationTypeNames();
		boolean refreshLater = true;

		if ( solution != null )
		{
			refreshLater = false;
			List<MenuInfo> mainmenuEntryList = MapControl.getAppConfig().getMenuInfo("mainmenu");

			for (int i = 0; i < mainmenuEntryList.size(); i++)
			{
				MenuInfo menuItem = mainmenuEntryList.get(i);
				//Using the (language specific) category names here and a suitable type icon
				//String typeName = mainmenuEntryList[i][0];
				//POIType type = getType(typeName);
				Bitmap bm = menuItem.getIcon();
				if ( bm != null )
				{
					elements.add(new IconTextElement(menuItem.getName(), bm, menuItem.getCategoryKey(), IconTextListAdapter.Objtype.CATEGORY));
				}
				else
				{
					refreshLater = true;
				}
			}
//			List<String> languages = solution.getAvailableLanguages();
//			for( int i = 0; i <languages.size(); i++ )
//			{
//				String languageCode = languages.get(i);
//				String language = "Dansk";
//				int imageId = R.drawable.language_dk;
//				if ( languageCode.contains("en") )
//				{
//					language = "English";
//					imageId = R.drawable.language_en;
//				}
//				elements.add(new IconTextElement(language, imageId, languageCode, IconTextListAdapter.Objtype.LANGUAGE));
//			}
		}
		if ( refreshLater )
		{
			//Not all icons loaded so this menu is not ready just yet. Schedule a refresh shortly:
			new Timer().schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					new Handler(context.getMainLooper()).post(new Runnable()
					{
						@Override
						public void run()
						{
							populateMenu();
						}
					});
				}
			}, 500);
		}
		else
		{
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
														menuListener.onMenuSelect(adapter.getItemObj(position), adapter.getObjType(position));
													}
												}
			);
		}
		mainMenuList.invalidate();
		return mainMenuList;
	}

	public void setExitbuttonActive(boolean exitActive)
	{
		Button exitButton = (Button) drawerLayout.findViewById(R.id.type_exiticon);
		exitButton.setVisibility(exitActive ? View.VISIBLE : View.GONE);
	}


	@Override
	public void onMenuShowRoute(Location location)
	{
		menuListener.onMenuShowRoute(location);
	}

	@Override
	public void onMenuVenueSelect(String venueId)
	{
		menuListener.onMenuVenueSelect(venueId);
	}

	@Override
	public void onMenuShowLocation(Location location)
	{
		menuListener.onMenuShowLocation(location);
	}


	//Feedback from the location detail fragment. Can be ignored for now.
	@Override
	public void onMenuSearch(String searchString, boolean finalSearch)
	{
	}

	@Override
	public void onMenuSelect(Object selectedObject, IconTextListAdapter.Objtype objtype)
	{
		//Location detail clicked. Start an activity depending on what was selected.
		String text = (String)selectedObject;
		switch (objtype)
		{
			case ROUTE:
				menuListener.onMenuShowRoute(mLocationMenuFragment.getLocation());
				break;
			case PHONE:
				context.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + text)));
				break;
			case URL:
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(text)));
				break;
		}
	}

	//Returns true if the screensize on this device should cause the menu to be locked.
	public static boolean isMenuLocked()
	{
		return isMenuLocked;
	}

}