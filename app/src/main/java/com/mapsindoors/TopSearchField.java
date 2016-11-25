package com.mapsindoors;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

/**
 * TopSearchField created by mh on 11-09-2015.
 */
public abstract class TopSearchField implements View.OnClickListener
{
	Activity context;
	boolean isActive;
	ImageView toolbar_close_image;
	Toolbar toolbar;

	public TopSearchField( Activity context )
	{
		this.context = context;
		toolbar = (Toolbar)context.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.app_name);
		toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.white));
		toolbar.setNavigationIcon(R.drawable.ic_dehaze_white_24dp);
		toolbar_close_image = (ImageView)context.findViewById(R.id.toolbar_close_button);
		toolbar_close_image.setOnClickListener(this);
		setSearchText(null);
	}

	@Override
	public void onClick(View v)
	{
		setSearchText(null);
		onClosePressed();
	}

	//Sets a search text and activates the close button.
	//Once exit is pressed onClosePressed will be called.
	public void setSearchText(String newText)
	{
		if ( newText == null || newText.length()<1 )
		{
			toolbar.setTitle(R.string.app_name);
			toolbar_close_image.setVisibility(View.INVISIBLE);
			isActive = false;
		}
		else
		{
			newText = context.getString(R.string.search_for) + " '" + newText + "'";
			toolbar.setTitle(newText);
			toolbar_close_image.setVisibility(View.VISIBLE);
			isActive = true;
		}
	}

	abstract void onClosePressed();
}
