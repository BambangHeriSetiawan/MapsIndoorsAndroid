package com.mapsindoors.positionprovider;

import com.mapspeople.position.OnPositionUpdateListener;
import com.mapspeople.position.PositionProvider;
import com.mapspeople.position.PositionResult;

/**
 * Created by mh on 08-05-2015.
 */
public class MixedPositionProvider implements PositionProvider
{
	private boolean isGPSActive;
	private boolean isBeaconActive;
	private PositionProvider GPSPositionProvider;
	private PositionProvider BeaconPositionProvider;
	private OnPositionUpdateListener updateListener;
	private long lastGPSUpdateTimestamp = 0;
	private long lastBeaconUpdateTimestamp = 0;

	public MixedPositionProvider(final PositionProvider GPSPositionProvider, final PositionProvider BeaconPositionProvider)
	{
		this.GPSPositionProvider = GPSPositionProvider;
		this.BeaconPositionProvider = BeaconPositionProvider;
		GPSPositionProvider.addOnPositionUpdateListener(new OnPositionUpdateListener()
		{
			@Override
			public void onPositionUpdate(PositionResult pos)
			{
				//Got a GPS pos. Check if we should use or ignore it.
				lastGPSUpdateTimestamp = System.currentTimeMillis();
				if (isGPSActive)
				{
					updateListener.onPositionUpdate(pos);
				}
				else if (isBeaconActive && (lastGPSUpdateTimestamp - lastBeaconUpdateTimestamp) > 5000)
				{
					isBeaconActive = false;
					isGPSActive = true;
					updateListener.onPositionUpdate(pos);
				}
			}

			@Override
			public void onPositionFailed(PositionProvider provider)
			{
				updateListener.onPositionFailed(provider);
			}

			@Override
			public void onPositioningStarted(PositionProvider provider)
			{
				if (!isBeaconActive)
				{
					isGPSActive = true;
				}
			}
		});
		BeaconPositionProvider.addOnPositionUpdateListener(new OnPositionUpdateListener()
		{
			@Override
			public void onPositionUpdate(PositionResult pos)
			{
				lastBeaconUpdateTimestamp = System.currentTimeMillis();
				if (isBeaconActive)
				{
					updateListener.onPositionUpdate(pos);
				}
				else if (isGPSActive && (lastBeaconUpdateTimestamp - lastGPSUpdateTimestamp) > 5000)
				{
					isGPSActive = false;
					isBeaconActive = true;
					updateListener.onPositionUpdate(pos);
				}
			}

			@Override
			public void onPositionFailed(PositionProvider provider)
			{
				updateListener.onPositionFailed(provider);
			}

			@Override
			public void onPositioningStarted(PositionProvider provider)
			{
				if (!isGPSActive)
				{
					isBeaconActive = true;
				}
			}
		});
	}

	@Override
	public void startPositioning(String arg)
	{
		GPSPositionProvider.startPositioning(arg);
		BeaconPositionProvider.startPositioning(arg);
	}

	@Override
	public void stopPositioning(String arg)
	{
		GPSPositionProvider.stopPositioning(arg);
		BeaconPositionProvider.stopPositioning(arg);
	}

	@Override
	public boolean isRunning()
	{
		return GPSPositionProvider.isRunning() && BeaconPositionProvider.isRunning();
	}

	@Override
	public void addOnPositionUpdateListener(OnPositionUpdateListener listener)
	{
		updateListener = listener;
	}

	@Override
	public void setProviderId(String id)
	{
		GPSPositionProvider.setProviderId(id);
	}

	@Override
	public String getProviderId()
	{
		return GPSPositionProvider.getProviderId();
	}

	@Override
	public PositionResult getLatestPosition()
	{
		if (isBeaconActive)
		{
			return BeaconPositionProvider.getLatestPosition();
		}
		else
		{
			return GPSPositionProvider.getLatestPosition();
		}
	}

	@Override
	public void startPositioningAfter(int millis, String arg)
	{
		BeaconPositionProvider.startPositioningAfter(millis, arg);
		GPSPositionProvider.startPositioningAfter(millis, arg);
	}
}
