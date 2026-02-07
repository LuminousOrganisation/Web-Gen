package luminous.organisation.webgen;

import static androidx.core.content.ContextCompat.startActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.util.ArrayList;

public class CompilerActivity extends AppCompatActivity {

    private static final String CODE_INTENT_KEY = "code";
    private static final String DEST_INTENT_KEY = "DEST_PROJECT_KEY";

    private WebView runtimeWebView;
    private ProgressBar progressBar;
    private TextView webTitle;
    private ImageView webIcon;
    private LinearLayout consoleContainer;
    private LinearLayout consoleHeader;
    private TextView consoleHeaderAlias;
    private ListView consoleListView;
    private LinearLayout bannerContainer;
    private Boolean reload = true;
    private ArrayList<String> consoleLogs;
    private ArrayAdapter<String> consoleAdapter;

    private boolean isConsoleCollapsed = false;
    private final float CONSOLE_WEIGHT_EXPANDED = 0.43f; // ~30% of total height (30/70 = 0.43)
    private final float CONSOLE_WEIGHT_COLLAPSED = 0.0f;
    private RewardedInterstitialAd rewardedInterstitialAd;

    private static final int FILECHOOSER_REQUEST_CODE = 1;
    private ValueCallback<Uri[]> mFilePathCallback; // Holds the callback needed for file uploads

    // --- Lifecycle and Setup ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compiler);

        // Initialize Views
        runtimeWebView = findViewById(R.id.runtime_webview);
        progressBar = findViewById(R.id.progress_bar);
        webTitle = findViewById(R.id.web_title);
        webIcon = findViewById(R.id.web_icon);

        consoleContainer = findViewById(R.id.console_container);
        consoleContainer.setVisibility(View.GONE);

        consoleHeaderAlias = findViewById(R.id.console_header_alias);
        consoleListView = findViewById(R.id.console_listview);
        bannerContainer = findViewById(R.id.bannerContainer);

        // Setup Console
        setupConsole();

        // Setup WebView
        String code = getIntent().getStringExtra(CODE_INTENT_KEY);
        String projectKey = getIntent().getStringExtra(DEST_INTENT_KEY);
        webTitle.setText(projectKey != null ? projectKey : "Runtime");
        webTitle.setSelected(true);
        setupWebView(code);

        // Setup Listeners
        setupListeners();

