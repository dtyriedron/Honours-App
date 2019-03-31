package com.example.a40203.tomtommapexample;

import android.os.AsyncTask;

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

//    String post(String url, String json) throws IOException {
//        RequestBody body = RequestBody.create(JSON, json);
//        Request request = new Request.Builder()
//                .url(url)
//                .post(body)
//                .build();
//        try (Response response = client.newCall(request).execute()) {
//            return response.body().string();
//        }
//    }

    JSONObject roadJSON(String location, String speedbumps, String trafficlights, String time, String speedlimit) {
        JSONObject postBody = new JSONObject();
        try {
            postBody.put("location", location);
            postBody.put("speedbumps", speedbumps);
            postBody.put("trafficlights", trafficlights);
            postBody.put("time", time);
            postBody.put("speedlimit", speedlimit);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return postBody;
    }

//    public static void main(String[] args) throws IOException {
//        ConnectToDB connectToDB = new ConnectToDB();
//        JSONObject json = connectToDB.roadJSON("edinburgh", "2", "3", "45", "40");
//        String response = connectToDB.sendRequest("http://192.168.0.33/text.php", json);
//        System.out.println(response);
//    }


    public void sendRequest(String url, JSONObject jsonObject){

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

                final String reposnseString = response.body().string();
//                    MainActivity.getResponse(reposnseString);
            }
        });
    }
}
