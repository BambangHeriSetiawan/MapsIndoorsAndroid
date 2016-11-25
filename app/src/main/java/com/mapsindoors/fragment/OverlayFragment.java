package com.mapsindoors.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.mapsindoors.R;

public class OverlayFragment extends Fragment
{
	private RelativeLayout overlayLayout;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		if (overlayLayout == null)
			overlayLayout = (RelativeLayout)inflater.inflate(R.layout.overlay_layout, container);
		return overlayLayout;
	}
}