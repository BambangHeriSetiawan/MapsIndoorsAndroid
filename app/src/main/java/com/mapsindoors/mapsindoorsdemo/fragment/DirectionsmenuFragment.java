package com.mapsindoors.mapsindoorsdemo.fragment;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.mapsindoors.mapsindoorsdemo.MapsIndoorsActivity;
import com.mapsindoors.mapsindoorsdemo.R;
import com.mapsindoors.mapsindoorsdemo.listener.DirectionsMenuListener;
import com.mapspeople.mapcontrol.MapControl;
import com.mapspeople.mapcontrol.OnFloorUpdateListener;
import com.mapspeople.models.Building;
import com.mapspeople.models.Location;
import com.mapspeople.routing.DirectionsRenderer;
import com.mapspeople.routing.MPDirectionsRenderer;
import com.mapspeople.routing.MPRoutingProvider;
import com.mapspeople.routing.OnRouteResultListener;
import com.mapspeople.routing.OnLegSelectedListener;
import com.mapspeople.routing.Route;
import com.mapspeople.routing.RouteLeg;
import com.mapspeople.routing.RouteStep;
import com.mapspeople.routing.RoutingProvider;
import com.mapspeople.routing.TravelMode;
import com.mapspeople.util.Convert;

import java.util.Calendar;
import java.util.List;

