package io.rapidpro.androidchannel.data;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.rapidpro.androidchannel.BuildConfig;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PacksDataSource {


    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client;

    public PacksDataSource() {
        client = new OkHttpClient();
    }

    public void getFiles(Callback callback) {
        client.newCall(filesRequest()).enqueue(callback);
    }

    private Request filesRequest() {
        final Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("path", BuildConfig.PACKS_PATH);
        RequestBody body = RequestBody.create(JSON, new JSONObject(bodyMap).toString());
        return new Request.Builder()
                .header("Authorization", String.format("Bearer %s", BuildConfig.AUTH_TOKEN))
                .url(BuildConfig.FILES_URL)
                .post(body)
                .build();
    }

    public void downloadFile(io.rapidpro.androidchannel.payload.File file, Callback callback) {
        client.newCall(downloadFileRequest(file.id)).enqueue(callback);
    }

    private Request downloadFileRequest(String fileId) {
        final Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("path", fileId);
        return new Request.Builder()
                .header("Authorization", String.format("Bearer %s", BuildConfig.AUTH_TOKEN))
                .header("Dropbox-API-Arg", new JSONObject(bodyMap).toString())
                .url(BuildConfig.DOWNLOAD_FILE_URL)
                .build();
    }
}
