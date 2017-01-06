package net.kazav.gabi.fm103archive;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static net.kazav.gabi.fm103archive.AppGlobal.GoogleFailed;
import static net.kazav.gabi.fm103archive.AppGlobal.LoadShow;
import static net.kazav.gabi.fm103archive.AppGlobal.RC_SIGN_IN;
import static net.kazav.gabi.fm103archive.AppGlobal.clicks;
import static net.kazav.gabi.fm103archive.AppGlobal.cur_code;
import static net.kazav.gabi.fm103archive.AppGlobal.cur_logo;
import static net.kazav.gabi.fm103archive.AppGlobal.cur_user;
import static net.kazav.gabi.fm103archive.AppGlobal.dates;
import static net.kazav.gabi.fm103archive.AppGlobal.mGoogleApiClient;
import static net.kazav.gabi.fm103archive.AppGlobal.myRef;
import static net.kazav.gabi.fm103archive.AppGlobal.names;
import static net.kazav.gabi.fm103archive.AppGlobal.sharedShow;
import static net.kazav.gabi.fm103archive.AppGlobal.shows;
import static net.kazav.gabi.fm103archive.AppGlobal.urls;

public class MainActivity extends AppCompatActivity {

    private final String loaderurl = "http://103.gabi.ninja/calls?show=";
    private final String showsurl = "http://103.gabi.ninja/shows";
    private final String showspicurl = "http://103.gabi.ninja/pics?pic=";
    private final String TAG = "Loader";
    private FirebaseAuth mAuth;
    private ArrayList<String> saved_on_db;


    private SignInButton gsi;
//    /**
//     * ATTENTION: This was auto-generated to implement the App Indexing API.
//     * See https://g.co/AppIndexing/AndroidStudio for more information.
//     */
//    private GoogleApiClient client;

    private void load_show(final String show_code) {
        assert cur_user.getEmail() != null;
        Log.i("User login", cur_user.getEmail());
        myRef = FirebaseDatabase.getInstance().getReference("users/" + cur_user.getEmail().replaceAll("\\.", ",") + "/" + show_code);
        saved_on_db = new ArrayList<>();
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot c : dataSnapshot.getChildren())
                    saved_on_db.add(c.getKey());
                new LoadData().execute(show_code);
            }
            @Override public void onCancelled(DatabaseError databaseError) {}});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        gsi = ((SignInButton) findViewById(R.id.sign_in_button));

        sharedShow = null;

        // Checking if we have show already chosen
        if ((extras != null) && (extras.containsKey(LoadShow))) {
            Log.i(TAG, "Choosed show");
            loading(true);
            ImageView img = (ImageView) findViewById(R.id.mainlogo);
            Log.i(TAG, "have extras and show key");
            img.setImageBitmap(cur_logo);
            final String show_code = extras.getString(LoadShow);
            load_show(show_code);
        } else { // If no show choosed - we are booting.
            Log.i(TAG, "onCreate");
            Log.d(TAG, loaderurl);
            Log.d(TAG, showsurl);
            Log.d(TAG, showspicurl);

            mAuth = FirebaseAuth.getInstance();
            cur_user = mAuth.getCurrentUser();

            // Requesting Google sign in (ID + email)
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, GoogleFailed)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
            gsi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    signIn();
                }
            });

            if (cur_user != null) signIn();
            else gsi.setVisibility(View.VISIBLE);
        }
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    void loading(boolean to_load) {
        if (to_load) {
            findViewById(R.id.loader).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.load_label)).setText(R.string.loading);
            gsi.setVisibility(View.INVISIBLE);
        } else {
            findViewById(R.id.loader).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.load_label)).setText(R.string.signing_in);
            gsi.setVisibility(View.VISIBLE);
        }
    }

    public void signIn() {
        loading(true);
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void goto_list(String kind) {
        Log.i(TAG, "goto_list " + kind);
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
                Log.i(TAG, "LoadData (calls)");
                URL url = new URL(loaderurl + params[0]);
                Log.i(TAG, "code=" + params[0]);
                Log.i(TAG, "url=" + loaderurl + params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("GET");
                connection.connect();

                int response = connection.getResponseCode();
                if (response >= 200 && response <= 399) {
                    Log.i(TAG, "got 200");
                    urls = new ArrayList<>();
                    names = new ArrayList<>();
                    dates = new ArrayList<>();
                    clicks = new ArrayList<>();
                    BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        Log.i(TAG, "Added: " + line);
                        String[] res = line.split("\\|\\|");
                        names.add(res[0]);
                        urls.add(res[1]);
                        dates.add(res[2]);
                        String cur_call_fix_code = res[1].split("=")[1].split("\\|")[0];
                        if (saved_on_db.contains(cur_call_fix_code)) clicks.add(true);
                        else clicks.add(false);
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
                Log.i(TAG, "LoadShows");
                URL url = new URL(showsurl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("GET");
                connection.connect();

                int response = connection.getResponseCode();
                if (response >= 200 && response <= 399) {
                    Log.i(TAG, "got 200");
                    shows = new ArrayList<>();
                    BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        String[] res = line.split("\\|");
                        Bitmap img = null;
                        URL picurl = new URL(showspicurl + res[0] + ".jpg");
                        HttpURLConnection piccon = (HttpURLConnection) picurl.openConnection();
                        piccon.getDoOutput();
                        piccon.setRequestMethod("GET");
                        piccon.connect();
                        if (piccon.getResponseCode() == 200)
                            img = BitmapFactory.decodeStream(piccon.getInputStream());
                        AppGlobal.Show cur_show = new AppGlobal.Show(res[1], res[0], img);
                        shows.add(cur_show);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            firebaseAuthWithGoogle(acct);
        } else {
            gsi.setVisibility(View.VISIBLE);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            loading(false);
                        }
                        else {
                            cur_user = FirebaseAuth.getInstance().getCurrentUser();
                            Intent intent = getIntent();
                            Uri data = intent.getData();
                            if (data != null) {
                                Log.i("Load shared", data.getPath());
                                sharedShow = data.getPath();
                                load_show(sharedShow.split("/")[1]);
                            } else new LoadShows().execute();
                        }
                    }
                });
    }

}