//Fragment that shows routes on the map (using directionsRenderer)
public class DirectionsmenuFragment extends Fragment implements OnLegSelectedListener, OnFloorUpdateListener
{
	private LinearLayout overlayLayout;
	private Route currentRoute;
	private int currentSelectedLegId;
	// Rendering object used to draw routes on top of the google map.
	private DirectionsRenderer directionsRenderer;
	DirectionsMenuListener menuListener;
	private GoogleMap googleMap;
	private Context context;
	private MapControl mapControl;
	private int currentLegIndex;
	private int maxLegIndex;
	private int[] actionFileId = { R.drawable.subfab_elevator, R.drawable.subfab_steps, R.drawable.subfab_steps, R.drawable.subfab_steps };
	private String[] actionName = {"elevator","escalator","steps","travelator"};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (overlayLayout == null)
		{
			overlayLayout = (LinearLayout)inflater.inflate(R.layout.directionsmenu, container);
		}
		setActive(false);
		return overlayLayout;
	}

	public void init(final MapsIndoorsActivity activity, GoogleMap map, DirectionsMenuListener menuListener )
	{
		this.context = activity;
		this.mapControl = activity.getMapControl();
		this.menuListener = menuListener;
		googleMap = map;

		int primaryColor = ContextCompat.getColor(context, R.color.primary);
		int accentColor = ContextCompat.getColor(context, R.color.accent);
		directionsRenderer = new MPDirectionsRenderer(context, this);
		directionsRenderer.setPrimaryColor(primaryColor);
		directionsRenderer.setAccentColor(accentColor);
		directionsRenderer.setTextColor(ContextCompat.getColor(context, R.color.white));
		Button nextButton = (Button)((Activity)context).findViewById(R.id.directionslayout_nextbutton);
		nextButton.getBackground().setColorFilter(accentColor, PorterDuff.Mode.MULTIPLY);
		nextButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (currentLegIndex < maxLegIndex)
				{
					currentLegIndex++;
					onLegSelected(currentLegIndex);
				}
			}
		});
		Button previousButton = (Button)((Activity)context).findViewById(R.id.directionslayout_previousbutton);
		previousButton.getBackground().setColorFilter(Color.parseColor("#FF808080"), PorterDuff.Mode.DST);
		previousButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (currentLegIndex > 0)
				{
					currentLegIndex--;
					onLegSelected(currentLegIndex);
				}
			}
		});
	}

	public void route(final Location origin, final Location destination, final String travelMode, String[] avoids, Calendar departure, Calendar arrival)
	{
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
				setActive(false);
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
						directionsRenderer.setMap(googleMap);
						directionsRenderer.setAlpha(255);
						currentSelectedLegId = 0;
						maxLegIndex = route.getRouteLegs().size()-1;
						render(route);
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

	//Populate the directions view with the legs from a route
	private void render( Route route )
	{
		resetLegs();
		String srcDesc = context.getString(R.string.you_are_here);
		int srcIcon = R.drawable.mylocation;
		String dstDesc;
		int dstIcon;
		List<RouteLeg> legs = route.getRouteLegs();
		View forground = overlayLayout.findViewById(R.id.directionslayout_forground);
		View background = overlayLayout.findViewById(R.id.directionslayout_background);
		View buttonGrid = overlayLayout.findViewById(R.id.directionslayout_buttongrid);
		if ( legs.size() <= 1)
		{
			//Only one leg in this route, no need to show the 'route leg selector'; the headline will suffice
			forground.setVisibility(View.GONE);
			background.setVisibility(View.GONE);
			buttonGrid.setVisibility(View.GONE);
		}
		else
		{
			forground.setVisibility(View.VISIBLE);
			background.setVisibility(View.VISIBLE);
			buttonGrid.setVisibility(View.VISIBLE);
			for (int i = 0; i < legs.size(); i++)
			{
				boolean isLastLeg = (i >= legs.size() - 1);
				if (isLastLeg)
				{
					dstDesc = context.getString(R.string.destination);
					addLeg(srcDesc, srcIcon, dstDesc, R.drawable.dest_icon, i, (i < 1), true);
				}
				else
				{
					RouteStep firstStepNext = legs.get(i + 1).getSteps().get(0);
					dstDesc = firstStepNext.getHighway();
					//If the level of the last step of this leg differs from the first step of the next leg, take a note.
					int startLevel = firstStepNext.getStartPoint().getZIndex();
					int endLevel = firstStepNext.getEndPoint().getZIndex();
					boolean isLevelChange = (startLevel != endLevel);
					int stairType = 0;
					dstIcon = R.drawable.subfab_exit;
					if (isLevelChange)
					{
						for (int idx = 0; idx < actionName.length; idx++)
						{
							if (dstDesc.equalsIgnoreCase(actionName[idx]))
							{
								stairType = idx;
								dstDesc = context.getString(R.string.stairs_level) + startLevel;
								dstIcon = actionFileId[idx];
								break;
							}
						}
					}
					else
					{
						dstDesc = "Enter venue";
					}
					addLeg(srcDesc, srcIcon, dstDesc, dstIcon, i, (i < 1), false);
					if (isLevelChange)
					{
						srcDesc = ((stairType == 1) ? context.getString(R.string.stairs_level) : context.getString(R.string.elevator_level)) + endLevel;
					}
					else
					{
						srcDesc = dstDesc;
					}
					srcIcon = dstIcon;
				}
			}
		}
		setActive(true);
	}

	//Removed all legs from the view leaving it blank.
	private void resetLegs()
	{
		LinearLayout forground = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_forground);
		LinearLayout background = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_background);
		forground.removeAllViewsInLayout();
		background.removeAllViewsInLayout();
	}

	//Adds a leg to the (buttom) direction menu
	//If it's selected draw it with alpha 1 - otherwise render it a bit transparent.
	//If it's the first icon draw 'mylocation' in full size and ignore the given image
	//If it's the last icon dont connect it to the next leg (thus don't draw the 'dotted line' after the last icon).
	private void addLeg(String srcDesc, int srcIcon, String dstDesc, int dstIcon, final int legIndex, boolean firstEntry, boolean lastEntry)
	{
		int primaryColor = ContextCompat.getColor(context, R.color.primary);
		ImageButton button = new ImageButton(context);
		LinearLayout forground = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_forground);
		LinearLayout background = (LinearLayout)overlayLayout.findViewById(R.id.directionslayout_background);
		//Create the forgrund button
		button.setLayoutParams(new LinearLayout.LayoutParams(Convert.getPixels(200), Convert.getPixels(48)));
		button.setBackgroundResource(android.R.color.transparent);
		button.setImageBitmap(createLabel(srcDesc, dstDesc, 11, srcIcon, dstIcon, primaryColor, firstEntry, legIndex == currentSelectedLegId));
		forground.addView(button);
		button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onLegSelected(legIndex);
			}
		});
		//Create the dotted line in the background unless this is the last entry
		if (!lastEntry)
		{
			ImageView line = new ImageView(context);
			line.setImageResource(R.drawable.dottedline);
			line.setScaleType(ImageView.ScaleType.FIT_CENTER);
			line.setColorFilter(primaryColor);
			line.setBackgroundResource(android.R.color.transparent);
			line.setLayoutParams(new LinearLayout.LayoutParams(Convert.getPixels(100), LinearLayout.LayoutParams.WRAP_CONTENT));
			background.addView(line);
			ImageView space = new ImageView(context);
			space.setImageResource(R.drawable.dottedline);
			line.setScaleType(ImageView.ScaleType.FIT_CENTER);
			space.setVisibility(View.INVISIBLE);
			space.setLayoutParams(new LinearLayout.LayoutParams(Convert.getPixels(100), LinearLayout.LayoutParams.WRAP_CONTENT));
			background.addView(space);
			int height = (int)(Convert.getPixels(48) * 0.6);
			background.setPadding(0,height,0,0);
		}
	}

	//Creates a new bitmap that contains a label with an icon and text
	//If it's selected draw it with alpha 1 - otherwise render it a bit transparent.
	//If it's the first icon draw 'mylocation' in full size and ignore the given image
	private Bitmap createLabel(String textSrc, String textDst, int textSize, int srcIcon, int dstIcon, int color, boolean firstIcon, boolean selected)
	{
		Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.menu_placeholder);
		Bitmap icon_src = BitmapFactory.decodeResource(context.getResources(), srcIcon);
		Bitmap icon_dst = BitmapFactory.decodeResource(context.getResources(), dstIcon);
		int width = Convert.getPixels(200);
		int height = Convert.getPixels(48);
		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(result);
		//The line should be two thirds down on the canvas
		int h2 = (int)(height * 0.66);
		int icon1x = width / 4;
		int icon2x = width * 3 / 4;
		//Draw the fat line between icons using the primary color
		drawLine(icon1x, h2, icon2x - icon1x, 8, canvas, color, selected);
		//Add the two icons
		drawIcon(icon_src, icon1x, h2, 0.5f, color, canvas, firstIcon, selected);
		drawIcon(icon_dst, icon2x, h2, 0.5f, color, canvas, false, selected);
		//Finally define font size and draw the text above each icon
		TextPaint tp = new TextPaint();
		tp.setTextSize(Convert.getPixels(textSize));
		int textWidthSrc = Math.round(tp.measureText(textSrc));
		int textWidthDst = Math.round(tp.measureText(textDst));
		int iconH2 = icon.getHeight() / 2;
		int text_posy = h2 - ( iconH2 + Convert.getPixels(3) );
		canvas.drawText(textSrc, icon1x - (textWidthSrc / 2), text_posy, tp);
		canvas.drawText(textDst, icon2x - (textWidthDst / 2), text_posy, tp);
		return result;
	}

	//Draws an icon on a canvas
	//If it's selected draw it with alpha 1 - otherwise render it a bit transparent.
	//If it's the first icon draw 'mylocation' in full size and ignore the given image
	private void drawIcon(Bitmap icon, int x, int y, double scale, int color, Canvas canvas, boolean firstIcon, boolean selected)
	{
		Paint primaryColor = new Paint();
		int iconW = icon.getWidth();
		int iconH = icon.getHeight();
		int backgroundImage = firstIcon ? R.drawable.mylocation : R.drawable.menu_placeholder;
		Bitmap iconBG = BitmapFactory.decodeResource(context.getResources(), backgroundImage);
		int iconX = x - (iconBG.getWidth() / 2);
		int iconY = y - (iconBG.getHeight() / 2);
		//primaryColor.setAlpha(selected ? 255 : 63);
		canvas.drawBitmap(iconBG, iconX, iconY, primaryColor);
		if ( !firstIcon )
		{
			primaryColor.setColor(color);
			primaryColor.setAlpha(selected ? 255 : 63);
			Rect srcRect = new Rect(0, 0, iconW, iconH);
			int widthScaled = (int)(iconW * (scale / 2));
			int heightScaled = (int)(iconH * (scale / 2));
			Rect dstRect = new Rect(x - widthScaled, y - heightScaled, x + widthScaled, y + heightScaled);
			canvas.drawBitmap(icon, srcRect, dstRect, primaryColor);
		}
	}

	//Draws the 'fat' line between the icons with a shadow
	private void drawLine(int x, int y, int width, int height, Canvas canvas, int color, boolean selected)
	{
		int h2 = height / 2;
		Paint line = new Paint();
		line.setColor(color);
		line.setAlpha(selected ? 255 : 63);
		int left = x;
		int top = y - h2;
		int right = x + width;
		int bottom = y + h2;
		if ( selected )
		{
			Paint background = new Paint();
			background.setColor(Color.parseColor("#20000000"));
			canvas.drawRect(left + 4, top + 4, right + 4, bottom + 4, background);
		}
		canvas.drawRect(left, top, right, bottom, line);
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

	//Highlights a specific leg
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
		final HorizontalScrollView sv = (HorizontalScrollView)((Activity)context).findViewById(R.id.horizontalScrollView);
		ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
		final int startPos = sv.getScrollX();
		final int destPos = (int)(Convert.getPixels(200) *(legIndex-0.5f));
		va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			public void onAnimationUpdate(ValueAnimator animation)
			{
				float animVal = ((Float)animation.getAnimatedValue());
				float newPos = (startPos * (1 - animVal)) + (destPos * animVal);
				sv.scrollTo((int)newPos, sv.getBottom());
			}
		});
		va.setDuration(500);
		va.start();
	}
	//Called the selected floor changed. If we are on the same floor as the current routeleg use fill alpha. Otherwise dim it.
	@Override
	public void onFloorUpdate(Building building, int newFloor)
	{
		if ( currentRoute != null && overlayLayout.getVisibility() == View.VISIBLE )
		{
			int floor = currentRoute.getRouteLegs().get(currentSelectedLegId).getEndPoint().getZIndex();
			int alpha = (newFloor == floor) ? 255 : 63;
			directionsRenderer.setAlpha(alpha);
		}
	}
}