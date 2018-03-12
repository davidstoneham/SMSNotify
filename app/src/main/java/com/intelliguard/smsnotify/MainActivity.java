package com.smsnotify;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addListeners();
        setupListview();
        setupApiKey();
        checkPermission();
    }

    private void checkPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 0);
        } else {
            startService(new Intent(this, ServiceTest.class));
        }
    }

    private void setupListview() {
        //add adapter
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        final ListView lstEndpoints = findViewById(R.id.lstEndpoints);
        lstEndpoints.setAdapter(adapter);

        //add items to list
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preferences_endpoints), Context.MODE_PRIVATE);
        Map<String, ?> prefs = sharedPref.getAll();
        for (Map.Entry<String, ?> entry : prefs.entrySet()) {
            listItems.add(entry.getValue().toString());
        }

        //link on long hold
        lstEndpoints.setLongClickable(true);
        lstEndpoints.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                return listItem_onLongClick(position);
            }
        });
    }

    private boolean listItem_onLongClick(int position) {
        //remove endpoint from shared pref
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preferences_endpoints), Context.MODE_PRIVATE);
        Map<String, ?> prefs = sharedPref.getAll();
        Set keys = prefs.keySet();
        String key = keys.toArray()[position].toString();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(key);
        editor.commit();
        //update list
        listItems.remove(position);
        adapter.notifyDataSetChanged();
        return true;
    }

    private void setupApiKey() {
        final Button btnSaveKey = findViewById(R.id.btnSaveKey);
        btnSaveKey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnSaveKey_onClick();
            }
        });

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        String apiKey = sharedPref.getString(getString(R.string.preferences_key), "");
        final EditText txtKey = findViewById(R.id.txtKey);
        txtKey.setText(apiKey);
    }

    private void addListeners() {
        final Button btnAddSource = findViewById(R.id.btnAddSource);
        btnAddSource.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btnAddSource_onClick();
            }
        });
    }

    private void btnAddSource_onClick() {
        //get endpoint text
        final EditText txtEndpoint = findViewById(R.id.txtEndpoint);
        String endpoint = txtEndpoint.getText().toString();

        //add to shared preferences
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preferences_endpoints), Context.MODE_PRIVATE);
        Map<String, ?> prefs = sharedPref.getAll();
        String nextId = "1";
        //get next preference key
        if (prefs.size() > 0) {
            Set keys = prefs.keySet();
            int lastId = Integer.parseInt(keys.toArray()[keys.size() - 1].toString());
            lastId++;
            nextId = Integer.toString(lastId);
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(nextId, endpoint);
        editor.commit();
        //add to list
        listItems.add(endpoint);
        adapter.notifyDataSetChanged();
    }

    private void btnSaveKey_onClick() {
        //get endpoint text
        final EditText txtKey = findViewById(R.id.txtKey);
        String endpoint = txtKey.getText().toString();

        //add to shared preferences
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.preferences_key), endpoint);
        editor.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startService(new Intent(this, ServiceTest.class));
        } else {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
        }
    }

}
