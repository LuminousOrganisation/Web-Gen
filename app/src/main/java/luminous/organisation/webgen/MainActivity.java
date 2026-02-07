package luminous.organisation.webgen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity {

    private Timer _timer = new Timer();

    private boolean h = false;
    private String a;
    private TextView textview2;
    private TextView textview3;
    private ImageView imageview1;
    private TimerTask mllllll;
    private TextView textview1;
    private TextView textview4;

    private SharedPreferences json, v2;
    private Intent i = new Intent();
    private SharedPreferences ver;
    private SharedPreferences chat, data;
    private TimerTask delayer;
    private LinearLayout halloween;
    private AlertDialog.Builder dlg;
    private AlertDialog.Builder bidayprithibi;
    private ProgressBar progressbar1;
    private TextView logtv;



    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);

        setContentView(R.layout.activity_main);

        progressbar1 = findViewById(R.id.progressbar1);
        logtv = findViewById(R.id.logtv);

        // Get a reference to your root layout
        View rootLayout = findViewById(R.id.your_root_layout_id);

        // Apply a listener to handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            // Get the insets for the system bars (status bar and navigation bar)
            Insets systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Apply padding to the root layout to avoid overlapping
            v.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, systemBarInsets.bottom);
            // Return the insets to allow them to be passed down to other views
            return insets;
        });

        initialize(_savedInstanceState);

        FirebaseApp.initializeApp(this);

        /**
         if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
         || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
         ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
         } else {
         initializeLogic();
         }
         **/

        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {});
                })
                .start();

        startProgressFlow();

    }

    /**
     @Override
     public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
     super.onRequestPermissionsResult(requestCode, permissions, grantResults);
     if (requestCode == 1000) {
     initializeLogic();
     }
     }
     **/

    private void startProgressFlow() {
        // Phase 1: Start
        updateStatus(10, "Initialising ...");

        logtv.postDelayed(() -> {
            // Phase 2: After 100ms
            updateStatus(40, "Connecting to Luminous AI ...");

            logtv.postDelayed(() -> {
                // Phase 3: After 500ms
                updateStatus(70, "Preparing Web Gen Callback ...");

                // Your API call starts here
                // String finalPrompt = generateAIPromptString();
                // getCodeResponse(finalPrompt);
                initializeLogic();

            }, 2000);
        }, 2000);
    }
    // Helper method to keep code clean
    private void updateStatus(int percentage, String message) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            progressbar1.setProgress(percentage, true); // Smooth animation
        } else {
            progressbar1.setProgress(percentage);
        }
        logtv.setText(message);
    }

    public void onGenerationComplete() {
        runOnUiThread(() -> {
            updateStatus(100, "Launching Web Gen ...");

            // Optional: Hide progress after a delay
            logtv.postDelayed(() -> {
                progressbar1.setVisibility(View.GONE);
                logtv.setVisibility(View.GONE);
            }, 2000);
        });
    }

    private void initialize(Bundle _savedInstanceState) {

        textview2 = findViewById(R.id.textview2);

        //textview1 = findViewById(R.id.textview1);
        textview4 = findViewById(R.id.textview4);
        json = getSharedPreferences("json", Activity.MODE_PRIVATE);
        v2 = getSharedPreferences("v2", Activity.MODE_PRIVATE);
        ver = getSharedPreferences("ver", Activity.MODE_PRIVATE);
        chat = getSharedPreferences("chat", Activity.MODE_PRIVATE);
        data = getSharedPreferences("data", Activity.MODE_PRIVATE);
        dlg = new AlertDialog.Builder(this);
        bidayprithibi = new AlertDialog.Builder(this);

    }

    private void initializeLogic() {
        try{
            Runtime.getRuntime().exec("su");
            finishAffinity();
        }
        catch (Exception e) {

        }
        textview4.setOnClickListener( v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://luminousorg.web.app/"));
            startActivity(intent);
        });

        _check();
    }

    @Override
    public void onStart() {
        super.onStart();

    }


    public void _check() {
        mllllll = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mllllll.cancel();
                        adabar();
                    }
                });
            }
        };

        _timer.schedule(mllllll, (int)(100));

    }

    public void adabar() {

        if (chat.getString("api_key", "").equals("")) {
            String ed = "AIzaSyAtDQy4ecCtpyEk";
            String ed2 = "lYaR-OVjHUG14f3bSJU";
            chat.edit().putString("api_key", ed+ed2).apply();
        }

        ver.edit().putString("current", "1.1").apply();
        delayer = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        delayer.cancel();
                        checkData();
                    }
                });
            }
        };

        _timer.schedule(delayer, (int)(500));
    }

    private void checkData() {
        // API URL to fetch location based on IP address
        String url = "https://luminousorg.web.app/app/secrets";

        // Run network operation in a separate thread
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                String result = "";
                try {
                    // Create the URL object and establish the connection
                    URL urlObject = new URL(url);
                    HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(5000);  // Set a timeout
                    urlConnection.setReadTimeout(5000);

                    // Get the response code and proceed if it's OK
                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = urlConnection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        reader.close();
                        result = stringBuilder.toString();  // Return the response
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(String response) {
                super.onPostExecute(response);


                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String chat_ai_version = jsonResponse.getString("webgen-version");
                    String gemini_api_key = jsonResponse.getString("gemini-api-key-2");
                    String free = jsonResponse.getString("webgen-free");
                    chat.edit().putString("api_key", gemini_api_key).apply();
                    //LuminousUtil.showMessage(getApplicationContext(), chat_ai_version);
                    if (!(chat_ai_version.equals(ver.getString("current", "1.0")))) {
                        //Not equals, prompt for update
                        update(chat_ai_version);
                    }

                    //TODO: if free then don't ip-api, but is not free, then do ip-api
                    if (free.equals("yes")) {
                        success();
                    } else {
                        checkGeoLocation();
                    }


                } catch (JSONException e) {

                }
            }
        }.execute();
    }

    private void update(String versionCode) {

        //TODO: next time update this update method

        /**

         String msg = "New version available: "+versionCode;
         new AlertDialog.Builder(MainActivity.this)
         .setTitle("Update Chat AI")
         .setIcon(R.drawable.app_icon)
         .setCancelable(true)
         .setMessage(msg)
         .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        //intent to a webpage
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://luminousorg.web.app/app/chat-ai"));
        startActivity(intent);
        finishAffinity();
        }
        })
         .show();

         **/
    }


    @SuppressWarnings("GestureBackNavigation")
    @Override
    public void onBackPressed() {

    }

    private void success() {
        //: intent to google login when implemented
        onGenerationComplete();
        i.setClass(getApplicationContext(), HomeActivity.class);
        startActivity(i);
    }

    private void checkGeoLocation() {
        // API URL to fetch location based on IP address
        String url = "http://ip-api.com/json/";

        // Run network operation in a separate thread
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                String result = "";
                try {
                    // Create the URL object and establish the connection
                    URL urlObject = new URL(url);
                    HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setConnectTimeout(5000);  // Set a timeout
                    urlConnection.setReadTimeout(5000);

                    // Get the response code and proceed if it's OK
                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = urlConnection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        reader.close();
                        result = stringBuilder.toString();  // Return the response
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return result;
            }

            @Override
            protected void onPostExecute(String response) {
                super.onPostExecute(response);

                final Set<String> ALLOWED_COUNTRIES = getString(R.string.adstat).equals("org")? new HashSet<>(Arrays.asList(
                        "United States", "United Kingdom", "Canada", "France", "Germany",
                        "Japan", "South Korea", "Norway", "Switzerland", "Sweden",
                        "The Netherlands", "Australia", "Denmark", "Bangladesh"
                )) : new HashSet<>(Arrays.asList(
                        "United States", "United Kingdom", "Canada", "France", "Germany",
                        "Japan", "South Korea", "Norway", "Switzerland", "Sweden",
                        "The Netherlands", "Australia", "Denmark"
                ));

                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    String countryName = jsonResponse.getString("country");

                    if (ALLOWED_COUNTRIES.contains(countryName)) {
                        success();
                    } else {
                        failed(countryName);
                    }

                } catch (JSONException e) {
                    failed("your location");
                }
            }
        }.execute();
    }

    private void failed(String country) {
        // Implement dialog
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Notice")
                .setIcon(R.drawable.web_gen_logo)
                .setCancelable(false)
                .setMessage("Sorry, Web Gen is unavailable in "+country+". Web Gen is available in United States, United Kingdom, Canada, Australia, Germany, Japan, South Korea, France, Norway, Switzerland, Sweden, Denmark and Netherlands.")
                .setPositiveButton("GOT IT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Exit the app when "GOT IT" is clicked
                        finishAffinity();
                    }
                })
                .show();
    }

    //: update google service json before uploading your app, update build gradle (app)
    public void toast(String s) {
        LuminousUtil.showMessage(getApplicationContext(), s);
    }


}
