package wtf.janvr.zrakandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

public class DeviceActivity extends AppCompatActivity {

    ArrayList<Map<String, String>> measurements;
    ListView meas_lv;
    SimpleAdapter meas_lv_adapter;
    Calendar calendar;
    String dev_id;
    String auth;
    EditText limit_tv;
    TextView stop_date_tv;
    TextView stop_time_tv;
    TextView start_time_tv;
    TextView start_date_tv;
    TextView message_tv;
    int DEFAULT_MEAS_LIM = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(getString(R.string.login_shared_pref), MODE_PRIVATE);
        String username = sharedPrefs.getString("username", null);
        String password = sharedPrefs.getString("password", null);
        auth = getString(R.string.basic_auth, username, password);

        Intent intent = this.getIntent();
        String dev_name = intent.getStringExtra("device_name");
        dev_id = intent.getStringExtra("device_id");

        setTitle(dev_name);
        limit_tv = findViewById(R.id.device_limit_text);
        limit_tv.setText(String.valueOf(DEFAULT_MEAS_LIM));
        start_time_tv = findViewById(R.id.device_start_time_text);
        start_date_tv = findViewById(R.id.device_start_date_text);
        stop_time_tv = findViewById(R.id.device_stop_time_text);
        stop_date_tv = findViewById(R.id.device_stop_date_text);
        message_tv = findViewById(R.id.device_message);

        if (start_time_tv.getText().toString() == null) {
            Log.d("jan", "gettextjenull");
        }
        Log.d("jan", "start_time_text: " + start_time_tv.getText().toString());

        meas_lv = findViewById(R.id.meas_listview);
        getMeasurements();
    }

    public void startTimePicker(View v) {
        TimePickerFragment tpf = new TimePickerFragment(TimePickerFragment.START_TIME_PICKER);
        tpf.show(getSupportFragmentManager(), "startTimePicker");
    }

    public void stopTimePicker(View v) {
        TimePickerFragment tpf = new TimePickerFragment(TimePickerFragment.STOP_TIME_PICKER);
        tpf.show(getSupportFragmentManager(), "stopTimePicker");
    }

    public void startDatePicker(View v) {
        DatePickerFragment dpf = new DatePickerFragment(DatePickerFragment.START_DATE_PICKER);
        dpf.show(getSupportFragmentManager(), "startDatePicker");
    }

    public void stopDatePicker(View v) {
        DatePickerFragment dpf = new DatePickerFragment(DatePickerFragment.STOP_DATE_PICKER);
        dpf.show(getSupportFragmentManager(), "stopDatePicker");
    }


//    private void getDevice(String auth, String id) {
//        RequestQueue rq = VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
//        rq.add(createDeviceRequest(auth, id));
//    }

    private void getMeasurements() {
        String start_time = start_time_tv.getText().toString();
        String start_date = start_date_tv.getText().toString();
        String stop_time = stop_time_tv.getText().toString();
        String stop_date = stop_date_tv.getText().toString();
        String limit = limit_tv.getText().toString();
        String start_datetime_utc = null;
        String stop_datetime_utc = null;

        SimpleDateFormat sdf_inp = new SimpleDateFormat("HH:mm dd.MM.yyyy");
        SimpleDateFormat sdf_out = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        sdf_out.setTimeZone(TimeZone.getTimeZone("UTC"));

        message_tv.setVisibility(View.GONE);

        if (start_time.equals("") ^ start_date.equals("")) {
            message_tv.setText("Please enter both, start date and start time");
            message_tv.setVisibility(View.VISIBLE);
            return;
        } else {
            if (start_time.equals("") && start_date.equals("")) {
                start_datetime_utc = null;
            } else {
                try {
                    Date date_start = sdf_inp.parse(start_time + " " + start_date);
                    start_datetime_utc = sdf_out.format(date_start);

                    Log.d("jan", start_datetime_utc);
                } catch (ParseException pe) {
                    Log.d("jan", pe.toString());
                }
            }
        }

        if (stop_time.equals("") ^ stop_date.equals("")) {
            message_tv.setText("Please enter both, stop date and stop time");
            message_tv.setVisibility(View.VISIBLE);
            return;
        } else {
            if (stop_time.equals("") && stop_date.equals("")) {
                stop_datetime_utc = null;
            } else {
                try {
                    Date date_stop = sdf_inp.parse(stop_time + " " + stop_date);
                    stop_datetime_utc = sdf_out.format(date_stop);

                    Log.d("jan", stop_datetime_utc);
                } catch (ParseException pe) {
                    Log.d("jan", pe.toString());
                }
            }
        }

        if (limit == "") {
            limit = null;
        }

        RequestQueue rq = VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
        rq.add(createMeasurementsRequest(auth, dev_id, start_datetime_utc, stop_datetime_utc, limit));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_icon:
                Toast.makeText(this, "refresh", Toast.LENGTH_SHORT).show();
                getMeasurements();
                break;
            case R.id.device_menu_reset_time:
                start_date_tv.setText("");
                stop_date_tv.setText("");
                start_time_tv.setText("");
                stop_time_tv.setText("");
                break;

        }
        return true;
    }

    private JsonObjectRequest createMeasurementsRequest(final String auth, final String dev_id,
                                                        @Nullable final String start,
                                                        @Nullable final String stop,
                                                        @Nullable final String lim) {
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
                                String utc_time = json.getString("time");
                                SimpleDateFormat sdf_inp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                sdf_inp.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date meas_date = sdf_inp.parse(utc_time);
                                SimpleDateFormat sdf_out = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                                measurement.put("time", sdf_out.format(meas_date));
                                StringBuilder values_builder = new StringBuilder();
                                Iterator<String> keys2 = json.keys();
                                while (keys2.hasNext()) {
                                    String key2 = keys2.next();
                                    if (!key2.equals("time") && !key2.equals("dev_id") && !key2.equals("id")) {
                                        values_builder.append(key2).append(": ").append(json.getString(key2)).append(" ");
                                    }
                                }
                                measurement.put("values", values_builder.toString());
                            } catch (Exception e) {
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
                        Toast.makeText(DeviceActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();

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

    /*private JsonObjectRequest createDeviceRequest(final String auth, final String dev_id) {
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
                        Toast.makeText(DeviceActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();

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
    }*/
}
