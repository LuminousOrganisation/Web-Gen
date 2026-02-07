package luminous.organisation.webgen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GenActivity extends AppCompatActivity {

    private static final String DEST_INTENT_KEY = "DEST_PROJECT_KEY";
    private String projectDest;
    private SharedPreferences projectPrefs, chat;
    private LinearLayout bannerContainer;
    private Boolean reload = true;
    private RewardedAd rewardedAd;

    private static final String[] GEMINI_MODELS = {
            "gemini-3-pro-preview",
            "gemini-3-flash-preview",
            "gemini-2.5-pro",
            "gemini-2.5-pro-flash",
            "gemini-2.5-pro-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemma-3n-e2b-it",
            "gemma-3n-e4b-it",
            "gemma-3-1b-it",
            "gemma-3-4b-it",
            "gemma-3-12b-it",
            "gemma-3-27b-it",
    };


    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final int MAX_RETRIES = 5;

    // Mapping SharedPreferences keys to EditText views
    private Map<String, EditText> inputFields;

    private ViewPager2 viewPager;
    private TextView tvStepIndicator;
    private ImageButton btnPrev, btnNext;
    private List<InputPageModel> inputPages;
    // Remove inputFields; it is no longer used.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gen);

        // 1. Retrieve project key (DEST)
        projectDest = getIntent().getStringExtra(DEST_INTENT_KEY);
        if (projectDest == null || projectDest.isEmpty()) {
            Toast.makeText(this, "Error: Project key not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize SharedPreferences for the specific project
        chat = getSharedPreferences("chat", Context.MODE_PRIVATE);
        projectPrefs = getSharedPreferences(projectDest, Context.MODE_PRIVATE);
        bannerContainer = findViewById(R.id.bannerContainer);

// 2. Setup ViewPager and Navigation
        viewPager = findViewById(R.id.viewPager);
        tvStepIndicator = findViewById(R.id.tv_step_indicator);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        inputPages = new ArrayList<>();
        inputPages.add(new InputPageModel("Project Title", "title",
                "e.g., Professional Age Calculator"));
        inputPages.add(new InputPageModel("Structure (HTML)", "html",
                "e.g., A Date Picker for birthday and a 'Calculate' button."));
        inputPages.add(new InputPageModel("Styling (CSS)", "css",
                "e.g., Modern dark theme with smooth fade-in animations."));
        inputPages.add(new InputPageModel("Logic (JS)", "js",
                "e.g., Calculate age and display the result in a popup Dialog Box."));
        inputPages.add(new InputPageModel("Special Instructions", "special",
                "e.g., Show precise age in years, months, and days."));
        InputPagerAdapter adapter = new InputPagerAdapter(this, inputPages);
        viewPager.setAdapter(adapter);

        // Navigation Sync
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvStepIndicator.setText("Step " + (position + 1) + " of " + inputPages.size());
                btnPrev.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
                btnNext.setVisibility(position == inputPages.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            }
        });

        btnPrev.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem() - 1));
        btnNext.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem() + 1));

        loadRewardedAd();

        // 5. Setup Generate button
        Button btnGenerate = findViewById(R.id.btn_generate);
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ensure all current field data is saved before generating prompt
                saveAllDataToPrefs();

                if (getString(R.string.adstat).equals("org")) {
                    _reward();
                } else {
                    showVideoAd();
                }

                // For demonstration, show the generated prompt length and content
                //Toast.makeText(GenActivity.this, "Prompt generated! Length: " + finalPrompt.length(), Toast.LENGTH_LONG).show();

                // In a real scenario, you would now send 'finalPrompt' to the Gemini API
                // For example: sendPromptToGemini(finalPrompt);
            }
        });
        bannerAd(bannerContainer);


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