        bannerAd(bannerContainer);
        loadAd();
    }

    public void loadAd() {
        if (rewardedInterstitialAd == null) {
            RewardedInterstitialAd.load(CompilerActivity.this, getString(R.string.rewardedinterstitial),
                    new AdRequest.Builder().build(), new RewardedInterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(RewardedInterstitialAd ad) {
                            CompilerActivity.this.rewardedInterstitialAd = ad;
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            //Log.d(TAG, loadAdError.toString());
                            rewardedInterstitialAd = null;
                        }
                    });
        }
    }

    private void showAd() {
        if (rewardedInterstitialAd != null) {

            //LuminousUtil.showMessage(getApplicationContext(), "Generating response! While you wait ...");

            rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    //Log.d("TAG", "Ad was clicked.");
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    //Log.d("TAG", "Ad dismissed fullscreen content.");
                    rewardedInterstitialAd = null;
                    //loadAd();
                    adDismissed();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // Called when ad fails to show.
                    //Log.e(TAG, "Ad failed to show fullscreen content.");
                    rewardedInterstitialAd = null;
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    //Log.d(TAG, "Ad recorded an impression.");
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    //Log.d(TAG, "Ad showed fullscreen content.");
                }
            });
            Activity activityContext = CompilerActivity.this;
            rewardedInterstitialAd.show(activityContext, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // _reward(data);
                }
            });

        } else {
            adDismissed();
        }
    }

    private void adDismissed() {
        finish();
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

    // --- WebView Configuration ---

    private void setupWebView(String code) {
        WebSettings settings = runtimeWebView.getSettings();

        // 1. Essential Web Feature Support
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(true);      // Allows the WebView to support zoom
        settings.setBuiltInZoomControls(true); // Enables the zoom controls (pinch gesture)
        settings.setDisplayZoomControls(false); // OPTIONAL: Hides the on-screen zoom buttons
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        // For the "best" experience, allow mixed content and local storage access
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 2. Load Content
        if (code != null && !code.isEmpty()) {
            // Load the HTML content directly using loadDataWithBaseURL
            // The base URL must be set for relative paths/links within the content to work correctly
            runtimeWebView.loadDataWithBaseURL("file:///android_asset/", code, "text/html", "UTF-8", null);
        } else {
            runtimeWebView.loadData("<html><body><h1>No code provided.</h1></body></html>", "text/html", "UTF-8");
        }

        // 3. Set Custom Clients
        runtimeWebView.setWebViewClient(new CustomWebViewClient());
        runtimeWebView.setWebChromeClient(new CustomWebChromeClient());

        runtimeWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                // Call the robust download handler logic (defined in step 2)
                handleDownloadRequest(url);
            }
        });

        // Initial state of navigation buttons
        updateNavigationButtons();
    }

    // Helper method to open the link using the system's default browser/handler
    private void handleDownloadRequest(String url) {
        LuminousUtil.showMessage(getApplicationContext(), url);
        try {
            // Use ACTION_VIEW to force the system browser/downloader to handle the link.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));

            // Add flag to ensure the new activity runs independently
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "Opening download in browser...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Could not find app to handle download.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "External download failed.", Toast.LENGTH_LONG).show();
            Log.e("Download", "External download attempt failed: " + e.getMessage());
        }
    }

    private void setupConsole() {
        consoleLogs = new ArrayList<>();

        // Custom ArrayAdapter for color-coding log messages
        consoleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, consoleLogs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                String log = consoleLogs.get(position);

                // Simple color coding: Red for errors, Yellow for warnings
                if (log.startsWith("ERROR:")) {
                    textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_light));
                } else if (log.startsWith("WARNING:")) {
                    textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_orange_light));
                } else {
                    textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                }

                // Setting background for the log list items
                textView.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                return textView;
            }
        };
        consoleListView.setAdapter(consoleAdapter);

        // Add long press listener to copy a specific log message
        consoleListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                copyLogToClipboard(consoleLogs.get(position));
                return true; // Consume the long click
            }
        });

        // Set console to initial expanded state
        // setConsoleVisibility(true);
    }

    // --- Utility Methods ---

    private void copyLogToClipboard(String log) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Console Log", log);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Log copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNavigationButtons() {
        //btnBack.setEnabled(runtimeWebView.canGoBack());
        //btnForward.setEnabled(runtimeWebView.canGoForward());

        // Visually indicate state (e.g., dim the disabled button)
        //btnBack.setAlpha(runtimeWebView.canGoBack() ? 1.0f : 0.3f);
        //btnForward.setAlpha(runtimeWebView.canGoForward() ? 1.0f : 0.3f);
    }

    private void changedConsoleVisibility() {

        if (ConsoleCollapsed()) {
            // EXPAND: Show the entire console container
            consoleContainer.setVisibility(View.VISIBLE);
            //isConsoleCollapsed = false;
        } else {
            // COLLAPSE: Hide the entire console container completely
            // Setting to GONE removes it from the layout, allowing the WebView to expand.
            consoleContainer.setVisibility(View.GONE);
            //isConsoleCollapsed = true;
        }

        // IMPORTANT: Force the parent layout to recalculate its child sizes
        // after removing/adding the console container.
        // ((View) consoleContainer.getParent()).requestLayout();
    }

    // --- Listeners and Overrides ---

    private void setupListeners() {
        /**
         btnBack.setOnClickListener(v -> {
         if (runtimeWebView.canGoBack()) {
         runtimeWebView.goBack();
         }
         });

         btnForward.setOnClickListener(v -> {
         if (runtimeWebView.canGoForward()) {
         runtimeWebView.goForward();
         }
         });

         **/

        consoleHeaderAlias.setOnClickListener(v -> {
            changedConsoleVisibility();
        });
    }

    private Boolean ConsoleCollapsed() {
        if (consoleContainer.getVisibility() == View.VISIBLE) {
            isConsoleCollapsed = false;
        } else {
            isConsoleCollapsed = true;
        }

        return isConsoleCollapsed;
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (runtimeWebView.canGoBack()) {
            runtimeWebView.goBack();
        } else {
            showAd();
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.INVISIBLE);
            updateNavigationButtons();

            // Update title after page loads if it's a real URL/page (not just the initial data load)
            if (!url.startsWith("file:///android_asset/")) {
                webTitle.setText(view.getTitle() != null ? view.getTitle() : "Web Runtime");
                //set web_icon from the url

            }


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == FILECHOOSER_REQUEST_CODE) {
            if (mFilePathCallback == null) return;

            Uri[] results = null;

            // Check if the user selected a file
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    String dataString = intent.getDataString();
                    ClipData clipData = intent.getClipData();

                    if (clipData != null) { // Multiple files selected
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    } else if (dataString != null) { // Single file selected
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            // Send the result back to the WebView
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }

    // --- WebChromeClient for Progress, Title, and Console Logs ---

    private class CustomWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            // Update the title bar
            webTitle.setText(title != null ? title : "Web Runtime");
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            // Capture console messages (log, error, warning, debug, info) and display them
            String log = String.format("%s: %s (Line: %d, Source: %s)",
                    consoleMessage.messageLevel(),
                    consoleMessage.message(),
                    consoleMessage.lineNumber(),
                    consoleMessage.sourceId());

            // Format log type for color coding in the adapter
            String displayLog;
            switch (consoleMessage.messageLevel()) {
                case ERROR:
                    displayLog = "ERROR: " + log;
                    break;
                case WARNING:
                    displayLog = "WARNING: " + log;
                    break;
                default:
                    displayLog = "LOG: " + log;
                    break;
            }

            consoleLogs.add(displayLog);
            consoleAdapter.notifyDataSetChanged();

            // Automatically scroll to the bottom of the log
            consoleListView.setSelection(consoleAdapter.getCount() - 1);

            return true; // Return true to indicate we have handled the message
        }
        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);        if (icon != null) {
                Glide.with(CompilerActivity.this)
                        .load(icon)
                        .into(webIcon);
            }
        }
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            // 1. Store the callback
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            // 2. Prepare the intent to select content
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*"); // Allows any file type
            // Optional: If multiple files are allowed
            if (fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }

            // 3. Launch the activity
            try {
                startActivityForResult(contentSelectionIntent, FILECHOOSER_REQUEST_CODE);
            } catch (Exception e) {
                mFilePathCallback = null;
                Toast.makeText(CompilerActivity.this, "Cannot open file chooser.", Toast.LENGTH_LONG).show();
                return false;
            }

            return true; // Return true to handle the file choosing yourself
        }
    }
}
