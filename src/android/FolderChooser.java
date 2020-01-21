package com.example.cordova.folderChooser;

import android.app.Activity;
import android.os.Environment;

import java.io.IOException;
import java.io.File;
import java.lang.Exception;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import java.util.ArrayList;

public class FolderChooser extends CordovaPlugin {
    private static final String ACTION_SAVE_FILE_TO_USB = "saveFileToUSB";

    private CallbackContext callback;

    private void chooseFile (CallbackContext callbackContext, String accept) {
        try {
            Context context = this.cordova.getActivity().getApplicationContext();
            String uri = "";

            for (File f : context.getExternalFilesDirs("")) {
                if (Environment.isExternalStorageRemovable(f)) {
                    uri = f.getAbsolutePath();
                    try {
                        new File(f.getAbsolutePath() + "/testFile.txt").createNewFile();
                    } catch (IOException err) {
                        this.callback.error("Execute create file is failed: " + err.toString());
                    }
                }
            }

            JSONObject result = new JSONObject();
            result.put("uri", uri);
            callbackContext.success(result);

        } catch (JSONException err) {
            this.callback.error("Execute failed: " + err.toString());
        }
    }

    @Override
    public boolean execute (
            String action,
            JSONArray args,
            CallbackContext callbackContext
    ) {
        try {
            if (action.equals(FolderChooser.ACTION_SAVE_FILE_TO_USB)) {
                this.chooseFile(callbackContext, args.getString(0));
                return true;
            }
        }
        catch (JSONException err) {
            this.callback.error("Execute failed: " + err.toString());
        }

        return false;
    }
}