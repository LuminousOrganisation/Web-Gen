package luminous.organisation.webgen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;

public class ConsoleActivity extends AppCompatActivity {

    private String destProjectKey; // Holds the DEST value
    private WebView codeEditorWebView;

    // Key used to retrieve the DEST value from the intent
    private static final String DEST_INTENT_KEY = "DEST_PROJECT_KEY";
    // Key used to store/retrieve code in SharedPreferences
    private static final String CODE_PREF_KEY = "code";

    // Variable to temporarily hold the code retrieved from JavaScript
    private String retrievedCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        codeEditorWebView = findViewById(R.id.webview_code_editor);
        ImageView saveIcon = findViewById(R.id.icon_save);
        ImageView runIcon = findViewById(R.id.icon_run);

        // 1. Retrieve the project key (DEST) from the intent
        Intent intent = getIntent();
        destProjectKey = intent.getStringExtra(DEST_INTENT_KEY);

        if (destProjectKey == null || destProjectKey.isEmpty()) {
            Toast.makeText(this, "Error: Project key not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. Load the code from SharedPreferences
        String storedCode = loadCodeFromPreferences();

        // 3. Setup WebView (Ace Editor) and inject content
        setupAceEditor(storedCode);

        // 4. Set up click listeners
        saveIcon.setOnClickListener(v -> saveCodeToPreferences());
        runIcon.setOnClickListener(v -> runCode());
    }

    /**
     * Loads the stored code from the project's SharedPreferences.
     */
    private String loadCodeFromPreferences() {
        // The SharedPreferences file is named after the DEST key
        SharedPreferences projectPrefs = getSharedPreferences(destProjectKey, Context.MODE_PRIVATE);

        // Load the string stored under the "code" key
        String storedCode = projectPrefs.getString(CODE_PREF_KEY, "");

        Toast.makeText(this, "Project '" + destProjectKey + "' loaded.", Toast.LENGTH_SHORT).show();
        return storedCode;
    }

    /**
     * Sets up the WebView to run the Ace Code Editor with syntax highlighting.
     */
    private void setupAceEditor(String initialCode) {
        WebSettings webSettings = codeEditorWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // IMPORTANT: Add the JavaScript interface to allow JS to call Java methods
        codeEditorWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        // Create the full HTML document containing Ace Editor code (loaded from CDN)
        String htmlContent = generateAceEditorHtml(initialCode);

        // Load the HTML content into the WebView
        // We use "about:blank" as the base URL to prevent file access issues
        codeEditorWebView.loadDataWithBaseURL("about:blank", htmlContent, "text/html", "UTF-8", null);

        codeEditorWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // After the page loads, ensure the initial code is set in the editor
                // Note: This is already handled by the injected HTML, but good for stability
                // Toast.makeText(ConsoleActivity.this, "Editor ready.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Generates the complete HTML for the Ace Editor, including CDN links and JS setup.
     */
    private String generateAceEditorHtml(String initialCode) {
        // We must escape quotes and backslashes in the initial code string
        String escapedCode;
        try {
            escapedCode = URLEncoder.encode(initialCode, "UTF-8").replaceAll("\\+", " ");
        } catch (Exception e) {
            escapedCode = "";
            e.printStackTrace();
        }

        // The HTML structure for the Ace Editor
        return "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<script src='https://cdnjs.cloudflare.com/ajax/libs/ace/1.33.0/ace.js'></script>" +
                "<style>" +
                "body, html { margin: 0; padding: 0; height: 100%; width: 100%; }" +
                "#editor { width: 100%; height: 100%; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div id='editor'></div>" +
                "<script>" +
                "var editor = ace.edit('editor');" +
                "editor.setTheme('ace/theme/monokai');" + // VS Code-like dark theme
                "editor.session.setMode('ace/mode/html');" + // Set mode for HTML/CSS/JS mixture
                "editor.setOptions({ " +
                "enableBasicAutocompletion: true, " +
                "enableSnippets: true, " +
                "enableLiveAutocompletion: true, " +
                "fontSize: '9pt'," +
                "showPrintMargin: true" +
                "});" +
                "var initialCode = decodeURIComponent('" + escapedCode + "');" +
                "editor.session.setValue(initialCode);" +
                "editor.gotoLine(1);" + // Ensure the cursor starts at the top

                // Function called by Java to retrieve content
                "function getEditorContent() {" +
                "   var content = editor.session.getValue();" +
                "   Android.setCodeContent(content);" + // Call Java method
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
    }


    private void getCodeFromJavaScript(int actionType) {
        retrievedCode = null;
        codeEditorWebView.evaluateJavascript("getEditorContent();", null);

        long startTime = System.currentTimeMillis();
        while (retrievedCode == null && System.currentTimeMillis() - startTime < 1000) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (retrievedCode != null) {
            if (actionType == 0) {
                saveRetrievedCode(retrievedCode);
            } else if (actionType == 1) {
                runRetrievedCode(retrievedCode);
            }
        } else {
            Toast.makeText(this, "Editor failed to load content. Try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Saves the content received from the JavaScript editor.
     */
    private void saveCodeToPreferences() {
        // Trigger JavaScript to retrieve content (actionType: 0 for Save)
        getCodeFromJavaScript(0);
    }

    private void saveRetrievedCode(String code) {
        // Save the current code back to the project's SharedPreferences
        SharedPreferences projectPrefs = getSharedPreferences(destProjectKey, Context.MODE_PRIVATE);
        projectPrefs.edit().putString(CODE_PREF_KEY, code).apply();

        Toast.makeText(this, "Code saved successfully!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Starts the CompilerActivity, passing the CURRENT editor content.
     */
    private void runCode() {
        // Trigger JavaScript to retrieve content (actionType: 1 for Run)
        getCodeFromJavaScript(1);
    }

    private void runRetrievedCode(String code) {
        Intent compilerIntent = new Intent(ConsoleActivity.this, CompilerActivity.class);

        // Send the entire code string to the CompilerActivity
        compilerIntent.putExtra(CODE_PREF_KEY, code);

        // We also send the project key
        compilerIntent.putExtra(DEST_INTENT_KEY, destProjectKey);

        startActivity(compilerIntent);
    }

    /**
     * JavaScript Interface: Bridge class for JavaScript to call Java methods.
     */
    private class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        // This method is called from JavaScript (getEditorContent function)
        @JavascriptInterface
        public void setCodeContent(String content) {
            // Set the retrieved code content and notify the waiting Java thread
            retrievedCode = content;
        }
    }
}
