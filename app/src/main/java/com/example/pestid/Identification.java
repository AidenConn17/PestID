package com.example.pestid;

import android.provider.ContactsContract;
import android.service.persistentdata.PersistentDataBlockManager;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.Executor;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Identification {
    Executor executor;


    public void getInfoAboutInsect(String imageBase64) throws IOException {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = "{\n \"images\": [\ndata:" + imageBase64 + "\n ]  \n}";
            Log.v("Identifier", jsonBody);
            RequestBody body = RequestBody.create(mediaType, jsonBody);
        Log.d("Identifier", "Ran identification on image at " + imageBase64);
            Request request = new Request.Builder()
                    .url("https://insect.kindwise.com/api/v1/identification?details=common_names,description,image,synonyms,danger,role")
                    .method("POST", body)
                    .addHeader("Api-Key", DataStorage.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();

//            mediaType = MediaType.parse("text/plain");
//            body = RequestBody.create(mediaType, "");
//            request = new Request.Builder()
//                    .url("https://insect.kindwise.com/api/v1/identification/dI3dtejN93l6uH1?details=common_names,description,image,synonyms,danger,role&language=en")
//                    .method("GET", body)
//                    .addHeader("Api-Key", DataStorage.API_KEY)
//                    .build();
//            response = client.newCall(request).execute();
    }
}
