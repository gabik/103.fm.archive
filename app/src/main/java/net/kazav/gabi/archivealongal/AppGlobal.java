package net.kazav.gabi.archivealongal;

import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.util.ArrayList;

/**
 * Global app data
 * Created by gabik on 12/20/16.
 */

class AppGlobal {

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

    static String LoadShow = "LoadShow";

    static String cur_code;
    static Bitmap cur_logo;

    static SharedPreferences save_done;
}
