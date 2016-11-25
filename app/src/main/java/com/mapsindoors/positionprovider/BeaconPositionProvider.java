package com.mapsindoors.positionprovider;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.mapspeople.mapssdk.R;
import com.mapspeople.models.Point;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import android.graphics.Color;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.mapspeople.debug.dbglog;
import com.mapspeople.position.*;
import com.mapspeople.util.ThreadUtil;

/**
 * Created by JSM on 26/02/15.
 */

public class BeaconPositionProvider implements PositionProvider, OnBeaconServiceConnectListener
{
	private boolean started = false;
	private String uuid;
	private BeaconProvider beaconProvider;
	private List<Point> posList;
	private PositionResult latestPosition;
	private String providerId;
	private OnPositionUpdateListener listener;
	private BeaconManager beaconManager;
	private String clientId;
	private BeaconRSSICacheMgr beaconRSSICacheMgr;
	private Collection<Beacon> beaconCollection;
	private GoogleMap mMap;
	private boolean isRSSICircleVisible;
	private static boolean isBeaconContextBound = false;
	private PositionCalculator positionCalculator;
	private boolean isUsingTrilateration = false;
	private static Context context = null;
	private static List<String> layoutList = new ArrayList<>();
	protected static final String TAG = "MonitoringActivity";

	public BeaconPositionProvider(Context ctx, String UUID)
	{
		if (context != null)
		{
			this.beaconManager = BeaconManager.getInstanceForApplication(context);
			try
			{
				beaconManager.unbind((BeaconConsumer)context);
				isBeaconContextBound = false;
			} catch (Exception e)
			{
				dbglog.Log("Failed to unbind the beacon manager");
			}
		}
		//BeaconManager.setUseTrackingCache(true);
		BeaconManager.setAndroidLScanningDisabled(true);
		context = ctx;
		this.uuid = UUID;
		this.beaconProvider = new MPBeaconProvider();
		this.positionCalculator = new PositionCalculator();
		setBeaconParserLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24");
		beaconRSSICacheMgr = new BeaconRSSICacheMgr();
	}

	public void unbindBeaconManager()
	{
	if ( beaconManager.isBound((BeaconConsumer)context) )
			beaconManager.unbind((BeaconConsumer)context);
	}

