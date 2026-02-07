package luminous.organisation.webgen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.Map
import java.util.Random

/**
 * A utility object containing static helper methods for common Android tasks and data manipulation.
 */
object LuminousUtil {

    // Properties converted to nullable and private set for sUri
    private var sUri: Uri? = null
    private var isUri: Boolean = false

    // Constants for gravity
    const val TOP = 1
    const val CENTER = 2
    const val BOTTOM = 3

// --- Toast Utilities ---

    @JvmStatic
    fun CustomToast(
        context: Context,
        message: String,
        textColor: Int,
        textSize: Int,
        bgColor: Int,
        radius: Int,
        gravity: Int
    ) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        val view = toast.view
        val textView = view?.findViewById<TextView>(android.R.id.message)

        textView?.textSize = textSize.toFloat()
        textView?.setTextColor(textColor)
        textView?.gravity = Gravity.CENTER

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(bgColor)
        gradientDrawable.cornerRadius = radius.toFloat()
        view?.background = gradientDrawable
        view?.setPadding(15, 10, 15, 10)
        view?.elevation = 10f

        when (gravity) {
            TOP -> toast.setGravity(Gravity.TOP, 0, 150)
            CENTER -> toast.setGravity(Gravity.CENTER, 0, 0)
            BOTTOM -> toast.setGravity(Gravity.BOTTOM, 0, 150)
        }
        toast.show()
    }

    @JvmStatic
    fun CustomToastWithIcon(
        context: Context,
        message: String,
        textColor: Int,
        textSize: Int,
        bgColor: Int,
        radius: Int,
        gravity: Int,
        icon: Int
    ) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        val view = toast.view
        // Explicit cast is often necessary for findViewById, or use generics
        val textView = view?.findViewById<TextView>(android.R.id.message)

        textView?.textSize = textSize.toFloat()
        textView?.setTextColor(textColor)
        textView?.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        textView?.gravity = Gravity.CENTER
        textView?.compoundDrawablePadding = 10

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(bgColor)
        gradientDrawable.cornerRadius = radius.toFloat()
        view?.background = gradientDrawable
        view?.setPadding(10, 10, 10, 10)
        view?.elevation = 10f

        when (gravity) {
            TOP -> toast.setGravity(Gravity.TOP, 0, 150)
            CENTER -> toast.setGravity(Gravity.CENTER, 0, 0)
            BOTTOM -> toast.setGravity(Gravity.BOTTOM, 0, 150)
        }
        toast.show()
    }

// --- List & Map Utilities ---

    @JvmStatic
    fun sortListMap(
        listMap: ArrayList<HashMap<String, Any?>>,
        key: String,
        isNumber: Boolean,
        ascending: Boolean
    ) {
        // Using Kotlin's built-in sort function with a lambda
        Collections.sort(listMap, Comparator { compareMap1, compareMap2 ->
            val value1 = compareMap1[key]?.toString() ?: ""
            val value2 = compareMap2[key]?.toString() ?: ""

            if (isNumber) {
                val count1 = value1.toIntOrNull() ?: 0
                val count2 = value2.toIntOrNull() ?: 0

                // Simplified comparison logic
                if (ascending) {
                    count1.compareTo(count2)
                } else {
                    count2.compareTo(count1) // Reversed comparison for descending
                }

            } else {
                if (ascending) {
                    value1.compareTo(value2)
                } else {
                    value2.compareTo(value1)
                }
            }
        })
    }

    @JvmStatic
    fun getAllKeysFromMap(map: Map<String, Any?>?, output: ArrayList<String>?) {
        if (output == null) return
        output.clear()

        // The extension function 'isNullOrEmpty' is not universally available for java.util.Map.
        // Replaced with an explicit null check and standard 'isEmpty()'.
        if (map == null || map.isEmpty()) return

        // FIX: Using an explicit 'for-in' loop over map.keySet() to resolve the ambiguous iterator error.
        for (key in map.keySet()) {
            output.add(key)
        }
    }

