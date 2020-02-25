package wtf.janvr.zrakandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DeviceActivity extends AppCompatActivity {

    ArrayList<Map<String, String>> measurements;
    ListView meas_lv;
    SimpleAdapter meas_lv_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(getString(R.string.login_shared_pref), MODE_PRIVATE);
        String username = sharedPrefs.getString("username", null);
        String password = sharedPrefs.getString("password", null);
        String auth = getString(R.string.basic_auth, username, password);

        Intent intent = this.getIntent();
        String dev_name = intent.getStringExtra("device_name");
        String dev_id = intent.getStringExtra("device_id");

        setTitle(dev_name);

        meas_lv = findViewById(R.id.meas_listview);
        getMeasurements(auth, dev_id, null, null, 30);
    }

    private void getDevice(String auth, String id) {
        RequestQueue rq = VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
        rq.add(createDeviceRequest(auth, id));
    }

    private void getMeasurements(String auth, String id, String start, String stop, int lim) {
        RequestQueue rq = VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
        rq.add(createMeasurementsRequest(auth, id, start, stop, String.valueOf(lim)));
    }

    private JsonObjectRequest createMeasurementsRequest(final String auth, final String dev_id,
                                                        final String start, final String stop,
                                                        final String lim) {
        String url = MainActivity.URL_MEASUREMENTS;
        url += "?device_id=" + dev_id;
        if (start != null) url += "&start=" + start;
        if (stop != null) url += "&stop=" + stop;
        if (lim != null) url += "&lim=" + lim;
        Log.d("jan", url);

        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("jan", response.toString());

                        measurements = new ArrayList<>();
                        Iterator<String> keys = response.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            Map<String, String> measurement = new HashMap<>();
                            try {
                                JSONObject json = (JSONObject) response.get(key);
                                measurement.put("time", json.getString("time"));
                                String values = "";
                                Iterator<String> keys2 = json.keys();
                                while (keys2.hasNext()) {
                                    String key2 = keys2.next();
                                    if (!key2.equals("time") && !key2.equals("dev_id") && !key2.equals("id")) {
                                        values += key2 + "=" + json.getString(key2) + " ";
                                    }
                                }
                                measurement.put("values", values);
                            } catch (JSONException e) {
                                Log.d("jan", e.toString());
                            }
                            measurements.add(measurement);
                        }

                        Log.d("jan", measurements.toString());
                        Collections.sort(measurements, new MapComparator("time", false));
                        meas_lv_adapter = new SimpleAdapter(getApplicationContext(), measurements,
                                R.layout.measurement_card, new String[]{"time", "values"},
                                new int[]{R.id.meas_card_time, R.id.meas_card_val});
                        meas_lv.setAdapter(meas_lv_adapter);

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse == null) return;
                        Log.d("jan", new String(error.networkResponse.data, StandardCharsets.UTF_8));
                        Log.d("jan", error.networkResponse.headers.toString());

                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Basic " + Base64.encodeToString(auth.getBytes(), Base64.DEFAULT));
                return headers;
            }
        };
        req.setShouldCache(false);
        return req;
    }

    private JsonObjectRequest createDeviceRequest(final String auth, final String dev_id) {
        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, MainActivity.URL_DEVICES, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("zrak", "network error");
                        if (error.networkResponse == null) return;
                        TextView tv = findViewById(R.id.main_message);
                        tv.setText(new String(error.networkResponse.data, StandardCharsets.UTF_8));

                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Basic " + Base64.encodeToString(auth.getBytes(), Base64.DEFAULT));
                return headers;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("device_id", dev_id);
                return params;
            }
        };
        return req;
    }
}
