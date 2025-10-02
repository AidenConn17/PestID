package com.example.pestid;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Identification {
    ThreadPerTaskExecutor executor;
    public Identification(ThreadPerTaskExecutor executor){this.executor = executor;}
    ArrayList<JSONObject> confidentSuggestions = new ArrayList<>();
    static CountDownLatch latch = new CountDownLatch(1);
        public ArrayList<JSONObject> getInfoAboutInsect(String imageBase64) throws IOException, JSONException {
            latch = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("application/json");
                String jsonBody = "{\"images\": [\"data:image/jpeg;base64," + imageBase64 + "\"]\n}";
                RequestBody body = RequestBody.create(jsonBody, mediaType);
                Request request = new Request.Builder()
                        .url("https://insect.kindwise.com/api/v1/identification?details=common_names,danger,danger_description,role")
                        .method("POST", body)
                        .addHeader("Api-Key", DataStorage.API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();
                String responseString = response.body().string();
                Log.v("Response", responseString);
                JSONObject json = new JSONObject(responseString);
                JSONObject result = new JSONObject(json.getString("result"));
                JSONObject classification = new JSONObject(result.getString("classification"));
                JSONArray suggestionsArray = new JSONArray(classification.getString("suggestions"));
                for (int i = 0; i < suggestionsArray.length(); i++) {
                    if (suggestionsArray.getJSONObject(i).getDouble("probability") > 0.2) {
                        confidentSuggestions.add(suggestionsArray.getJSONObject(i));
                    } else {
                        break;
                    }
                }
                response.close();
            } catch (JSONException | IOException ignored){
            }
            latch.countDown();
        });
        return confidentSuggestions;
    }
}