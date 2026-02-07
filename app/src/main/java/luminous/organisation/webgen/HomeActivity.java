package luminous.organisation.webgen;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ListView projectListView;
    private FloatingActionButton fabAddProject;
    private final String PREF_CHAT_NAME = "chat";
    private final String PREF_ROOT_KEY = "root";
    private List<ProjectItem> projectList;
    private ProjectAdapter adapter;
    private LinearLayout bannerContainer;
    private Boolean reload = true;
    private InterstitialAd interstitialAd;
    private Boolean reloadInt = true;
    private String Stype, SDEST = "";
    private SharedPreferences chat, ver;

    // Data class equivalent in Java
    public static class ProjectItem {
        public String name;
        public String dest;

        public ProjectItem(String name, String dest) {
            this.name = name;
            this.dest = dest;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Assumes activity_home.xml is set up

        projectListView = findViewById(R.id.project_list_view);
        fabAddProject = findViewById(R.id.fab_add_project);
        bannerContainer = findViewById(R.id.bannerContainer);
        chat = getSharedPreferences("chat", Context.MODE_PRIVATE);
        ver = getSharedPreferences("ver", Context.MODE_PRIVATE);

        loadProjects();
        bannerAd(bannerContainer);
        loadInterstitial();

        fabAddProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddProjectDialog();
            }
        });

        //checkData();

    }

    private void bannerAd(LinearLayout bannerContainer) {
        if (getString(R.string.adstat).equals("org")) {
            bannerContainer.setVisibility(View.GONE);
            return;
        }

        AdView adView = new AdView(getApplicationContext());
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getString(R.string.banner));
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        bannerContainer.removeAllViews();
        bannerContainer.addView(adView, params);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                // Code to be executed when the user clicks on an ad.
                //showMessage("ad clicked");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                // Code to be executed when an ad request fails.

                if (reload) {
                    reload = false;
                    bannerAd(bannerContainer);
                } else {
                    bannerContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        });
    }

    private void loadInterstitial(){
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                getString(R.string.interstitial),
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        HomeActivity.this.interstitialAd = interstitialAd;
                        //Log.i(TAG, "onAdLoaded");
                        //Toast.makeText(SettingsActivity.this, "onAdLoaded()", Toast.LENGTH_SHORT).show();
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Called when fullscreen content is dismissed.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        HomeActivity.this.interstitialAd = null;
                                        adDismissed(Stype, SDEST);
                                        //Log.d("TAG", "The ad was dismissed.");
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        // Called when fullscreen content failed to show.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        HomeActivity.this.interstitialAd = null;
                                        //Log.d("TAG", "The ad failed to show.");
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when fullscreen content is shown.
                                        //Log.d("TAG", "The ad was shown.");
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        //Log.i(TAG, loadAdError.getMessage());
                        interstitialAd = null;
                        if (reloadInt) {
                            reloadInt = false;
                            loadInterstitial();
                        }

                    }
                });
    }

    public void adDismissed(String type, String DEST) {
        switch (type) {
            case "ai" :
                Intent compilerIntent = new Intent(HomeActivity.this, GenActivity.class);

                // Pass the DEST string as an extra
                compilerIntent.putExtra("DEST_PROJECT_KEY", DEST);

                // Start the activity
                startActivity(compilerIntent);
                break;
            case "edit" :
                Intent compilerIntent2 = new Intent(HomeActivity.this, ConsoleActivity.class);

                // Pass the DEST string as an extra
                compilerIntent2.putExtra("DEST_PROJECT_KEY", DEST);

                // Start the activity
                startActivity(compilerIntent2);
                break;
            case "run" :
                Intent compilerIntent3 = new Intent(HomeActivity.this, CompilerActivity.class);

                SharedPreferences fs = getSharedPreferences(DEST, Context.MODE_PRIVATE);

                // Load the string stored under the "code" key
                String code = fs.getString("code", "");
                // Send the entire code string to the CompilerActivity
                compilerIntent3.putExtra("code", code);

                // We also send the project key
                compilerIntent3.putExtra("DEST_PROJECT_KEY", DEST);

                startActivity(compilerIntent3);

                //Toast.makeText(context, "Launching Compiler for: " + DEST, Toast.LENGTH_SHORT).show();

                break;
            default:
                break;
        }

    }

    private void showIntAd(String type, String DEST) {
        if (interstitialAd != null) {
            interstitialAd.show(HomeActivity.this);
        } else {
            adDismissed(type, DEST);
        }
    }

    /**
     * Loads the project list from SharedPreferences "chat" -> "root" key.
     */
    private void loadProjects() {
        SharedPreferences prefs = getSharedPreferences(PREF_CHAT_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(PREF_ROOT_KEY, "[]");

        projectList = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String dest = jsonObject.getString("dest");
                projectList.add(new ProjectItem(name, dest));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading projects data.", Toast.LENGTH_LONG).show();
        }

        adapter = new ProjectAdapter(this, projectList);
        projectListView.setAdapter(adapter);
    }

    /**
     * Shows a dialog to add a new project. (Same logic as requested)
     */
    private void showAddProjectDialog() {
        // Layout for dialog_add_project.xml (assumed to exist)
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_project, null);
        final EditText editText = dialogView.findViewById(R.id.edittext_project_name);
        Button createButton = dialogView.findViewById(R.id.button_create);

        // Set character limit
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Project")
                .setView(dialogView)
                .create();

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = editText.getText().toString().trim();

                if (newName.isEmpty()) {
                    Toast.makeText(HomeActivity.this, "Project name cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check for existing name (case-insensitive)
                for (ProjectItem item : projectList) {
                    if (item.name.equalsIgnoreCase(newName)) {
                        Toast.makeText(HomeActivity.this, "Project name already exists.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                addNewProject(newName);
                dialog.dismiss();
            }
        });

        dialog.show();
    }


    /**
     * Adds a new project to SharedPreferences and updates the UI.
     */
    private void addNewProject(String name) {
        // 1. Update the "chat" root JSON
        SharedPreferences chatPrefs = getSharedPreferences(PREF_CHAT_NAME, Context.MODE_PRIVATE);
        String jsonString = chatPrefs.getString(PREF_ROOT_KEY, "[]");

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            JSONObject newProjectJson = new JSONObject();
            newProjectJson.put("name", name);
            newProjectJson.put("dest", name);
            jsonArray.put(newProjectJson);

            chatPrefs.edit().putString(PREF_ROOT_KEY, jsonArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving project list.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Create the new project's dedicated SharedPreferences (name.xml)
        SharedPreferences projectPrefs = getSharedPreferences(name, Context.MODE_PRIVATE);
        projectPrefs.edit()
                .putString("title", name)
                .putString("html", "")
                .putString("css", "")
                .putString("js", "")
                .putString("special", "")
                .putString("console", "")
                .putString("comment", "")
                .putString("code", "")
                .putString("others", "")
                .apply();

        // 3. Update the ListView
        ProjectItem newProjectItem = new ProjectItem(name, name);
        projectList.add(newProjectItem);
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Project '" + name + "' created!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Deletes a project from the list and its associated SharedPreferences.
     */
    private void deleteProject(final ProjectItem projectToDelete) {
        new AlertDialog.Builder(this)
                .setTitle("Delete "+projectToDelete.name)
                .setMessage("Are you sure you want to delete the project '" + projectToDelete.name + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // 1. Delete the project's dedicated SharedPreferences (project name)
                    SharedPreferences projectPrefs = getSharedPreferences(projectToDelete.name, Context.MODE_PRIVATE);
                    projectPrefs.edit().clear().apply();

                    // 2. Update the "chat" root JSON (remove the entry)
                    SharedPreferences chatPrefs = getSharedPreferences(PREF_CHAT_NAME, Context.MODE_PRIVATE);
                    String jsonString = chatPrefs.getString(PREF_ROOT_KEY, "[]");

                    try {
                        JSONArray jsonArray = new JSONArray(jsonString);
                        JSONArray newJsonArray = new JSONArray();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            // Only add projects that are NOT the one we are deleting
                            if (!jsonObject.getString("name").equals(projectToDelete.name)) {
                                newJsonArray.put(jsonObject);
                            }
                        }

                        chatPrefs.edit().putString(PREF_ROOT_KEY, newJsonArray.toString()).apply();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(HomeActivity.this, "Error deleting project from list.", Toast.LENGTH_SHORT).show();
                    }

                    // 3. Update the ListView
                    projectList.remove(projectToDelete);
                    adapter.notifyDataSetChanged();

                    Toast.makeText(HomeActivity.this, "Project '" + projectToDelete.name + "' deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        //declare the dialog below
        AlertDialog.Builder xjxitdiyf = new AlertDialog.Builder(this);

        xjxitdiyf.setTitle("Web Gen");
        xjxitdiyf.setIcon(R.drawable.web_gen_logo);
        xjxitdiyf.setMessage("Do you want to exit?");
        xjxitdiyf.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface _dialog, int _which) {
                finishAffinity();
            }
        });
        xjxitdiyf.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface _dialog, int _which) {

            }
        });
        xjxitdiyf.create().show();
    }

    /**
     * Custom Adapter for the ListView.
     */
    private class ProjectAdapter extends ArrayAdapter<ProjectItem> {

        private Context context;
        private List<ProjectItem> projects;

        public ProjectAdapter(Context context, List<ProjectItem> projects) {
            super(context, 0, projects);
            this.context = context;
            this.projects = projects;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View listItemView = convertView;
            if (listItemView == null) {
                listItemView = LayoutInflater.from(context).inflate(
                        R.layout.list_items,
                        parent,
                        false
                );
            }

            final ProjectItem currentProject = projects.get(position);

            TextView projectNameText = listItemView.findViewById(R.id.project_name_text);
            ImageView editIcon = listItemView.findViewById(R.id.edit_icon);
            ImageView iconAi = listItemView.findViewById(R.id.icon_ai);
            ImageView iconCode = listItemView.findViewById(R.id.icon_code);
            ImageView iconRun = listItemView.findViewById(R.id.icon_run);
            ImageView iconShare = listItemView.findViewById(R.id.icon_share);

            projectNameText.setText(currentProject.name);

            // Set the DELETION functionality on the EDIT icon
            editIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteProject(currentProject);
                }
            });

            // Common click action for the four bottom icons
            final String DEST = currentProject.dest;

            View.OnClickListener actionClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // DEST is now available for use (e.g., passing to a new Activity/Fragment)
                    // Toast.makeText(context, "Action on " + currentProject.name + " -> DEST: " + DEST, Toast.LENGTH_SHORT).show();

                    // Specific actions based on the clicked icon:
                    int id = v.getId();
                    if (id == R.id.icon_ai) { /* Launch AI screen with DEST */
                        Stype = "ai";
                        SDEST = DEST;
                        showIntAd("ai", DEST);
                    }
                    else if (id == R.id.icon_code) { /* Launch Code editor screen with DEST */
                        Stype = "edit";
                        SDEST = DEST;
                        showIntAd("edit", DEST);
                    }
                    else if (id == R.id.icon_run) { /* Launch WebView/Run screen with DEST */
                        Stype = "run";
                        SDEST = DEST;
                        showIntAd("run", DEST);
                    }
                    else if (id == R.id.icon_share) { /* Handle sharing logic with DEST */
                    // Share the String from "code" key according to DEST private data reference in SharedPreference
                        // The DEST value is used as the SharedPreferences file name
                        SharedPreferences projectPrefs = context.getSharedPreferences(DEST, Context.MODE_PRIVATE);

                        // Retrieve the String from the "code" key
                        // The default value is an empty string ("")
                        String codeString = projectPrefs.getString("code", "");

                        // To show the retrieved string (for verification/sharing)
                        if (codeString.isEmpty()) {
                            Toast.makeText(context, "No code found for " + currentProject.name, Toast.LENGTH_LONG).show();
                        } else {
                            // You can now use the 'codeString' for sharing (e.g., via Intent)
                            //Toast.makeText(context, "Code fetched! Length: " + codeString.length(), Toast.LENGTH_LONG).show();

                            // Example: Start a share Intent with the codeString

                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, codeString);
                            context.startActivity(Intent.createChooser(shareIntent, "Share code of project \""+currentProject.name+"\" via"));

                        }
                    }
                }
            };

            iconAi.setOnClickListener(actionClickListener);
            iconCode.setOnClickListener(actionClickListener);
            iconRun.setOnClickListener(actionClickListener);
            iconShare.setOnClickListener(actionClickListener);

            return listItemView;
        }
    }
}