	public void setBeaconParserLayout(String layout)
	{
		beaconManager = BeaconManager.getInstanceForApplication(context);
		if (!layoutList.contains(layout))
		{
			layoutList.add(layout);
			beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(layout));
		}
		if (!isBeaconContextBound)
		{
			try
			{
				if ( !beaconManager.isBound((BeaconConsumer)context) )
				{
					beaconManager.bind((BeaconConsumer)context);
				}
				isBeaconContextBound = true;
			} catch (Exception e)
			{
				dbglog.Log("Failed to bind the beacon manager");
			}
		}
	}

	public void useTrilateration(boolean isActive)
	{
		isUsingTrilateration = isActive;
	}

	@Override
	public void startPositioning(String arg)
	{
		this.clientId = arg;
	}

	@Override
	public void stopPositioning(String arg)
	{
	}

	@Override
	public boolean isRunning()
	{
		return false;
	}

	@Override
	public void addOnPositionUpdateListener(OnPositionUpdateListener listener)
	{
		this.listener = listener;
	}

	@Override
	public void setProviderId(String id)
	{
		providerId = id;
	}

	@Override
	public String getProviderId()
	{
		return providerId;
	}

	@Override
	public PositionResult getLatestPosition()
	{
		return latestPosition;
	}

	@Override
	public void startPositioningAfter(int millis, String arg)
	{
	}

	@Override
	public void onBeaconServiceConnect()
	{
		beaconManager.setRangeNotifier(new RangeNotifier()
		{
			@Override
			public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region)
			{
				if (beacons.size() > 0)
				{
					beaconCollection = new ArrayList<>(beacons);
					collectBeaconsFromAPI(new ArrayList<>(beaconCollection));
				}
			}
		});
		try
		{
			if (beaconManager.checkAvailability() && !started)
			{
				started = true;
				final String UUID = this.uuid;
//				beaconManager.startRangingBeaconsInRegion(new Region(UUID, null, null, null));
//				beaconManager.startMonitoringBeaconsInRegion(new Region(UUID, null, null, null));
//				beaconManager.setForegroundScanPeriod(1000);
//				beaconManager.setForegroundBetweenScanPeriod(0);
//				beaconManager.setBackgroundMode(false);
//				beaconManager.updateScanPeriods();

				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						//Set the beacon manager to update faster after a short pause (to allow the app to start up properly)
						new ThreadUtil().sleep(5000);
						try
						{
							beaconManager.startRangingBeaconsInRegion(new Region(UUID, null, null, null));
							beaconManager.setForegroundScanPeriod(500);
							beaconManager.setForegroundBetweenScanPeriod(0);
							beaconManager.setBackgroundMode(false);
							beaconManager.updateScanPeriods();
						}
						catch (RemoteException e)
						{
							dbglog.Log(e.getMessage());
						}
					}
				}).start();
			}
		} catch (Exception e)
		{
			dbglog.Log(e.getMessage());
		}
	}

	private void collectBeaconsFromAPI(final List<Beacon> beacons)
	{
		List<String> beaconIds = new ArrayList<>();
		for (Beacon b : beacons)
		{
			beaconIds.add(b.getId1() + "-" + b.getId2() + "-" + b.getId3());
		}
		beaconProvider.setOnBeaconsReadyListener(new OnBeaconsReadyListener()
		{
			@Override
			public void onBeaconsReady(List<MPBeacon> mpBeacons)
			{
				boolean isChanged = false;
				List<PointXY> measurements = new ArrayList<>();
				for (MPBeacon mpBeacon : mpBeacons)
				{
					for (Beacon beacon : beacons)
					{
						if (beaconEquals(beacon,mpBeacon))
						{
							isChanged = true;
							beaconRSSICacheMgr.add(mpBeacon, beacon.getRssi());
						}
					}
				}
				if (!isChanged)
				{
					return;
				}
				//Get a list of beacons we have heard from within 5 seconds
				ArrayList<BeaconRSSICache> beaconList = beaconRSSICacheMgr.getBeaconList(5000);
				if (beaconList.size() == 0)
				{
					return;
				}
				for (BeaconRSSICache c : beaconList)
				{
					double myDist = positionCalculator.convertRSSItoMeter(c.getAvgVal(), c.beacon.getMaxTxPower());
					measurements.add(new PointXY(c.beacon.getPoint(), myDist));
				}

                /*
                measurements.add( new PointXY(new Point( 57.085958, 9.9569259), 3.9));
                measurements.add( new PointXY(new Point(57.086000, 9.956929), 0.8));
                measurements.add( new PointXY(new Point(57.086006, 9.957032), 1.23));
                measurements.add( new PointXY(new Point(57.085955, 9.957026), 1.0));
                */
				latestMeasurements = measurements;
				updateRSSICircles();
				//Got a number of measurements. Calculate where the origin should be.
				Point newPos = positionCalculator.calcLatLngPos(measurements, isUsingTrilateration);
				if (Double.isNaN(newPos.lat()) || Double.isNaN(newPos.lat()))
				{
					//Unable to calculate a sane position from the given data
					return;
				}
				if (posList == null || !isUsingTrilateration)
				{
					MPPositionResult posResult = new MPPositionResult(newPos, 0, Double.MIN_VALUE, Double.MIN_VALUE);
					posList = new ArrayList<>();
					posList.add(posResult.getPoint());
					latestPosition = posResult;
				}
				else
				{
					if (!isUsingTrilateration)
					{
						//Reuse the poslist, but only keep one entry
						posList.clear();
						MPPositionResult posResult = new MPPositionResult(newPos, 0, Double.MIN_VALUE, Double.MIN_VALUE);
						posList.add(posResult.getPoint());
						latestPosition = posResult;
					}
					else
					{
						double distanceFromPrevious = newPos.distanceTo(latestPosition.getPoint());
						if (distanceFromPrevious > 50 && posList.size() > 1)
						{
							dbglog.Log("Reading not used: Last measured position is far away from previous pos: " + distanceFromPrevious + " meters");
						}
						else if (newPos.z() > 50)
						{
							dbglog.Log("Reading not used: High error distance: " + newPos.z() + " meters");
						}
						else
						{
							posList.add(newPos);
						}
					}
				}
				if (listener != null)
				{
					if (posList.size() > 1)
					{
						posList.remove(0);
					}
					double avgLat = 0;
					double avgLon = 0;
					double avgErr = 0;
					for (Point p : posList)
					{
						avgLat += p.lat();
						avgLon += p.lng();
						avgErr += p.z();
					}
					avgLat /= posList.size();
					avgLon /= posList.size();
					avgErr /= posList.size();
					latestPosition = new MPPositionResult(new Point(avgLat, avgLon, 0), avgErr, Double.MIN_VALUE, Double.MIN_VALUE);
					new Handler(context.getMainLooper()).post(new Runnable()
					{
						@Override
						public void run()
						{
							listener.onPositionUpdate(latestPosition);
						}
					});
				}
			}
		});
		beaconProvider.queryBeacons(clientId, beaconIds.toArray(new String[beaconIds.size()]));
	}

	public void addBeaconsToMap(GoogleMap map, boolean showRSSICircle)
	{
		mMap = map;
		isRSSICircleVisible = showRSSICircle;
	}

	public List<MPBeacon> getBeaconCollection()
	{
		List<BeaconRSSICache> cache = beaconRSSICacheMgr.getBeaconList(5000);
		List<MPBeacon> result = new ArrayList<>(cache.size());
		for( BeaconRSSICache bc : cache )
		{
			result.add(bc.beacon);
		}
		return result;
	}

	private List<PointXY> latestMeasurements = new ArrayList<>();
	private List<Circle> circles = new ArrayList<>();
	private void updateRSSICircles()
	{
		if ( isRSSICircleVisible && mMap != null )
		{
			Handler mainHandler = new Handler(context.getMainLooper());
			final Runnable myRunnable = new Runnable()
			{
				public void run()
				{
					if ( circles == null )
					{
						circles = new ArrayList<>();
						for( int i = 0; i < 16; i++ )
						{
							CircleOptions options = new CircleOptions().center(new LatLng(0,0)).radius(1).fillColor(Color.argb(128, 128, 128, 255)).zIndex(99).strokeWidth(0);
							Circle newCircle = mMap.addCircle(options);
							circles.add( newCircle );
						}
					}
					for( int i = 0; i < circles.size(); i++)
					{
						Circle c = circles.get(i);
						if ( i < latestMeasurements.size())
						{
							PointXY pos = latestMeasurements.get(i);
							c.setCenter(pos.latlng.getGLatLng());
							c.setRadius(pos.distance);
							c.setVisible(true);
						}
						else
						{
							if ( c.isVisible())
							{
								c.setVisible(false);
							}
						}
					}
				}
			};
			mainHandler.post(myRunnable);

		}
	}

	public boolean beaconEquals(Beacon beacon, MPBeacon mpBeacon) {
		return (beacon.getId1() + "-" + beacon.getId2() + "-" + beacon.getId3()).toLowerCase().equals(mpBeacon.getId().toLowerCase());
	}
}
