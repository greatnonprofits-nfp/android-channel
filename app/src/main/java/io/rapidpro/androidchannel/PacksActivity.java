package io.rapidpro.androidchannel;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.rapidpro.androidchannel.data.PacksDataSource;
import io.rapidpro.androidchannel.payload.Entries;
import io.rapidpro.androidchannel.payload.File;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;


public class PacksActivity extends BaseActivity {

    private ProgressBar progressBar;
    private ListView packs;
    private ArrayAdapter<File> adapter;
    private PacksDataSource dataSource;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.packs);

        progressBar = (ProgressBar) findViewById(R.id.progress);
        packs = (ListView) findViewById(R.id.packs);
        packs.setOnItemClickListener(onFileItemClickListener);

        adapter = new ArrayAdapter<File>(this,
                android.R.layout.simple_list_item_1);

        packs.setAdapter(adapter);

        dataSource = new PacksDataSource();
        dataSource.getFiles(getFilesCallback);
    }

    private AdapterView.OnItemClickListener onFileItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            changeProgressVisibility(View.VISIBLE);
            final File file = (File) parent.getItemAtPosition(position);
            dataSource.downloadFile(file, downloadFileCallback(file));
        }
    };

    private void changeProgressVisibility(final int visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(visibility);
            }
        });
    }

    private Callback getFilesCallback = new Callback() {

        @Override
        public void onFailure(Call call, IOException e) {
            changeProgressVisibility(View.GONE);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showListFileError();
                }
            });
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            final ResponseBody body = response.body();
            final Type type = new TypeToken<Entries<File>>() {}.getType();

            changeProgressVisibility(View.GONE);
            processBody(body, type);
        }
    };

    private Callback downloadFileCallback(final File file) {
        return new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        changeProgressVisibility(View.GONE);
                        showDownloadError();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                changeProgressVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String destination = prepareFileDestination(file);
                    Uri uri = Uri.parse("file://" + destination);
                    java.io.File downloadedFile =
                            new java.io.File(destination);
                    BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
                    sink.writeAll(response.body().source());
                    sink.close();
                    installPack(uri);
                } else {
                    showDownloadError();
                }
            }
        };
    }

    private String prepareFileDestination(File file) {
        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        String fileName = file.name;
        destination += fileName;
        return destination;
    }

    private void showDownloadError() {
        Toast.makeText(this, "Download failed!", Toast.LENGTH_SHORT).show();
    }

    private void showListFileError() {
        Toast.makeText(this, "We had a problem to get all packs, try it again later!", Toast.LENGTH_SHORT).show();
    }

    private void processBody(ResponseBody body, Type type) throws IOException {
        if (body != null) {
            final Entries<File> files = new Gson().fromJson(body.string(), type);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    populatePacks(files.entries);
                }
            });
        }
    }

    private void populatePacks(List<File> items) {
        Collections.sort(items, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.name.compareTo(o2.name);
            }
        });
        adapter.addAll(items);
    }

    private void installPack(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
