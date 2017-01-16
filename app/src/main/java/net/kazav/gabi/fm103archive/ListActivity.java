package net.kazav.gabi.fm103archive;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import static net.kazav.gabi.fm103archive.AppGlobal.SHOW_CODE_EXTRA;
import static net.kazav.gabi.fm103archive.AppGlobal.clicks;
import static net.kazav.gabi.fm103archive.AppGlobal.dates;
import static net.kazav.gabi.fm103archive.AppGlobal.myRef;
import static net.kazav.gabi.fm103archive.AppGlobal.names;
import static net.kazav.gabi.fm103archive.AppGlobal.share_prefix;
import static net.kazav.gabi.fm103archive.AppGlobal.sharedShow;
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
    private RecyclerView lv;
    private String show_code, cur_play_url;
    private ArrayList<CallsHolder> filtered_calls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);

        Log.d(TAG, callsurl);
        Log.d(TAG, call_direct);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        show_code = extras.getString(SHOW_CODE_EXTRA);

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

        stop = (TextView) findViewById(R.id.stoptime);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop_resume_listen(false);
            }
        });

        if (sharedShow != null) playShared(sharedShow.split("/")[3]);

        initSwipe();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list_menu, menu);
        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean hide_listens = settings.getBoolean("checkbox", false);
        MenuItem item = menu.findItem(R.id.hide_listened);
        item.setChecked(hide_listens);
        filtered_calls = new ArrayList<>();
        init_adapter();
        filter_list( hide_listens);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.hide_listened) {
            item.setChecked(!item.isChecked());
            SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("checkbox", item.isChecked());
            editor.apply();
            filter_list(item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //    @Override
    protected void onResume() {
        super.onResume();
        if (lv != null) lv.getAdapter().notifyDataSetChanged();
    }

    private String get_code_from_position(int position) {
        return filtered_calls.get(position).url.split("\\?")[1].split("\\|")[0].split("=")[1];
    }

    private void playShared(String call_code) {
        int ix = -1;
        for (int i = 0 ; i < filtered_calls.size() ; i++)
            if (get_code_from_position(i).equals(call_code))
                ix = i;
        if (ix > 0) {
            play_next(ix);
            lv.getLayoutManager().scrollToPosition(ix);
        }
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
        if (i < filtered_calls.size()) {
            cur_play_url = filtered_calls.get(i).url;
            Log.i("URL", cur_play_url);
            Log.i("Name", filtered_calls.get(i).name);

            final int firstListItemPosition = ((LinearLayoutManager) lv.getLayoutManager()).findFirstVisibleItemPosition();
            final int lastListItemPosition = ((LinearLayoutManager) lv.getLayoutManager()).findLastVisibleItemPosition();
            if (i >= firstListItemPosition && i <= lastListItemPosition ) {
                final int childIndex = i - firstListItemPosition;
                View view = lv.getChildAt(childIndex);
                set_click(view, true, "");
            }
            int real_pos = filtered_calls.get(i).real_pos;
            clicks.set(real_pos, true);
            filtered_calls.get(i).click = true;
            myRef.child(filtered_calls.get(i).url.split("=")[1].split("\\|")[0]).setValue(true);
            new GetCall().execute(filtered_calls.get(i).url);
            lv.getAdapter().notifyDataSetChanged();
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

    private class CallsAdapter extends RecyclerView.Adapter<CallsAdapter.ViewHolder>{
        private final String AdapterTag = "Recycler Adapter";

        class ViewHolder extends RecyclerView.ViewHolder{
            private TextView call;
            private TextView date;

            ViewHolder(View itemView) {
                super(itemView);
                call = (TextView) itemView.findViewById(R.id.callname);
                date = (TextView) itemView.findViewById(R.id.calldate);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View newView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_with_date, parent, false);
            Log.v(AdapterTag, "CreateView");
            return new ViewHolder(newView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final int real_pos = filtered_calls.get(position).real_pos;
            final int pos = position;
            holder.call.setText(filtered_calls.get(position).name);
            holder.date.setText(filtered_calls.get(position).date);
            set_click(holder.itemView, filtered_calls.get(position).click, filtered_calls.get(position).url);
            Log.v(AdapterTag, "Added " + Integer.toString(position) + " : " + filtered_calls.get(position).name);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("Clicked", Integer.toString(real_pos));
                    play_next(pos);
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    myRef.child(filtered_calls.get(pos).url.split("=")[1].split("\\|")[0]).removeValue();
                    clicks.set(real_pos, false);
                    filtered_calls.get(pos).click = false;
                    set_click(v, false, "");
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return filtered_calls.size();
        }

    }

    private void set_click(View v, boolean b, String cur_url) {
        if (b) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (cur_url.equals(cur_play_url)) v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright, getTheme()));
                else v.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light, getTheme()));
            else
                if (cur_url.equals(cur_play_url)) //noinspection deprecation
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_bright));
                else //noinspection deprecation
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) v.setBackgroundColor(getResources().getColor(android.R.color.white, getTheme()));
            else //noinspection deprecation
                v.setBackgroundColor(getResources().getColor(android.R.color.white));
        }
    }

    private void shareCall(int position) {
        Log.i("SHARE", Integer.toString(position));
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, share_prefix + show_code + "/" + get_code_from_position(position));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share call"));
    }

    private void initSwipe(){
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    shareCall(position);
                }

                @Override
                public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                        RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                        int actionState, boolean isCurrentlyActive) {

                    Paint p = new Paint();
                    Bitmap icon;
                    if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){

                        View itemView = viewHolder.itemView;
                        float height = (float) itemView.getBottom() - (float) itemView.getTop();
//                        float width = height;
//                        Log.d("Swipe H/W", String.valueOf(height) + " / " + String.valueOf(width));

                        p.setStyle(Paint.Style.FILL);
                        if (Build.VERSION.SDK_INT >= 23)
                            p.setColor(getResources().getColor(R.color.shareChildBG, getTheme()));
                        else
                            //noinspection deprecation
                            p.setColor(getResources().getColor(R.color.shareChildBG));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                        c.drawRect(background,p);
                        p.setStyle(Paint.Style.STROKE);
                        if (Build.VERSION.SDK_INT >= 23)
                            p.setColor(getResources().getColor(R.color.shareChildStroke, getTheme()));
                        else
                            //noinspection deprecation
                            p.setColor(getResources().getColor(R.color.shareChildStroke));
                        p.setStrokeWidth(15.0f);
                        RectF stroke = new RectF((float) itemView.getLeft() + 2,
                                                     (float) itemView.getTop() + 2,
                                                     (float) itemView.getWidth()/3-2,
                                                     (float) itemView.getBottom() - 2);
                        c.drawRect(stroke, p);

                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.share);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + 5,
                                                    (float) itemView.getTop() + 5,
                                                    (float) itemView.getLeft() + height - 5,
                                                    (float)itemView.getBottom() - 5);
                        c.drawBitmap(icon,null,icon_dest,p);
                    }