// --- Image & File Utilities ---

    @JvmStatic
    fun CropImage(activity: Activity, path: String, requestCode: Int) {
        try {
            val intent = Intent("com.android.camera.action.CROP")
            val file = File(path)
            val contentUri = Uri.fromFile(file)
            intent.setDataAndType(contentUri, "image/*")
            intent.putExtra("crop", "true")
            intent.putExtra("aspectX", 1)
            intent.putExtra("aspectY", 1)
            intent.putExtra("outputX", 280)
            intent.putExtra("outputY", 280)
            intent.putExtra("return-data", false)
            activity.startActivityForResult(intent, requestCode)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                activity,
                "Your device doesn't support the crop action!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @JvmStatic
    fun copyFromInputStream(inputStream: InputStream): String {
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)

        try {
            var i: Int
            while (inputStream.read(buffer).also { i = it } != -1) {
                outputStream.write(buffer, 0, i)
            }
        } catch (e: IOException) {
            // Error handling can be added here
        } finally {
            try {
                outputStream.close()
                inputStream.close()
            } catch (e: IOException) {
                // Ignore secondary close errors
            }
        }
        // Use Charsets.UTF_8 for safer conversion if encoding is known
        return outputStream.toString()
    }

    @JvmStatic
    fun getUri(file: File, ctx: Context): Uri? {
        isUri = false
        MediaScannerConnection.scanFile(ctx, arrayOf(file.toString()), null) { _, uri ->
            sUri = uri
            if (sUri != null) {
                isUri = true
            }
        }
        // NOTE: This function is inherently problematic in Java/Kotlin
        // because scanFile is asynchronous. It returns before sUri is set.
        // For synchronous return, one must use a loop or, preferably,
        // refactor to use a callback/suspend function.
        // Maintaining original Java logic for direct translation:
        return if (isUri) sUri else null
    }

         // --- Network Utilities ---

    @JvmStatic
    fun isConnected(context: Context): Boolean {
        // Using Kotlin's getSystemService extension from androidx.core
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val activeNetworkInfo: NetworkInfo? = connectivityManager?.activeNetworkInfo
        // Simplified boolean check
        return activeNetworkInfo?.isConnected == true
    }

         // --- Keyboard & Clipboard Utilities ---

    @JvmStatic
    fun hideKeyboard(context: Context) {
        val inputMethodManager = context.getSystemService<InputMethodManager>()
        // FIX: When hiding, the showFlags (1st arg) should be 0, and hideFlags (2nd arg) should be a valid HIDE flag.
        inputMethodManager?.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    @JvmStatic
    fun showKeyboard(context: Context) {
        val inputMethodManager = context.getSystemService<InputMethodManager>()
        // FIX: Use SHOW_IMPLICIT to satisfy the compiler's strict check for showFlags (1st arg).
        // Use a valid hide flag (e.g., HIDE_NOT_ALWAYS) for the 2nd arg.
        inputMethodManager?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    @JvmStatic
    fun copyToClipboard(context: Context, clipData: String) {
        val clipboard = context.getSystemService<ClipboardManager>()
        val clip = ClipData.newPlainText("label", clipData)
        clipboard?.setPrimaryClip(clip)
    }

    // --- Display & View Utilities ---

    @JvmStatic
    fun getLocationX(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[0]
    }

    @JvmStatic
    fun getLocationY(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[1]
    }

    @JvmStatic
    fun getDisplayWidthPixels(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    @JvmStatic
    fun getDisplayHeightPixels(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    @JvmStatic
    fun getDip(context: Context, input: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            input.toFloat(),
            context.resources.displayMetrics
        )
    }

    @JvmStatic
    fun getRandom(min: Int, max: Int): Int {
        val random = Random()
        return random.nextInt(max - min + 1) + min
    }

// --- Message & ListView Utilities ---

    @JvmStatic
    fun showMessage(context: Context, s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun getCheckedItemPositionsToArray(list: ListView): ArrayList<Double> {
        val result = ArrayList<Double>()
        val arr: SparseBooleanArray = list.checkedItemPositions
        // FIX: Replaced 'arr.indices' with explicit range '0 until arr.size()' to resolve
        // the unresolved reference error due to missing index extensions for SparseBooleanArray.
        for (i in 0 until arr.size()) {
            if (arr.valueAt(i)) {
                result.add(arr.keyAt(i).toDouble())
            }
        }
        return result
    }
}
