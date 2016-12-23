package net.kazav.gabi.archivealongal;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

import static net.kazav.gabi.archivealongal.AppGlobal.LIVE_URL;
import static net.kazav.gabi.archivealongal.AppGlobal.LoadShow;
import static net.kazav.gabi.archivealongal.AppGlobal.cur_code;
import static net.kazav.gabi.archivealongal.AppGlobal.cur_logo;
import static net.kazav.gabi.archivealongal.AppGlobal.shows;
import static net.kazav.gabi.archivealongal.AppGlobal.Show;

public class ShowsActivity extends AppCompatActivity {

    private final String TAG = "ShowsView";
    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shows);

        Log.i(TAG, "onCreate");
        Log.i(TAG, Integer.toString(shows.size()));

        CustomAdapter adpt = new CustomAdapter(this, shows);
        ListView lv = (ListView) findViewById(R.id.shows_list);
        lv.setAdapter(adpt);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("Clicked", Integer.toString(i));
                Log.i("URL", shows.get(i).name);
                goto_list(i);
            }
        });
    }

    private void stop_live() {
        if (mp != null && mp.isPlaying()) mp.stop();
        if (mp != null) mp.reset();
        mp = null;
    }

    private void goto_list(int i) {
        Log.i(TAG, "Choosed: " + shows.get(i).name);
        stop_live();
        cur_logo = shows.get(i).img;
        cur_code = shows.get(i).code;
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(LoadShow, shows.get(i).code);
        startActivity(intent);
        finish();
    }

    public void listen_live(View view) {
        stop_live();
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mp.setDataSource(LIVE_URL);
            mp.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Cannot play");
        }
        mp.start();

    }

    private class CustomAdapter extends ArrayAdapter<Show>{

        private final Activity context;

        CustomAdapter(Activity context, ArrayList<Show> showlist) {
            super(context, 0, showlist);
            this.context = context;
        }

        private class ViewHolder {
            private TextView name;
            private ImageView img;
        }

        @NonNull
        @Override
        public View getView(int position, View view, @NonNull ViewGroup parent) {
            ViewHolder mViewHolder;

            if (view == null) {
                Log.i(TAG, "view is null");
                mViewHolder = new ViewHolder();
                LayoutInflater inflater = context.getLayoutInflater();
                view = inflater.inflate(R.layout.list_row, parent, false);
                mViewHolder.name = (TextView) view.findViewById(R.id.Itemname);
                mViewHolder.img = (ImageView) view.findViewById(R.id.Itemicon);
                view.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) view.getTag();
            }

            mViewHolder.name.setText(shows.get(position).name);
            mViewHolder.img.setImageBitmap(shows.get(position).img);
            Log.i(TAG, "Added " + shows.get(position).name);
            return view;
        }
    }
}
