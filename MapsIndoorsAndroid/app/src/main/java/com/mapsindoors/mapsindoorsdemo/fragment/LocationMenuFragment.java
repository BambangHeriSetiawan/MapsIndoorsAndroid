package com.mapsindoors.mapsindoorsdemo.fragment;

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

import com.mapsindoors.mapsindoorsdemo.IconTextElement;
import com.mapsindoors.mapsindoorsdemo.IconTextListAdapter;
import com.mapsindoors.mapsindoorsdemo.listener.MenuListener;
import com.mapsindoors.mapsindoorsdemo.R;
import com.mapspeople.models.DataField;
import com.mapspeople.models.Location;
import com.mapspeople.routing.Route;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class LocationMenuFragment extends Fragment
{
	private ListView mainMenuList;
	private Context context;
	private MenuListener menuListener;
	private com.mapspeople.models.Location location;
	private boolean isKeyboardActive;
	private LinearLayout headerView;
	private TextView descriptionTextView;
	private Route route;
	private IconTextListAdapter myAdapter;

	public LocationMenuFragment()
	{
		super();
		isKeyboardActive = false;
	}

	public void init(Context context, MenuListener menuListener)
	{
		this.context = context;
		this.menuListener = menuListener;
		descriptionTextView = new TextView(context);
		descriptionTextView.setTextColor(Color.parseColor("#FF000000"));
		descriptionTextView.setAlpha(1f);
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
		ImageView image = (ImageView) headerView.findViewById(R.id.topimage);
		image.setImageResource(R.drawable.bella); //http://www.isca-web.org/files/EU2012_Web/pics/BellaCenter.jpg
		image.setScaleType(ImageView.ScaleType.FIT_XY);
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
				menuListener.onShowLocation(location);
			}
		});
		final Button showRouteButton = (Button) headerView.findViewById(R.id.locations_routebutton);
		showRouteButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				menuListener.onShowRoute(location);
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
														menuListener.onSelect(adapter.getItemObj(position), adapter.getObjType(position));
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

	public void setRoute( Route route)
	{
		this.route = route;
	}

	public void setLocation( Location location )
	{
		this.location = location;

		descriptionTextView.setText(location.getStringProperty("description"));
		String openinghours = getFieldValue("openinghours", location);
		String phone = getFieldValue("phone", location);
		String website = getFieldValue("website", location);
		String imageUrl = getFieldValue("imageurl", location);
		loadTopImage(imageUrl);

		ArrayList<IconTextElement> elements = new ArrayList<>();
		String routeText = "" + (route.getDuration()+30)/60 + " minutes walk";
		addElement(elements, routeText, R.drawable.locationmenu_walk, IconTextListAdapter.Objtype.ROUTE);
		addElement(elements, openinghours, R.drawable.locationmenu_clock, IconTextListAdapter.Objtype.OPENINGHOURS);
		addElement(elements, phone, R.drawable.locationmenu_phone, IconTextListAdapter.Objtype.PHONE);
		addElement(elements, website, R.drawable.locationmenu_www, IconTextListAdapter.Objtype.URL);
		myAdapter = new IconTextListAdapter(context);
		myAdapter.setTint("@color/primary");
		myAdapter.setList(elements);
		mainMenuList.setAdapter(myAdapter);
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
		float dpi = 320;
		final float scale = (dpi / 600f);
		if ( imageURL == null || imageURL.length() < 1)
		{
			image.setImageResource(R.drawable.bella); //http://www.isca-web.org/files/EU2012_Web/pics/BellaCenter.jpg
		}
		else
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
							setTopImageBitmap(image, bitmap, scale);
						}
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	private void setTopImageBitmap(final ImageView image, final Bitmap bitmap, float scale)
	{
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
}