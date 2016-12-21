package net.kazav.gabi.archivealongal;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static net.kazav.gabi.archivealongal.AppGlobal.names;
import static net.kazav.gabi.archivealongal.AppGlobal.urls;

public class ListActivity extends AppCompatActivity implements Runnable{

    private final String callsurl = "http://103.gabi.ninja/get";
    private final String call_direct = "http://103fm.aod.streamgates.net/103fm_aod/";
    private final String TAG = "ListView";
    private MediaPlayer mp;
    private SeekBar sb;
    private TextView endtime, starttime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        sb = (SeekBar) findViewById(R.id.seek);

        endtime = (TextView) findViewById(R.id.endtime);
        starttime = (TextView) findViewById(R.id.starttime);

        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                try {
                    if (mp.isPlaying()) {
                        if (b) {
                            mp.seekTo(i);
                            starttime.setText(get_human_time(i));
                        }
                    }
                } catch (Exception e) {
                    Log.e("seek bar", "" + e);
                    seekBar.setEnabled(false);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ArrayAdapter<String> list_adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        ListView lv = (ListView) findViewById(R.id.calls_list);
        lv.setAdapter(list_adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("Clicked", Integer.toString(i));
                Log.i("URL", urls.get(i));
                new GetCall().execute(urls.get(i));
            }
        });

        TextView stop = (TextView) findViewById(R.id.stoptime);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop_listen();
            }
        });
    }

    @Override
    public void run() {
        int currentPosition = mp.getCurrentPosition();
        int total = mp.getDuration();
        sb.setMax(total);

        while ((currentPosition < total) && (mp.isPlaying())) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) { Log.w(TAG, "sleep interrupted"); }
            currentPosition = mp.getCurrentPosition();
            final int tmppos = currentPosition;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    starttime.setText(get_human_time(tmppos));
                }
            });
            sb.setProgress(currentPosition);
        }
    }


    private String get_human_time(int millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    private void stop_listen() {
        if(mp != null && mp.isPlaying()) {
            mp.stop();
            mp.reset();
        }
    }

    private void start_listen(String mp3) {
        Log.i(TAG, mp3);
        String full_url = call_direct + mp3 + ".mp3";
        Log.i("FullURL", full_url);
        stop_listen();
        try {
            mp.setDataSource(full_url);
            mp.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Cannot play");
        }
        mp.start();
        endtime.setText(get_human_time(mp.getDuration()));
        new Thread(this).start();
    }

        private class GetCall extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String ret_val = "";
            try {
                URL url = new URL(callsurl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("POST");
                connection.connect();

                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write("calls=" + params[0]);
                writer.flush();
                writer.close();
                os.close();

                int response = connection.getResponseCode();
                if (response >= 200 && response <= 399) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    ret_val = r.readLine();
                } else {
                    Log.e(TAG, "Got response: " + Integer.toString(response));
                }

            } catch (MalformedURLException e) {
                Log.e(TAG, "Wrong URL");
            } catch (IOException e) {
                Log.e(TAG, "Cannot open connection");
            }
            return ret_val;
        }

        @Override
        protected void onPostExecute(String result) {
            start_listen(result);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