//    private void initializeViews() {
//        inputFields = new HashMap<>();
//
//        // Map EditTexts to their corresponding SharedPreferences keys
//        inputFields.put("title", findViewById(R.id.edit_title));
//        inputFields.put("html", findViewById(R.id.edit_design)); // 'html' key for Design
//        inputFields.put("css", findViewById(R.id.edit_animation)); // 'css' key for Animation
//        inputFields.put("js", findViewById(R.id.edit_functionality)); // 'js' key for Functionality
//        inputFields.put("special", findViewById(R.id.edit_special)); // 'special' key for Special Instruction
//
//    }

    private void loadDataFromPrefs() {
        for (Map.Entry<String, EditText> entry : inputFields.entrySet()) {
            String key = entry.getKey();
            EditText editText = entry.getValue();

            // Load value from SharedPreferences, defaulting to empty string
            String savedValue = projectPrefs.getString(key, "");
            editText.setText(savedValue);
        }
    }

    private void setupAutoSaveListeners() {
        View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // When an EditText loses focus, save its current content
                    saveSingleDataToPrefs((EditText) v);
                }
            }
        };

        for (EditText editText : inputFields.values()) {
            editText.setOnFocusChangeListener(focusChangeListener);
        }
    }

    private void saveSingleDataToPrefs(EditText editText) {
        String keyToSave = null;
        String currentValue = editText.getText().toString();

        // Find the key corresponding to the EditText
        for (Map.Entry<String, EditText> entry : inputFields.entrySet()) {
            if (entry.getValue() == editText) {
                keyToSave = entry.getKey();
                break;
            }
        }

        if (keyToSave != null) {
            projectPrefs.edit().putString(keyToSave, currentValue).apply();
            // Log for debugging: Toast.makeText(this, "Saved " + keyToSave, Toast.LENGTH_SHORT).show();
        }
    }

    // Ensures all data is saved, especially useful just before exiting or generating
    private void saveAllDataToPrefs() {
//        SharedPreferences.Editor editor = projectPrefs.edit();
//        for (Map.Entry<String, EditText> entry : inputFields.entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue().getText().toString();
//            editor.putString(key, value);
//        }
//        editor.apply();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        saveAllDataToPrefs();
        super.onBackPressed();
    }

    /**
     * Constructs the final prompt string that will be sent to the Gemini API.
     * This includes the 5 user inputs plus a special internal instruction string.
     * @return The complete, formatted prompt string.
     */
    private String generateAIPromptString() {
// Read directly from Prefs (the Adapter saves data here in real-time)
        String title = projectPrefs.getString("title", "");
        String design = projectPrefs.getString("html", "");
        String animation = projectPrefs.getString("css", "");
        String functionality = projectPrefs.getString("js", "");
        String special = projectPrefs.getString("special", "");

        // This is the special string provided by the app (the system instruction context)
        String appContextInstruction =
                "Generate a single, self-contained HTML file containing all necessary HTML, CSS (in a <style> block), and JavaScript (in a <script> block). Do not use external files or libraries unless specifically requested. This HTML should be supported and well scaled for both mobile and desktop. No bug should be exist. The design, animation and functionality should be user friendly as instructed. Never skip any single instruction provided here. Ensure the output is only the code, don't provide anything other than code, ready to be executed.";

        // Construct the structured prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append(appContextInstruction).append("\n\n");
        prompt.append("--- USER PROJECT REQUEST ---\n");
        prompt.append("Title: ").append(title).append("\n");
        prompt.append("Design & Structure (HTML): ").append(design).append("\n");
        prompt.append("Styling & Animation (CSS): ").append(animation).append("\n");
        prompt.append("Interactivity & Logic (JS): ").append(functionality).append("\n");
        prompt.append("Additional Constraints: ").append(special).append("\n");
        prompt.append("----------------------------\n");

        return prompt.toString();
    }

    private String getApiKey() {
        return chat.getString("api_key", "");
    }

    /**
     * Requests a code response from the Gemini API, trying multiple models in order
     * with exponential backoff for retries.

     */

    private void getCodeResponse(String promptText) {
        final String apiKey = getApiKey();
        if (apiKey.isEmpty()) return;

        executorService.execute(() -> {
            String result = null;
            boolean success = false;

            // Try models in order
            for (String model : GEMINI_MODELS) {
                if (success) break;
                try {
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // Manual JSON string to keep it simple and avoid library overhead
                    String escapedPrompt = promptText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                    String jsonInput = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";

                    try (OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream())) {
                        os.write(jsonInput);
                    }

                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);

                        JSONObject response = new JSONObject(sb.toString());
                        result = response.getJSONArray("candidates")
                                .getJSONObject(0).getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0).getString("text");
                        success = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final String finalCode = result;
            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (finalSuccess && finalCode != null) {
                    projectPrefs.edit().putString("code", clean(finalCode)).apply();
                    Toast.makeText(GenActivity.this, "Code Generated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GenActivity.this, "Internal Error", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     private void getCodeResponse(String instruction) {
     final String apiKey = getApiKey();

     if (apiKey.isEmpty()) {
     // CONVERSION: Replaced lambda with anonymous inner class
     runOnUiThread(new Runnable() {
    @Override
    public void run() {
    Toast.makeText(GenActivity.this, "Luminous AI Key is missing. Cannot generate code.", Toast.LENGTH_LONG).show();
    }
    });
     return;
     }

     // Show loading indicator
     // CONVERSION: Replaced lambda with anonymous inner class
     runOnUiThread(new Runnable() {
    @Override
    public void run() {
    //Toast.makeText(GenActivity.this, "Starting code generation across models...", Toast.LENGTH_LONG).show();
    }
    });

     executorService.execute(new Runnable() {
    @Override
    public void run() {
    String generatedCode = null;
    String errorMessage = "Failed to generate code after trying all models.";
    boolean success = false;

    // --- 1. MODEL ITERATION LOOP ---
    for (String currentModel : GEMINI_MODELS) {
    if (success) break;

    long delay = 1000; // Initial delay for exponential backoff

    // --- 2. RETRY LOOP (with Exponential Backoff) ---
    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
    try {
    String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent?key=" + apiKey;
    URL url = new URL(apiUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    // --- Construct Simple Payload ---
    JSONObject userPart = new JSONObject();
    userPart.put("text", instruction);

    JSONObject content = new JSONObject();
    content.put("parts", new JSONArray().put(userPart));
    content.put("role", "user");

    JSONObject payload = new JSONObject();
    payload.put("contents", new JSONArray().put(content));
    payload.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", "Respond ONLY with the complete, self-contained code block. Do not include markdown or external commentary."))));


    try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")) {
    writer.write(payload.toString());
    }

    int responseCode = conn.getResponseCode();

    if (responseCode == HttpURLConnection.HTTP_OK) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
    StringBuilder response = new StringBuilder();
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);
    }
    JSONObject jsonResponse = new JSONObject(response.toString());

    if (jsonResponse.has("candidates")) {
    JSONObject candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0);
    if (candidate.has("content")) {
    JSONObject contentObj = candidate.getJSONObject("content");
    if (contentObj.has("parts")) {
    JSONObject firstPart = contentObj.getJSONArray("parts").getJSONObject(0);
    if (firstPart.has("text")) {
    generatedCode = firstPart.getString("text");
    success = true;
    break; // Success! Exit inner retry loop
    }
    }
    }
    }
    }
    } else if (responseCode == 429) { // Throttling
    if (attempt < MAX_RETRIES - 1) {
    Thread.sleep(delay);
    delay *= 2; // Exponential backoff
    continue; // Continue inner loop
    } else {
    // Max retries reached for this model
    errorMessage = "Model " + currentModel + " failed due to throttling.";
    break; // Break inner retry loop, continue to next model
    }
    } else {
    // Other non-retriable HTTP error (e.g., 400 Bad Request, 404 Not Found)
    errorMessage = "Model " + currentModel + " failed with HTTP code: " + responseCode;
    break; // Break inner retry loop, continue to next model
    }
    } catch (Exception e) {
    e.printStackTrace();
    // Catch network/parsing errors and retry
    if (attempt < MAX_RETRIES - 1) {
    try {
    Thread.sleep(delay);
    delay *= 2;
    continue; // Continue inner loop
    } catch (InterruptedException ie) {
    Thread.currentThread().interrupt();
    break; // Break both loops
    }
    } else {
    errorMessage = "Model " + currentModel + " failed due to error: " + e.getMessage();
    break; // Break inner retry loop, continue to next model
    }
    }
    } // End of inner retry loop
    if (success) break; // Break outer model loop on success
    } // End of outer model loop

    final String finalGeneratedCode = generatedCode;
    final String finalErrorMessage = errorMessage;
    // FIX: Make a final copy of the 'success' variable for the inner Runnable
    final boolean finalSuccess = success;

    // --- UI Update (User-Provided Logic) ---
    // CONVERSION: Replaced lambda with anonymous inner class
    runOnUiThread(new Runnable() {
    @Override
    public void run() {
    // Use finalSuccess instead of success
    Log.e("TAG", "finalSuccess: " + finalSuccess+"\nGenCode:\n"+finalGeneratedCode);
    if (finalGeneratedCode != null && finalSuccess) {
    // Check for <html> (case-insensitive)
    projectPrefs.edit().putString("code", clean(finalGeneratedCode)).apply();
    Toast.makeText(GenActivity.this, "Code successfully generated and saved!", Toast.LENGTH_LONG).show();
    if (finalGeneratedCode.toLowerCase().contains("<html>")) {
    // Save the code to the specific shared preference key "code"
    } else {
    // The response didn't look like code
    //Toast.makeText(GenActivity.this, "AI response was not valid HTML code. Response length: " + finalGeneratedCode.length(), Toast.LENGTH_LONG).show();
    }
    } else {
    Toast.makeText(GenActivity.this, "An internal error is occurred. Cannot generate code.", Toast.LENGTH_LONG).show();
    }
    }
    });
    // ----------------------------------------



    }
    });
     }
     **/

    private String clean (String old) {
        String now = old;
        if (now.substring(0, 7).equals("```html")) {
            now = now.substring(7, now.length());
        }
        if (now.substring(now.length() - 3, now.length()).equals("```")) {
            now = now.substring(0, now.length() - 3);
        }
        return "<!-- Developed by Web Gen Mobile App https://luminousorg.web.app/app/webgen  -->\n" + now + "\n<!-- Developed by Web Gen Mobile App https://luminousorg.web.app/app/webgen  -->";
    }

    private void loadRewardedAd() {
        if (rewardedAd == null) {
            //isLoading = true;
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(
                    this,
                    getString(R.string.rewarded),
                    adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error.
                            //Log.d(TAG, loadAdError.getMessage());
                            rewardedAd = null;
                            //NtfActivity.this.isLoading = false;

                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            GenActivity.this.rewardedAd = rewardedAd;
                            //Log.d(TAG, "onAdLoaded");
                            //NtfActivity.this.isLoading = false;

                        }
                    });
        }
    }

    private void showVideoAd() {
        if (rewardedAd == null) {
            LuminousUtil.showMessage(getApplicationContext(), "You have to watch a video ad to generate code by Luminous AI. Wait for the ad to be loaded.");
            return;
        }


        rewardedAd.setFullScreenContentCallback(
                new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        //Log.d(TAG, "onAdShowedFullScreenContent");
                        //LuminousUtil.showMessage(getApplicationContext(), "Generating response! While you wait ...");
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when ad fails to show.
                        //Log.d(TAG, "onAdFailedToShowFullScreenContent");
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null;

                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null;
                        loadRewardedAd();
                        //LuminousUtil.showMessage(getApplicationContext(), "You didn't finish watching the ad.");
                    }
                });
        Activity activityContext = GenActivity.this;
        rewardedAd.show(
                activityContext,
                new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        _reward();
                    }
                });
    }

    public void _reward() {
        // Construct and process the final AI prompt
        String finalPrompt = generateAIPromptString();

        LuminousUtil.showMessage(GenActivity.this, "Stay connected. Your code will be generated within 1-2 minutes.");


        getCodeResponse(finalPrompt);
    }


    // Ensure the executor service is shut down when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    // --- ADD THESE AT THE BOTTOM OF GENACTIVITY ---

    private class InputPagerAdapter extends RecyclerView.Adapter<InputPagerAdapter.ViewHolder> {
        private final List<InputPageModel> pages;
        private final Context context;

        public InputPagerAdapter(Context context, List<InputPageModel> pages) {
            this.context = context;
            this.pages = pages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_input_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            InputPageModel page = pages.get(position);
            holder.tvLabel.setText(page.label);
            holder.etInput.setHint(page.hint);
            holder.etInput.setText(projectPrefs.getString(page.prefKey, ""));

            if (holder.watcher != null) holder.etInput.removeTextChangedListener(holder.watcher);
            holder.watcher = new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(Editable s) {
                    projectPrefs.edit().putString(page.prefKey, s.toString()).apply();
                }
            };
            holder.etInput.addTextChangedListener(holder.watcher);
        }

        @Override public int getItemCount() { return pages.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvLabel; EditText etInput; TextWatcher watcher;
            public ViewHolder(@NonNull View v) {
                super(v);
                tvLabel = v.findViewById(R.id.tv_label);
                etInput = v.findViewById(R.id.et_input);
            }
        }
    }

    private static class InputPageModel {
        String label, prefKey, hint;
        InputPageModel(String label, String prefKey, String hint) {
            this.label = label; this.prefKey = prefKey; this.hint = hint;
        }
    }
}
