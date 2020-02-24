package com.example.zrakandroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    final String URL_USERS = "https://api.zrak.janvr.wtf/users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }


    public void login(View view) {
        String username = ((TextView) findViewById(R.id.login_username)).getText().toString();
        String password = ((TextView) findViewById(R.id.login_password)).getText().toString();

        RequestQueue rq = VolleySingleton.getInstance(this.getApplicationContext()).getRequestQueue();

        rq.add(createLoginRequest(username, password));
    }

    public JsonObjectRequest createLoginRequest(final String username, final String password) {
        final String auth = getString(R.string.basic_auth, username, password);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, URL_USERS, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("zrak", "onResponse");
                        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getString(R.string.login_shared_pref), MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("username", username);
                        editor.putString("password", password);
                        editor.putBoolean("authorized", true);
                        editor.commit();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("zrak", "network error");

                        TextView message = findViewById(R.id.login_message);
                        if (error.networkResponse == null) return;
                        try {
                            message.setText(new String(error.networkResponse.data, "UTF-8"));
                            message.setVisibility(View.VISIBLE);
                        } catch (UnsupportedEncodingException e) {
                            Log.d("zrak", "exception");
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                String encoded = Base64.encodeToString(auth.getBytes(), Base64.DEFAULT);
                headers.put("Authorization", "Basic " + encoded);
                return headers;
            }
        };
        return req;
    }
}
