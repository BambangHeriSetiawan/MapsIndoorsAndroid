package com.mapsindoors.positionprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mapsindoors.MapsIndoorsActivity;

/**
 * BootUpReciever created by mh on 08-05-2015.
 */
public class BootUpReciever extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent args)
    {
        incBootCounter(context);
        Intent i = new Intent(context, MapsIndoorsActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    private void incBootCounter(Context ctx)
    {
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (s != null)
        {
            SharedPreferences.Editor edit = s.edit();
            if (edit != null)
            {
                edit.putInt("how_many_times_have_we_booted", s.getInt("how_many_times_have_we_booted", 0) + 1);
                edit.apply();
            }
        }
    }
}