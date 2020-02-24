package com.example.zrakandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    String username;
    String password;
    Boolean loggedIn;
    final String URL_DEVICES = "https://api.zrak.janvr.wtf/devices";
    SimpleAdapter devices_adapter;
    ArrayList<Map<String, String>> devices_list;
    ListView devices_lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(getString(R.string.login_shared_pref), MODE_PRIVATE);
        loggedIn = sharedPrefs.getBoolean("authorized", false);

        if (!loggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        username = sharedPrefs.getString("username", null);
        password = sharedPrefs.getString("password", null);
        String auth = getString(R.string.basic_auth, username, password);

        devices_lv = findViewById(R.id.main_devices_list);

        RequestQueue rq = VolleySingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        rq.add(createDevicesRequest(auth));

    }


    private JsonObjectRequest createDevicesRequest(final String auth) {
        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, URL_DEVICES, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
//                        TextView tv = findViewById(R.id.main_message);
//                        tv.setText(response.toString());

                        devices_list = new ArrayList<>();
                        Iterator<String> keys = response.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            try {
                                Log.d("zrak", response.getJSONObject(key).toString());
                                JSONObject dev = response.getJSONObject(key);
                                Map<String, String> dev_map = new HashMap<String, String>();
                                dev_map.put("name", dev.getString("name"));
                                dev_map.put("location", dev.getString("location"));
                                devices_list.add(dev_map);
                            } catch (JSONException e) {
                                Log.d("zrak", e.toString());
                            }
                        }

                        devices_adapter = new SimpleAdapter(getApplicationContext(), devices_list,
                                R.layout.device_card, new String[]{"name", "location"},
                                new int[]{R.id.device_card_name, R.id.device_card_location});
                        devices_lv.setAdapter(devices_adapter);

                        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                TextView name_tv = view.findViewById(R.id.device_card_name);
                                String dev_name = name_tv.getText().toString();
                                Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
                                intent.putExtra("device_name", dev_name);
                                startActivity(intent);
                            }
                        };
                        devices_lv.setOnItemClickListener(clickListener);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("zrak", "network error");
                        if (error.networkResponse == null) return;
                        try {
                            TextView tv = findViewById(R.id.main_message);
                            tv.setText(new String(error.networkResponse.data, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            Log.d("zrak", "exception");
                        }

                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Basic " + Base64.encodeToString(auth.getBytes(), Base64.DEFAULT));
                return params;
            }

        };
        return req;
    }
}
