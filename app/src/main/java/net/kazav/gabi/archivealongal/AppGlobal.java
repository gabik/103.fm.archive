package net.kazav.gabi.archivealongal;

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
    static ArrayList<Show> shows = null;
    static ArrayList<Boolean> clicks = null;

    static String LoadShow = "LoadShow";
}
