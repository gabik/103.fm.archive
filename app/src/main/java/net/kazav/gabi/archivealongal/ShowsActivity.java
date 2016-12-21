package net.kazav.gabi.archivealongal;

import android.app.Activity;
import android.content.Intent;
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

import static net.kazav.gabi.archivealongal.AppGlobal.LoadShow;
import static net.kazav.gabi.archivealongal.AppGlobal.showimg;
import static net.kazav.gabi.archivealongal.AppGlobal.showscode;
import static net.kazav.gabi.archivealongal.AppGlobal.showsname;

public class ShowsActivity extends AppCompatActivity {

    private final String TAG = "ShowsView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shows);

        CustomAdapter adpt = new CustomAdapter(this);
        ListView lv = (ListView) findViewById(R.id.shows_list);
        lv.setAdapter(adpt);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("Clicked", Integer.toString(i));
                Log.i("URL", showsname.get(i));
                goto_list(i);
            }
        });
    }

    private void goto_list(int i) {
        Log.i(TAG, "Choosed: " + showscode.get(i));
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(LoadShow, showscode.get(i));
        startActivity(intent);
        finish();
    }

    private class CustomAdapter extends ArrayAdapter<String>{

        private final Activity context;

        CustomAdapter(Activity context) {
            super(context, R.layout.list_row);
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
                mViewHolder = new ViewHolder();
                LayoutInflater inflater = context.getLayoutInflater();
                view = inflater.inflate(R.layout.list_row, parent, true);
                mViewHolder.name = (TextView) view.findViewById(R.id.Itemname);
                mViewHolder.img = (ImageView) view.findViewById(R.id.Itemicon);
                view.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) view.getTag();
            }

            mViewHolder.name.setText(showsname.get(position));
            mViewHolder.img.setImageBitmap(showimg.get(position));
            Log.i(TAG, "Added " + showsname.get(position));
            return view;
        }
    }
}
