package com.smsnotify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;


/*
REMOVE SENDING LIMITS
https://www.xda-developers.com/change-sms-limit-android/
Enter an ADB shell by sending the following command in the command prompt or terminal window: adb shell
Then run the following command in the ADB shell to change the max SMS limit option: settings put global sms_outgoing_check_max_count 5
So in this example, I am actually reducing the number so Android warns me if I have sent more than 5 SMS messages within 30 minutes. Change “5” to whatever number you want.
Next, you can run the following command to also change the time frame: settings put global sms_outgoing_check_interval_ms 9000000
android sms limit
And with this command I am reducing the time frame for this check from 30 minutes, to 15 minutes (this value is in milliseconds). You can choose any integer value here for the time frame, just make sure it’s something sensible.
 */

public class ServiceTest extends Service {
    private static final String TAG = "SMSNOTIFY";
    private static final String SENT = "SMS_SENT";
    private String key = "";
    private String Endpoint = "";
    private JSONArray pendingMessages = new JSONArray();
    private int currentMsgId = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ignorePowerOptimisation();
        //createWakeLock();
        registerReceiver();
        mTimer = new Timer();
        mTimer.schedule(timerTask, 1000, 60 * 1000);
    }

    private void createWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
    }

    private void ignorePowerOptimisation() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (Build.VERSION.SDK_INT >= 23 && !pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    private void registerReceiver() {
        //---when the SMS has been sent---
        this.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                int resultCode = getResultCode();
                Log.v(TAG, "BroadcastReceiver Result: " + resultCode);
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.v(TAG, "BroadcastReceiver SMS sent: " + currentMsgId);
                        if (currentMsgId > 0) {
                            try {
                                Runnable r = new MarkSMSSent(currentMsgId);
                                Thread t = new Thread(r);
                                t.start();
                                t.join();
                            } catch (InterruptedException ex) {
                                Log.e(TAG, "InterruptedException", ex);
                            }
                        }
                        SendPendingMessages();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.e(TAG, "BroadcastReceiver SMS RESULT_ERROR_GENERIC_FAILURE");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Log.e(TAG, "BroadcastReceiver SMS RESULT_ERROR_NO_SERVICE");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Log.e(TAG, "BroadcastReceiver SMS RESULT_ERROR_NULL_PDU");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Log.e(TAG, "BroadcastReceiver SMS RESULT_ERROR_RADIO_OFF");
                        break;
                }
            }
        }, new IntentFilter(SENT));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private Timer mTimer;

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Log.v(TAG, "Running");
            //get api key
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
            key = sharedPref.getString(getString(R.string.preferences_key), "");
            //get endpoints
            sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences_endpoints), Context.MODE_PRIVATE);
            Map<String, ?> prefs = sharedPref.getAll();
            for (Map.Entry<String, ?> entry : prefs.entrySet()) {
                Endpoint = entry.getValue().toString();
                String pendingURL = Endpoint + "/SMSPending";
                if (key != null && key != "") pendingURL += "?key=" + key;
                //get pending messages
                String pendingRaw = HttpGetRequest(pendingURL);
                Log.v(TAG, "pendingRaw");
                Log.v(TAG, pendingRaw);

                if (pendingRaw != "") {
                    try {
                        pendingMessages = new JSONArray(pendingRaw);
                        SendPendingMessages();
                    } catch (JSONException ex) {
                        Log.e(TAG, "JSONException", ex);
                    }
                }
            }
        }
    };

    void SendPendingMessages() {
        if (pendingMessages != null && pendingMessages.length() > 0) {
            try {
                JSONObject log = pendingMessages.getJSONObject(0);
                pendingMessages.remove(0);
                int msgId = log.getInt("id");
                String mobile = log.getString("mobile");
                String msg = log.getString("message");
                currentMsgId = msgId;
                Log.v(TAG, "New msg: " + msgId);
                SendSMS(msgId, mobile, msg);
            } catch (JSONException ex) {
                Log.e(TAG, "JSONException", ex);
            }
        }
    }

    private String HttpGetRequest(String stringUrl) {
        String REQUEST_METHOD = "GET";
        int READ_TIMEOUT = 15000;
        int CONNECTION_TIMEOUT = 15000;
        String result = "";
        String inputLine;
        try {
            Log.v(TAG, "HttpGetRequest Sending: " + stringUrl);
            //Create a URL object holding our url
            URL myUrl = new URL(stringUrl);
            //Create a connection
            HttpURLConnection connection = (HttpURLConnection) myUrl.openConnection();
            //Set methods and timeouts
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);

            //Connect to our url
            connection.connect();
            //Create a new InputStreamReader
            InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            //Check if the line we are reading is not null
            while ((inputLine = reader.readLine()) != null) {
                stringBuilder.append(inputLine);
            }
            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();
            //Set our result equal to our stringBuilder
            result = stringBuilder.toString();
            Log.v(TAG, "HttpGetRequest Received: " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void SendSMS(int msgId, String phoneNumber, String message) {
        try {
            Log.v(TAG, "SendSMS Sending: " + msgId);
            Context context = this.getApplicationContext();
            //create sent intent
            Intent SentIntent = new Intent(SENT);
            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, SentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
            sentPIs.add(sentPI);
            //get sms manager
            SmsManager sms = SmsManager.getDefault();
            //split sms if longer than 155 chars
            ArrayList<String> messageParts = sms.divideMessage(message);
            //send sms as multi part
            sms.sendMultipartTextMessage(phoneNumber, null, messageParts, sentPIs, null);
            Log.v(TAG, "SendSMS Sent: " + msgId);
        } catch (Exception e) {
            Log.e(TAG, "SendSMS", e);
        }
    }

    public class MarkSMSSent implements Runnable {
        private int _msgId;

        public MarkSMSSent(int msgId) {
            _msgId = msgId;
        }

        public void run() {
            String stringUrl = Endpoint + "/SMSSent?id=" + _msgId;
            if (key != null && key != "") stringUrl += "&key=" + key;
            String REQUEST_METHOD = "GET";
            int READ_TIMEOUT = 15000;
            int CONNECTION_TIMEOUT = 15000;
            String inputLine;
            try {
                Log.v(TAG, "MarkSMSSent Sending: " + stringUrl);
                //Create a URL object holding our url
                URL myUrl = new URL(stringUrl);
                //Create a connection
                HttpURLConnection connection = (HttpURLConnection) myUrl.openConnection();
                //Set methods and timeouts
                connection.setRequestMethod(REQUEST_METHOD);
                connection.setReadTimeout(READ_TIMEOUT);
                connection.setConnectTimeout(CONNECTION_TIMEOUT);

                //Connect to our url
                connection.connect();
                //Create a new InputStreamReader
                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                //Create a new buffered reader and String Builder
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();
                //Check if the line we are reading is not null
                while ((inputLine = reader.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                //Close our InputStream and Buffered reader
                reader.close();
                streamReader.close();
                //Set our result equal to our stringBuilder
                Log.v(TAG, "MarkSMSSent Received: " + stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onDestroy() {
        try {
            mTimer.cancel();
            timerTask.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent("com.smsnotify");
        sendBroadcast(intent);
    }
}
