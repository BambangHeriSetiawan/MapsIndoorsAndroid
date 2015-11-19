package com.mapsindoors.mapsindoorsdemo;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RelativeLayout;

import com.mapsindoors.mapsindoorsdemo.listener.FloatingActionListener;

/**
 * Created by mh on 11-09-2015.
 */
public class FloatingAction
{
	private int FABButtonHeight = 200;
	private boolean isFABOpen;
	private RelativeLayout view;
	private FloatingActionListener fabListener;
	private boolean isActive;

	public FloatingAction( Activity activity, FloatingActionListener fabListener)
	{
		this.view = (RelativeLayout)activity.findViewById(R.id.overlayfragment);
		this.fabListener = fabListener;
		isActive = true;
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		FABButtonHeight = (int)((float)FABButtonHeight*(dm.densityDpi/600.0f));

		FloatingActionButton fab = (FloatingActionButton)view.findViewById(R.id.fabSearch);
		fab.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onFABClick();
			}
		});
		FloatingActionButton ab1 = (FloatingActionButton)view.findViewById(R.id.abutton1);
		FloatingActionButton ab2 = (FloatingActionButton)view.findViewById(R.id.abutton2);
		FloatingActionButton ab3 = (FloatingActionButton)view.findViewById(R.id.abutton3);
		final FloatingActionListener listener = this.fabListener;
		ab1.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				listener.onFABSelect("Restroom");
				//Close the fab if open
				if ( isFABOpen )
					onFABClick();
			}
		});
		ab2.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				listener.onFABSelect("Stairs");
				//Close the fab if open
				if (isFABOpen)
				{
					onFABClick();
				}
			}
		});
		ab3.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				listener.onFABSelect("Entrance");
				//Close the fab if open
				if (isFABOpen)
				{
					onFABClick();
				}
			}
		});
	}

	public void closeMenu()
	{
		if ( isFABOpen )
		{
			onFABClick();
		}
	}


	public void setActive( boolean isActive )
	{
		final FloatingActionButton fab = (FloatingActionButton) (view.findViewById(R.id.fabSearch));
		if ( isActive != this.isActive )
		{
			ValueAnimator va = isActive ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
			va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
			{
				public void onAnimationUpdate(ValueAnimator animation)
				{
					float animVal = ((Float)animation.getAnimatedValue());
					fab.setAlpha(animVal);
				}
			});
			va.setDuration(500);
			va.start();
			if (!isActive)
				closeMenu();
			this.isActive = isActive;
		}
	}

	private void onFABClick()
	{
		if ( !isActive )
			return;
		isFABOpen = !isFABOpen;
		final FloatingActionButton fab = (FloatingActionButton) (view.findViewById(R.id.fabSearch));
		ValueAnimator va = isFABOpen ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
		va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			public void onAnimationUpdate(ValueAnimator animation)
			{
				float animVal = ((Float)animation.getAnimatedValue());
				fab.setRotation(animVal * 90);
			}
		});
		if ( isFABOpen )
		{
			fab.setImageResource(R.drawable.ic_clear_white_24dp);
		}
		va.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animation)
			{
				if ( !isFABOpen )
				{
					fab.setImageResource(R.drawable.fab_findnearest);
				}
			}
			@Override public void onAnimationStart(Animator animation) {}
			@Override public void onAnimationCancel(Animator animation) {}
			@Override public void onAnimationRepeat(Animator animation) {}
		});
		va.start();
		toggle((FloatingActionButton) (view.findViewById(R.id.abutton1)), FABButtonHeight  , isFABOpen);
		toggle((FloatingActionButton) (view.findViewById(R.id.abutton2)), FABButtonHeight*2, isFABOpen);
		toggle((FloatingActionButton) (view.findViewById(R.id.abutton3)), FABButtonHeight*3, isFABOpen);
	}

	private void toggle(final FloatingActionButton button, final int distancePx, boolean open) {
		FloatingActionButton fab = (FloatingActionButton) (view.findViewById(R.id.fabSearch));
		button.setX(fab.getX());
		button.setY(fab.getY());
		button.setVisibility(View.VISIBLE);
		button.setClickable(open);
		final float startY = fab.getY();
		ValueAnimator va = open ? ValueAnimator.ofFloat(0f, 1f) : ValueAnimator.ofFloat(1f, 0f);
		va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
		{
			public void onAnimationUpdate(ValueAnimator animation)
			{
				float animVal = ((Float)animation.getAnimatedValue());
				button.setAlpha(animVal);
				button.setY(startY - (animVal * distancePx));
			}
		});
		va.start();
	}
}
