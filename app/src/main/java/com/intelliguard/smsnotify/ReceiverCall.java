package com.smsnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by David on 21/12/2017.
 */

public class ReceiverCall extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Service Stops", "Ohhhhhhh");
        context.startService(new Intent(context, ServiceTest.class));;
    }

}
