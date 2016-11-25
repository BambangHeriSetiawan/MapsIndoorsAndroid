package com.mapsindoors.positionprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.LocationSource;
import com.mapspeople.debug.dbglog;
import com.mapspeople.models.Point;
import com.mapspeople.position.MPPositionResult;
import com.mapspeople.position.OnPositionUpdateListener;
import com.mapspeople.position.PositionProvider;
import com.mapspeople.position.PositionResult;

public class GoogleAPIPositionProvider extends Activity implements PositionProvider, LocationListener, LocationSource, android.location.LocationListener
{
	private List<OnPositionUpdateListener> listeners;
	private String providerId;
	private PositionResult latestPosition;
	private boolean isRunning;
	private Context context;
	private LocationRequest mLocationRequest;
	private GoogleApiClient mGoogleApiClient;
	private LocationManager locationManager;

	public GoogleAPIPositionProvider(Context context)
	{
		super();
		this.context = context;
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		// Define a listener that responds to location updates
		isRunning = false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	public void startPositioning(String arg)
	{
		if (!isRunning)
		{
			isRunning = true;
			if (mGoogleApiClient == null)
			{
				mGoogleApiClient = new GoogleApiClient.Builder(context)
						.addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
						{
							@Override
							public void onConnected(@Nullable Bundle bundle)
							{
								requestLocationStart();
							}

							@Override
							public void onConnectionSuspended(int cause)
							{
								if (cause == CAUSE_SERVICE_DISCONNECTED)
								{
									dbglog.Log("Disconnected. Please re-connect.");
								}
								else if (cause == CAUSE_NETWORK_LOST)
								{
									dbglog.Log("Network lost. Please re-connect.");
								}
							}
						})
						.addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
						{
							@Override
							public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
							{
								dbglog.Log("Failed to connect to LocationServices");
							}
						})
						.addApi(com.google.android.gms.location.LocationServices.API)
						.build();
			}
			mGoogleApiClient.connect();
			if (listeners != null)
			{
				for (OnPositionUpdateListener listener : listeners)
				{
					listener.onPositioningStarted(this);
				}
			}
		}
	}

	void requestLocationStart()
	{
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(5000);
		mLocationRequest.setFastestInterval(2000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
		final LocationListener locationListener = this;
		final PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
		result.setResultCallback(new ResultCallback<LocationSettingsResult>()
		{
			@Override
			public void onResult(@NonNull LocationSettingsResult locationSettingsResult)
			{
				int statusCode = locationSettingsResult.getStatus().getStatusCode();
				if (statusCode == LocationSettingsStatusCodes.SUCCESS)
				{
					LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, locationListener);
				}
				else
				{
					dbglog.Log("Failed to connect to LocationServices. Using the native LocationManager as fallback.");
					requestLocationStartLow();
				}
			}
		});
	}

	void requestLocationStartLow()
	{
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, this);
		onLocationChanged( locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) );
	}

	@Override
	public void stopPositioning(String arg)
	{
		if (isRunning)
		{
			if ( mGoogleApiClient != null )
			{
				mGoogleApiClient.disconnect();
			}
			isRunning = false;
		}
	}

	@Override
	public void addOnPositionUpdateListener(OnPositionUpdateListener listener)
	{
		if (this.listeners == null)
		{
			this.listeners = new ArrayList<>();
		}
		if ( listener != null )
		{
			this.listeners.add(listener);
		}
	}

	@Override
	public void setProviderId(String id)
	{
		providerId = id;
	}

	@Override
	public PositionResult getLatestPosition()
	{
		return latestPosition;
	}

	@Override
	public boolean isRunning()
	{
		return isRunning;
	}

	@Override
	public String getProviderId()
	{
		return providerId;
	}

	public void onLocationChanged(Location location)
	{
		if (location != null && isRunning())
		{
			latestPosition = new MPPositionResult(new Point(location.getLatitude(), location.getLongitude()), 0, location.getBearing(), location.getTime());
			latestPosition.setProvider(this);
			for (OnPositionUpdateListener listener : listeners)
			{
				listener.onPositionUpdate(latestPosition);
			}
		}
	}

	@Override
	public void startPositioningAfter(int millis, final String arg)
	{
		Timer restartTimer = new Timer();
		restartTimer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				startPositioning(arg);
			}
		}, millis);
	}

	@Override
	public void activate(OnLocationChangedListener onLocationChangedListener)
	{
	}

	@Override
	public void deactivate()
	{
	}


	//	android.location.LocationListener methods
	@Override
	public void onStatusChanged(String s, int i, Bundle bundle)
	{
	}

	@Override
	public void onProviderEnabled(String s)
	{
	}

	@Override
	public void onProviderDisabled(String s)
	{
	}

}
