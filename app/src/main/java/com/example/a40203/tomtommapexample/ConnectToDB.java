package com.example.a40203.tomtommapexample;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConnectToDB {


    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();


    JSONObject roadJSON(String streetname, String point) {
        JSONObject postBody = new JSONObject();
        try {
            postBody.put("action", "searchDB");
            postBody.put("streetname", streetname);
            postBody.put("point", point);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return postBody;
    }


    public void sendRequest(String url, JSONObject jsonObject){

        Log.w("server", "json: " + jsonObject.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, jsonObject.toString()))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("unexpected server error " +response);
                }

                String reposnseString = response.body().string();
                    MapActivity.getResponse(reposnseString);
            }
        });
    }
}
