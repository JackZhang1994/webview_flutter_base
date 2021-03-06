package io.flutter.plugins.webviewflutter;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.ValueCallback;

import androidx.core.content.ContextCompat;

import static io.flutter.plugins.webviewflutter.Constants.*;

public class FileChooserLauncher extends BroadcastReceiver {

    private Context activity;
    private String title;
    private String type;
    private boolean showCameraOption;
    private ValueCallback<Uri[]> filePathCallback;

    public FileChooserLauncher(Context context, String title, String type, boolean showCameraOption, ValueCallback<Uri[]> filePathCallback) {
        //this.activity = getActivityByContext(context);
        this.activity = context;
        this.title = title;
        this.type = type;
        this.showCameraOption = showCameraOption;
        this.filePathCallback = filePathCallback;
    }

    ///类型不充分并最终导致空指针异常
    private Activity getActivityByContext(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof Activity){
            return (Activity)context;
        } else if (context instanceof ContextWrapper){
            return getActivityByContext(((ContextWrapper)context).getBaseContext());
        }
        return null;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void start() {
        if (!showCameraOption || hasCameraPermission()) {
            showFileChooser();
        } else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_REQUEST_CAMERA_PERMISSION_FINISHED);
            activity.registerReceiver(this, intentFilter);

            activity.startActivity(new Intent(activity, RequestCameraPermissionActivity.class));
        }
    }

    private void showFileChooser() {
        IntentFilter intentFilter = new IntentFilter(ACTION_FILE_CHOOSER_FINISHED);
        activity.registerReceiver(this, intentFilter);

        Intent intent = new Intent(activity, FileChooserActivity.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_TYPE, type);
        intent.putExtra(EXTRA_SHOW_CAMERA_OPTION, showCameraOption && hasCameraPermission());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
        activity.startActivity(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_REQUEST_CAMERA_PERMISSION_FINISHED)) {
            activity.unregisterReceiver(this);
            showFileChooser();
        } else if (intent.getAction().equals(ACTION_FILE_CHOOSER_FINISHED)) {
            String uriString = intent.getStringExtra(EXTRA_FILE_URI);
            Uri[] result = uriString != null ? new Uri[]{Uri.parse(uriString)} : null;
            filePathCallback.onReceiveValue(result);
            activity.unregisterReceiver(this);
            filePathCallback = null;
        }
    }
}
