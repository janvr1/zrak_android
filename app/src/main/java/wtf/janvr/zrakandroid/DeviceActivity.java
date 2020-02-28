package wtf.janvr.zrakandroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

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

    String dev_id;
    String auth;
    EditText limit_tv;
    TextView stop_date_tv, stop_time_tv, start_time_tv, start_date_tv, message_tv;
    int DEFAULT_MEAS_LIM = 20;
    ConstraintLayout constraintLayout;
    CardView stop_cv, start_cv, measurements_card_view;
    TableLayout measurementsTable;

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
        measurementsTable = findViewById(R.id.measurements_table);
        measurements_card_view = findViewById(R.id.measurements_card_view);

        constraintLayout = findViewById(R.id.device_constraint_layout);

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                limit_tv.clearFocus();
                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return false;
            }
        };

        constraintLayout.setOnTouchListener(onTouchListener);
        measurements_card_view.setOnTouchListener(onTouchListener);
        measurementsTable.setOnTouchListener(onTouchListener);

        stop_cv = findViewById(R.id.device_stop_card);
        start_cv = findViewById(R.id.device_start_card);

        View.OnLayoutChangeListener layoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int stop_width = stop_cv.getWidth();
                int start_width = start_cv.getWidth();

                if (stop_width == 0 || start_width == 0) {
                    return;
                }

                ConstraintSet constraints = new ConstraintSet();
                constraints.clone(constraintLayout);
                constraints.clear(R.id.device_stop_card, ConstraintSet.RIGHT);
                constraints.clear(R.id.device_start_card, ConstraintSet.RIGHT);
                constraints.applyTo(constraintLayout);

                stop_width = stop_cv.getWidth();
                start_width = start_cv.getWidth();

                if (start_width > stop_width) {
                    constraints.connect(R.id.device_stop_card, ConstraintSet.RIGHT, R.id.device_start_card, ConstraintSet.RIGHT);
                    constraints.applyTo(constraintLayout);
                }
                if (start_width < stop_width) {
                    constraints.connect(R.id.device_start_card, ConstraintSet.RIGHT, R.id.device_stop_card, ConstraintSet.RIGHT);
                    constraints.applyTo(constraintLayout);
                }
            }
        };

        stop_cv.addOnLayoutChangeListener(layoutChangeListener);
        start_cv.addOnLayoutChangeListener(layoutChangeListener);
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

                } catch (ParseException pe) {
                    Toast.makeText(this, R.string.error_message, Toast.LENGTH_SHORT).show();
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

                } catch (ParseException pe) {
                    Toast.makeText(this, R.string.error_message, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.refresh_measurements, Toast.LENGTH_SHORT).show();
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

        JsonObjectRequest req = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Iterator<String> iter = response.keys();
                            if (!iter.hasNext()) {
                                message_tv.setText(R.string.no_measurements_message);
                                message_tv.setVisibility(View.VISIBLE);
                                measurements_card_view.setVisibility(View.GONE);
                                return;
                            }

                            message_tv.setVisibility(View.GONE);
                            measurements_card_view.setVisibility(View.VISIBLE);

                            measurementsTable.removeAllViews();

                            Iterator<String> var_i = ((JSONObject) response.get("0")).keys();
                            ArrayList<String> variables = new ArrayList();
                            Context ctx = getApplicationContext();
                            TableRow th = new TableRow(ctx);


                            ArrayList<JSONObject> measurements_array = new ArrayList<>();
                            while (iter.hasNext()) {
                                measurements_array.add((JSONObject) response.get(iter.next()));
                            }
                            JSONComparator comparator = new JSONComparator("time", false);
                            Collections.sort(measurements_array, comparator);

                            while (var_i.hasNext()) {
                                String s = var_i.next();
                                if (!s.equals("dev_id") && !s.equals("id")) {
                                    TextView tv = new TextView(ctx);
                                    tv.setTypeface(null, Typeface.BOLD);
                                    tv.setTextSize(18);
                                    tv.setPadding(4, 4, 16, 4);
                                    tv.setTextColor(getResources().getColor(R.color.myTextColor, ctx.getTheme()));
                                    if (s.equals("time")) {
                                        tv.setText("Time");
                                        th.addView(tv, 0);
                                        variables.add(0, s);
                                    } else {
                                        tv.setText(s);
                                        th.addView(tv);
                                        variables.add(s);
                                    }
                                }
                            }
                            measurementsTable.addView(th);
                            Iterator<String> keys = response.keys();

                            SimpleDateFormat sdf_inp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            sdf_inp.setTimeZone(TimeZone.getTimeZone("UTC"));
                            SimpleDateFormat sdf_out = new SimpleDateFormat("HH:mm'\n'dd.MM.yyyy");

                           /* while (keys.hasNext()) {
                                String meas_key = keys.next();*/
                            for (JSONObject measurement : measurements_array) {
                                TableRow tr = new TableRow(getApplicationContext());
                                tr.setGravity(Gravity.CENTER_VERTICAL);
                                for (String variable : variables) {
                                    TextView tv = new TextView(ctx);
                                    tv.setTextSize(16);
                                    tv.setPadding(4, 4, 16, 4);
                                    tv.setTextColor(getResources().getColor(R.color.myTextColor, ctx.getTheme()));
                                    if (variable.equals("time")) {
//                                        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                        Date meas_date = sdf_inp.parse(measurement.get(variable).toString());
                                        tv.setText(sdf_out.format(meas_date));
                                    } else {
                                        String val = measurement.get(variable).toString();
                                        if (val.endsWith(".0")) {
                                            val = val.substring(0, val.indexOf("."));
                                        }
                                        tv.setText(val);
                                    }
                                    tr.addView(tv);
                                }
                                measurementsTable.addView(tr);

                            }
//                        Log.d("jan_variables", variables.toString());
                        } catch (Exception e) {
                            //bla bla
                            Log.d("janexception", e.toString());
                        }


                        /*Log.d("jan", response.toString());

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
*/
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