//                    Log.i("dX", Float.toString(dX) + " / " + Integer.toString(viewHolder.itemView.getWidth()));
                    super.onChildDraw(c, recyclerView, viewHolder, Math.min(dX, viewHolder.itemView.getWidth() / 3), dY, actionState, isCurrentlyActive);
                }
            };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView((RecyclerView) findViewById(R.id.calls_list));
    }

    private class CallsHolder {
        String name, url, date;
        boolean click;
        int real_pos;

        CallsHolder(String name, String url, String date, boolean click, int real_pos){
            this.name = name;
            this.url = url;
            this.date = date;
            this.click = click;
            this.real_pos = real_pos;
        }
    }

    private void filter_list(boolean hide_listens) {
        filtered_calls = new ArrayList<>();
        for (int i=0; i<names.size(); i++) {
            if (!(clicks.get(i) && hide_listens)) {
                filtered_calls.add(new CallsHolder(names.get(i), urls.get(i), dates.get(i), clicks.get(i), i));
            }
        }
        lv.getAdapter().notifyDataSetChanged();
    }

    private void init_adapter() {
        CallsAdapter adpt = new CallsAdapter();
        lv = (RecyclerView) findViewById(R.id.calls_list);
        lv.setLayoutManager(new LinearLayoutManager(this));
        lv.setItemAnimator(new DefaultItemAnimator());
        lv.setAdapter(adpt);
    }
}
