package com.example.pestid;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Identification {
        public void getInfoAboutInsect(String imageBase64) throws IOException, JSONException {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = "{\"images\": [\"data:image/jpeg;base64," + imageBase64 + "\"]\n}";
//            Log.v("json", jsonBody);
            RequestBody body = RequestBody.create(jsonBody, mediaType);
            Request request = new Request.Builder()
                    .url("https://insect.kindwise.com/api/v1/identification?details=common_names,url,description,image,danger,danger_desctiption,role")
                    .method("POST", body)
                    .addHeader("Api-Key", DataStorage.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();
            String responseString = response.body().string();
            Log.v("Response", responseString);
                JSONObject json = new JSONObject(responseString);
//                JSONArray jsonArray = json.getJSONArray("details");
                Log.v("Response", json.toString());
            response.close();
    }
}