package net.kazav.gabi.fm103archive;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

/**
 * Global app data
 * Created by gabik on 12/20/16.
 */

class AppGlobal {
    static final int RC_SIGN_IN = 1923;
    static final String SHOW_CODE_EXTRA = "gabi.kazav.show_code";

    static final String LIVE_URL = "http://103fm.live.streamgates.net/103fm_live/1multix/icecast.audio";

    static class Show {
        String name;
        String code;
        Bitmap img;

        Show(String n, String c, Bitmap i) {
            name = n;
            code = c;
            img = i;
        }
    }

    static ArrayList<String> names = null;
    static ArrayList<String> urls = null;
    static ArrayList<String> dates = null;
    static ArrayList<Show> shows = null;
    static ArrayList<Boolean> clicks = null;

    static final String LoadShow = "LoadShow";

    static Bitmap cur_logo;

    static GoogleApiClient mGoogleApiClient;

    static GoogleApiClient.OnConnectionFailedListener GoogleFailed = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e("Google connection", "Failed");
        }
    };

    static FirebaseUser cur_user = null;
    static DatabaseReference myRef;

    static String sharedShow;
    static final String share_prefix = "http://103.gabi.ninja/share/";
}
