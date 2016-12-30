package net.kazav.gabi.fm103archive;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static net.kazav.gabi.fm103archive.AppGlobal.clicks;
import static net.kazav.gabi.fm103archive.AppGlobal.dates;
import static net.kazav.gabi.fm103archive.AppGlobal.myRef;
import static net.kazav.gabi.fm103archive.AppGlobal.names;
import static net.kazav.gabi.fm103archive.AppGlobal.urls;

public class ListActivity extends AppCompatActivity implements Runnable {

    private final String callsurl = "http://103.gabi.ninja/get";
    private final String call_direct = "http://103fm.aod.streamgates.net/103fm_aod/";
    private final String TAG = "ListView";
    private MediaPlayer mp;
    private int cur_pos, cur_play;
    private SeekBar sb;
    private TextView endtime, starttime;
    private TextView stop;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Log.d(TAG, callsurl);
        Log.d(TAG, call_direct);

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
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Log.i(TAG, "Loading saved");

        CallsAdapter adpt = new CallsAdapter(this, names);
        lv = (ListView) findViewById(R.id.calls_list);
        lv.setAdapter(adpt);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("Clicked", Integer.toString(i));
                play_next(i);
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                myRef.child(urls.get(i).split("=")[1].split("\\|")[0]).removeValue();
                clicks.set(i, false);
                set_click(view, false);
                return true;
            }
        });

        stop = (TextView) findViewById(R.id.stoptime);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop_resume_listen(false);
            }
        });
    }

    @Override
    public void run() {
        cur_pos = mp.getCurrentPosition();
        int total = mp.getDuration();
        sb.setMax(total);

        while ((cur_pos < total) && (mp.isPlaying())) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.w(TAG, "sleep interrupted");
            }
            cur_pos = mp.getCurrentPosition();
            final int tmppos = cur_pos;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    starttime.setText(get_human_time(tmppos));
                }
            });
            sb.setProgress(cur_pos);
        }
        Log.d("POS/TOTAL", Integer.toString(cur_pos) + "/" + Integer.toString(total));
        if ((cur_pos+500) >= total) {
            Log.i(TAG, "EOF");
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  play_next(cur_play + 1);
                              }
                          });
        }
    }

    private void play_next(int i) {
        cur_play = i;
        if (i < urls.size()) {
            Log.i("URL", urls.get(i));
            Log.i("Name", names.get(i));
            final int firstListItemPosition = lv.getFirstVisiblePosition();
            final int lastListItemPosition = firstListItemPosition + lv.getChildCount() - 1;
            if (i > firstListItemPosition && i < lastListItemPosition ) {
                final int childIndex = i - firstListItemPosition;
                View view = lv.getChildAt(childIndex);
                set_click(view, true);
            }
            clicks.set(i, true);
            myRef.child(urls.get(i).split("=")[1].split("\\|")[0]).setValue(true);
            new GetCall().execute(urls.get(i));
        } else {Log.w(TAG, "End of list"); }
    }

    private String get_human_time(int millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    private void stop_resume_listen(boolean force_stop) {
        Log.i(TAG, "STOP RESUME");
        if (force_stop) {
            if (mp.isPlaying()) mp.stop();
            cur_pos = 0;
        }
        else if (mp != null)
            if (!mp.isPlaying()) {
                Log.i(TAG, "RESUME");
                try {
                    mp.prepare();
                    mp.seekTo(cur_pos);
                    mp.start();
                    stop.setText("STOP");
                    endtime.setText(get_human_time(mp.getDuration()));
                    new Thread(this).start();
                } catch (IOException e) {
                    Log.e(TAG, "Cannot play");
                }
            } else {
                Log.i(TAG, "STOP");
                mp.stop();
                stop.setText("PLAY");
            }
    }

    private void start_listen(String mp3) {
        Log.i(TAG, mp3);
        String full_url = call_direct + mp3 + ".mp3";
        Log.i("FullURL", full_url);
        stop_resume_listen(true);
        if (mp != null) {
            try {
                mp.reset();
                mp.setDataSource(full_url);
            } catch (IOException e) {
                Log.e(TAG, "Cannot play");
            }
            stop_resume_listen(false);
        }
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

    private class CallsAdapter extends ArrayAdapter<String>{

        private final Activity context;

        CallsAdapter(Activity context, ArrayList<String> callslist) {
            super(context, 0, callslist);
            this.context = context;
        }

        private class ViewHolder {
            private TextView call;
            private TextView date;
        }

        @NonNull
        @Override
        public View getView(int position, View view, @NonNull ViewGroup parent) {
            ViewHolder mViewHolder;

            if (view == null) {
                Log.i(TAG, "view is null");
                mViewHolder = new ViewHolder();
                LayoutInflater inflater = context.getLayoutInflater();
                view = inflater.inflate(R.layout.list_row_with_date, parent, false);
                mViewHolder.call = (TextView) view.findViewById(R.id.callname);
                mViewHolder.date = (TextView) view.findViewById(R.id.calldate);
                view.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) view.getTag();
            }

            mViewHolder.call.setText(names.get(position));
            mViewHolder.date.setText(dates.get(position));
            set_click(view, clicks.get(position));
            Log.v(TAG, "Added " + Integer.toString(position) + " : " + names.get(position));
            return view;
        }
    }

    private void set_click(View v, boolean b) {
        if (b) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                v.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light, getTheme()));
            }else {
                //noinspection deprecation
                v.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                v.setBackgroundColor(getResources().getColor(android.R.color.white, getTheme()));
            }else {
                //noinspection deprecation
                v.setBackgroundColor(getResources().getColor(android.R.color.white));
            }
        }
    }
}
