package com.thegrizzlylabs.geniusscan.cordova;

import com.thegrizzlylabs.geniusscan.sdk.core.GeniusScanLibrary;
import com.thegrizzlylabs.geniusscan.cordova.model.Page;
import com.thegrizzlylabs.geniusscan.cordova.processing.BorderDetectionActivity;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;

import java.io.File;

/**
 * This class echoes a string called from JavaScript.
 */
public class GeniusScan extends CordovaPlugin {
    private static CordovaInterface cordovaInstance;

    private final int REQUEST_CODE = 42;
    private CallbackContext callback = null;
    private Page scanContainer = null;
    private String initializeError = null;


    public static int getResourceIdentifier(String name, String type) {
        Application app = cordovaInstance.getActivity().getApplication();
        String package_name = app.getPackageName();
        Resources resources = app.getResources();
        return resources.getIdentifier(name, type, package_name);
    }

    public static String getResourceString (String name, String type) {
        Application app = cordovaInstance.getActivity().getApplication();
        Resources resources = app.getResources();
        return resources.getString(GeniusScan.getResourceIdentifier(name, type));
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        GeniusScan.cordovaInstance = cordova;
        super.initialize(cordova, webView);

        Context context = cordova.getActivity().getApplicationContext();
        ApplicationInfo ai = null;
        String value = null;
        try {
            ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            value = ai.metaData.getString("GSSDK_LICENCE_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            initializeError = GeniusScan.getResourceString("internal_error", "string");
            return;
        }

        try {
            GeniusScanLibrary.init(cordova.getActivity().getApplicationContext(), value);
        } catch (RuntimeException e) {
            initializeError = e.getMessage();
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scan")) {
            callback = callbackContext;

            if (initializeError != null) {
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, initializeError);
                callback.sendPluginResult(result);
                return true;
            }

            Uri imageFileUri = Uri.parse(args.getString(0));
            String originalImageFilePath = imageFileUri.getPath();

            Context context = this.cordova.getActivity().getApplicationContext();
            scanContainer = new Page(context, callbackContext, originalImageFilePath);

            Intent intent = new Intent(context, BorderDetectionActivity.class);
            intent.putExtra(BorderDetectionActivity.EXTRA_PAGE, (Page) scanContainer);

            this.cordova.setActivityResultCallback((CordovaPlugin) this);
            this.cordova.startActivityForResult((CordovaPlugin) this, intent, REQUEST_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_CODE) {
            if( resultCode == Activity.RESULT_OK)
            {
                String enhancedImageFilePath = scanContainer.getEnhancedImage().getAbsolutePath(null);
                Uri enhancedImageFileUri = Uri.fromFile(new File(enhancedImageFilePath));
                PluginResult result = new PluginResult(PluginResult.Status.OK, enhancedImageFileUri.toString());
                callback.sendPluginResult(result);
            }
            else
            {
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, "error" );
                callback.sendPluginResult(result);
            }
        }
    }
}
