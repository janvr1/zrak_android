package wtf.janvr.zrakandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class MainActivity extends AppCompatActivity {

    String username;
    String password;
    Boolean loggedIn;
    private final int MENU_LOGOUT = 0;
    private final int MENU_REFRESH_DEVICES = 1;
    public static final String URL_USERS = "https://api.zrak.janvr.wtf/users";
    public static final String URL_DEVICES = "https://api.zrak.janvr.wtf/devices";
    public static final String URL_MEASUREMENTS = "https://api.zrak.janvr.wtf/measurements";
    SimpleAdapter devices_adapter;
    ArrayList<Map<String, String>> devices_list;
    GridView devices_lv;
    String auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(getString(R.string.devices_overview));

//        Toolbar toolbar = findViewById(R.id.main_toolbar);
//        setSupportActionBar(toolbar);

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
        auth = getString(R.string.basic_auth, username, password);

        devices_lv = findViewById(R.id.main_devices_list);

        getDevices(auth);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_REFRESH_DEVICES, 0, "Refresh");
        MenuItem refresh = menu.findItem(MENU_REFRESH_DEVICES);
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        refresh.setIcon(R.drawable.ic_refresh_black_24dp);
        menu.add(0, MENU_LOGOUT, 1, "Log out");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemID = item.getItemId();
        switch (itemID) {
            case MENU_LOGOUT:
                logOut();
                break;
            case MENU_REFRESH_DEVICES:
                Toast.makeText(this, "refresh devices", Toast.LENGTH_SHORT).show();
                getDevices(auth);
                break;
        }
        return true;
    }

    public void logOut() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getString(R.string.login_shared_pref), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("username");
        editor.remove("password");
        editor.putBoolean("authorized", false);
        editor.commit();

        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void getDevices(String auth) {
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
                                dev_map.put("id", String.valueOf(dev.getInt("id")));
                                devices_list.add(dev_map);
                            } catch (JSONException e) {
                                Log.d("zrak", e.toString());
                            }
                        }

                        Collections.sort(devices_list, new MapComparator("name", true));
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
                                Log.d("zrak", "device_id: " + devices_list.get(position).get("id"));
                                intent.putExtra("device_id", devices_list.get(position).get("id"));
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

        };
        req.setShouldCache(false);
        return req;
    }
}
