package net.kazav.gabi.archivealongal;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static net.kazav.gabi.archivealongal.AppGlobal.LoadShow;
import static net.kazav.gabi.archivealongal.AppGlobal.names;
import static net.kazav.gabi.archivealongal.AppGlobal.showimg;
import static net.kazav.gabi.archivealongal.AppGlobal.showscode;
import static net.kazav.gabi.archivealongal.AppGlobal.showsname;
import static net.kazav.gabi.archivealongal.AppGlobal.urls;

public class MainActivity extends AppCompatActivity {

    private final String loaderurl = "http://103.gabi.ninja/?show=";
    private final String showsurl = "http://103.gabi.ninja/shows";
    private final String showspicurl = "http://103.gabi.ninja/pics?pic=";
    private final String TAG = "Loader";
//    /**
//     * ATTENTION: This was auto-generated to implement the App Indexing API.
//     * See https://g.co/AppIndexing/AndroidStudio for more information.
//     */
//    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            if (extras.containsKey(LoadShow)) {
                String show_code = extras.getString(LoadShow);
                new LoadData().execute(show_code);
            } else new LoadShows().execute();
        }


//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void goto_list(String kind) {
        Intent intent;
        if (kind.equals("show")) intent = new Intent(this, ListActivity.class);
        else intent = new Intent(this, ShowsActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    public Action getIndexApiAction() {
//        Thing object = new Thing.Builder()
//                .setName("Main Page") // TODO: Define a title for the content shown.
//                // TODO: Make sure this auto-generated URL is correct.
//                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
//                .build();
//        return new Action.Builder(Action.TYPE_VIEW)
//                .setObject(object)
//                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
//                .build();
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//        AppIndex.AppIndexApi.start(client, getIndexApiAction());
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        AppIndex.AppIndexApi.end(client, getIndexApiAction());
//        client.disconnect();
//    }

    private class LoadData extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                URL url = new URL(loaderurl + params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("GET");
                connection.connect();

                int response = connection.getResponseCode();
                if (response >= 200 && response <= 399) {
                    urls = new ArrayList<>();
                    names = new ArrayList<>();
                    BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        String[] res = line.split("xXx");
                        names.add(res[0]);
                        urls.add(res[1]);
                    }
                } else {
                    Log.e(TAG, "Got response: " + Integer.toString(response));
                }

            } catch (MalformedURLException e) {
                Log.e(TAG, "Wrong URL");
            } catch (IOException e) {
                Log.e(TAG, "Cannot open connection");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if ((names != null) && (urls != null))
                for (int i=0 ; i<names.size(); i++)
                    Log.i(names.get(i), urls.get(i));
            goto_list("show");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class LoadShows extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL(showsurl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("GET");
                connection.connect();

                int response = connection.getResponseCode();
                if (response >= 200 && response <= 399) {
                    showscode = new ArrayList<>();
                    showsname = new ArrayList<>();
                    showimg = new ArrayList<>();
                    BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        String[] res = line.split("\\|");
                        showscode.add(res[0]);
                        showsname.add(res[1]);
                        URL picurl = new URL(showspicurl + res[0] + ".jpg");
                        HttpURLConnection piccon = (HttpURLConnection) picurl.openConnection();
                        piccon.getDoOutput();
                        piccon.setRequestMethod("GET");
                        piccon.connect();
                        if (piccon.getResponseCode() == 200)
                            showimg.add(BitmapFactory.decodeStream(piccon.getInputStream()));
                        piccon.disconnect();
                    }
                } else {
                    Log.e(TAG, "Got response: " + Integer.toString(response));
                }
                connection.disconnect();

            } catch (MalformedURLException e) {
                Log.e(TAG, "Wrong URL");
            } catch (IOException e) {
                Log.e(TAG, "Cannot open connection");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            goto_list("all_shows");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
