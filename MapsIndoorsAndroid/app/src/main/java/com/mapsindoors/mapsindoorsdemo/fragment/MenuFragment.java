package com.mapsindoors.mapsindoorsdemo.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapsindoors.mapsindoorsdemo.IconTextElement;
import com.mapsindoors.mapsindoorsdemo.IconTextListAdapter;
import com.mapsindoors.mapsindoorsdemo.MapsIndoorsActivity;
import com.mapsindoors.mapsindoorsdemo.listener.MenuListener;
import com.mapsindoors.mapsindoorsdemo.R;
import com.mapspeople.models.Location;
import com.mapspeople.models.POIType;
import com.mapspeople.models.Solution;
import com.mapspeople.routing.Route;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MenuFragment extends Fragment implements MenuListener
{
	private ListView mainMenuList;
	private Context context;
	private Solution solution;
	private MenuListener menuListener;
	private boolean isKeyboardActive;
	private RelativeLayout menuLayout;
	private DrawerLayout drawerLayout;
	private LinearLayout locationMenuLayout;
	private LocationMenuFragment mLocationMenuFragment;
	private ActionBarDrawerToggle mDrawerToggle;
	private static boolean isMenuLocked;

	public MenuFragment()
	{
		super();
		isKeyboardActive = false;
	}

	public void init(Context context, MenuListener menuListener)
	{
		this.context = context;
		this.menuListener = menuListener;
		mLocationMenuFragment = (LocationMenuFragment)((FragmentActivity)context).getSupportFragmentManager().findFragmentById(R.id.locationmenufragment);
		mLocationMenuFragment.init(context, this);

		drawerLayout = (DrawerLayout)((FragmentActivity)context).findViewById(R.id.drawer);
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
		return drawerLayout.isDrawerOpen(GravityCompat.START);
	}

	public void openMenu()
	{
		if ( !isMenuLocked )
		{
			drawerLayout.setClickable(true);
			drawerLayout.openDrawer(GravityCompat.START);
			//Tint the buttons in the colors required.
			mLocationMenuFragment.buttonColorSetup();
		}
	}

	public void closeMenu()
	{
		//User closed the menu while the the keyboard was active. Close it too.
		closeKeyboard(true);
		if ( !isMenuLocked)
		{
			drawerLayout.closeDrawer(GravityCompat.START);
			drawerLayout.setClickable(false);
		}
	}

	public void openLocationMenu(final Location location, final Route route)
	{
		new Handler(context.getMainLooper()).post(new Runnable()
		{
			@Override
			public void run()
			{
				//User selected a specific location. Close the keyboard if needed.
				closeKeyboard(false);
				mLocationMenuFragment.setRoute(route);
				mLocationMenuFragment.setLocation(location);
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
			locationMenuLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.menu_exit));
		}
	}

	private void openKeyboard()
	{
		if (!isKeyboardActive)
		{
			EditText headerText = (EditText)drawerLayout.findViewById(R.id.searchtext);
			final InputMethodManager keyboard = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
			headerText.setText("");
			headerText.setCursorVisible(true);
			headerText.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL);
			headerText.setVisibility(View.VISIBLE);
			headerText.setTextIsSelectable(true);
			headerText.requestFocus();
			keyboard.showSoftInput(headerText, InputMethodManager.SHOW_IMPLICIT);
			isKeyboardActive = true;
		}
	}

	private void closeKeyboard( boolean clearText )
	{
		if (isKeyboardActive)
		{
			EditText headerText = (EditText)drawerLayout.findViewById(R.id.searchtext);
			final InputMethodManager keyboard = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
			keyboard.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
			headerText.setInputType(InputType.TYPE_NULL);
			if ( clearText )
			{
				headerText.setText("");
			}
			isKeyboardActive = false;
		}
	}

	public ListView initMenu(final Solution solution )
	{
		this.solution = solution;
		mLocationMenuFragment.initMenu();
		ImageView image = (ImageView) drawerLayout.findViewById(R.id.topimage);
		image.setImageResource(R.drawable.bella); //http://www.isca-web.org/files/EU2012_Web/pics/BellaCenter.jpg
		image.setScaleType(ImageView.ScaleType.FIT_XY);
		//SearchIcon
		final ImageView headerImage = (ImageView) drawerLayout.findViewById(R.id.searchicon);
		headerImage.setImageResource(R.drawable.seachicon);
		final InputMethodManager keyboard = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		Button editTextButton = (Button) drawerLayout.findViewById(R.id.searchtext_button);
		editTextButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				openKeyboard();
			}
		});
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

		EditText headerText = (EditText) drawerLayout.findViewById(R.id.searchtext);
		headerText.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView headerText, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED)
				{
					menuListener.onSearch(headerText.getText().toString(), true);
					closeKeyboard(false);
				}
				return false;
			}
		});

		headerText.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3)
			{
				String newSearchText = cs.toString();
				setExitbuttonActive(newSearchText.length() > 0);
				menuListener.onSearch(newSearchText, false);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

			@Override
			public void afterTextChanged(Editable headerText) {}
		});
		//Code for the exit button. Pressing this will get the user back to the default list of types
		Button exitButton = (Button) drawerLayout.findViewById(R.id.type_exiticon);
		exitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				//Exit button pressed. Close the keyboard and go back to default - (viewing types)
				menuListener.onSearch("", true);
				closeKeyboard(true);
			}
		});
		mDrawerToggle = new  ActionBarDrawerToggle((Activity)context, drawerLayout, R.drawable.ic_dehaze_white_24dp, R.string.desc)
		{

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				closeKeyboard(true);
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

	public ListView populateMenu()
	{
		mainMenuList = (ListView) drawerLayout.findViewById(R.id.itemlist);
		ArrayList<IconTextElement> elements = new ArrayList<>();
		Map<String, Object> imageDict = MapsIndoorsActivity.getLocationTypeNames();
		IconTextListAdapter myAdapter = new IconTextListAdapter(context);
		mainMenuList.setAdapter(myAdapter);
		boolean refreshLater = true;
		if ( solution != null )
		{
			refreshLater = false;
			for (POIType type : solution.getTypes())
			{
				String typeName = type.name;
				Bitmap bm = (Bitmap)imageDict.get(typeName);
				if (bm != null)
				{
					elements.add(new IconTextElement(typeName, bm, type, IconTextListAdapter.Objtype.TYPE));
				}
				else
				{
					refreshLater = true;
					break;
				}
			}
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
			mainMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener()
												{
													@Override
													public void onItemClick(AdapterView<?> parent, View view, int position, long id)
													{
														//Item on the viewlist selected. Inform the listener.
														IconTextListAdapter adapter = (IconTextListAdapter)parent.getAdapter();
														menuListener.onSelect(adapter.getItemObj(position), adapter.getObjType(position));
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
	public void onShowRoute(Location location)
	{
		menuListener.onShowRoute(location);
	}

	@Override
	public void onShowLocation(Location location)
	{
		menuListener.onShowLocation(location);
	}


	//Feedback from the location detail fragment. Can be ignored for now.
	@Override
	public void onSearch(String searchString, boolean finalSearch)
	{
	}

	@Override
	public void onSelect(Object selectedObject, IconTextListAdapter.Objtype objtype)
	{
		//Location detail clicked. Start an activity depending on what was selected.
		String text = (String)selectedObject;
		switch (objtype)
		{
			case ROUTE:
				menuListener.onShowRoute(mLocationMenuFragment.getLocation());
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