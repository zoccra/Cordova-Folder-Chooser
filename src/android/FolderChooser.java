package com.example.cordova.folderChooser;

import android.content.Intent;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.lang.Exception;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FolderChooser extends CordovaPlugin {
    private String currentFolderId = "0";

    private static final String ACTION_OPEN = "open";
    private static final int PICK_FOLDER_REQUEST = 1;
    private static final String TAG = "Chooser";

    /** @see https://stackoverflow.com/a/17861016/459881 */
    public static byte[] getBytesFromInputStream (InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];

        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }

        return os.toByteArray();
    }

    /** @see https://stackoverflow.com/a/23270545/459881 */
    public static String getDisplayName (ContentResolver contentResolver, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor metaCursor = contentResolver.query(uri, projection, null, null, null);

        if (metaCursor != null) {
            try {
                if (metaCursor.moveToFirst()) {
                    return metaCursor.getString(0);
                }
            } finally {
                metaCursor.close();
            }
        }

        return "File";
    }


    private CallbackContext callback;

    private void chooseFile (CallbackContext callbackContext, String accept) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//        intent.setType("*/*");
//        if (!accept.equals("*/*")) {
//            intent.putExtra(Intent.EXTRA_MIME_TYPES, accept.split(","));
//        }
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
//        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        Intent chooser = Intent.createChooser(intent, "Open folder");
        cordova.startActivityForResult(this, chooser, FolderChooser.PICK_FOLDER_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        this.callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public boolean execute (
            String action,
            JSONArray args,
            CallbackContext callbackContext
    ) {
        try {
            if (action.equals(FolderChooser.ACTION_OPEN)) {
                this.chooseFile(callbackContext, args.getString(0));
                return true;
            }
        }
        catch (JSONException err) {
            this.callback.error("Execute failed: " + err.toString());
        }

        return false;
    }


    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == FolderChooser.PICK_FOLDER_REQUEST && this.callback != null) {
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    File path = Environment.getExternalStorageState();

                    if (uri != null) {
//                        ContentResolver contentResolver =
//                                this.cordova.getActivity().getContentResolver()
//                                ;
//
//                        String name = FolderChooser.getDisplayName(contentResolver, uri);
//
//                        String mediaType = contentResolver.getType(uri);
//                        if (mediaType == null || mediaType.isEmpty()) {
//                            mediaType = "application/octet-stream";
//                        }
//
//                        byte[] bytes = FolderChooser.getBytesFromInputStream(
//                                contentResolver.openInputStream(uri)
//                        );
//
//                        String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
//
                        JSONObject result = new JSONObject();

//                        result.put("data", base64);
//                        result.put("mediaType", mediaType);
//                        result.put("name", name);
                        result.put("uri", path);

                        this.callback.success(result);
                    }
                    else {
                        this.callback.error("Folder URI was null.");
                    }
                }
                else if (resultCode == Activity.RESULT_CANCELED) {
                    this.callback.success("RESULT_CANCELED");
                }
                else {
                    this.callback.error(resultCode);
                }
            }
        }
        catch (Exception err) {
            this.callback.error("Failed to read folder: " + err.toString());
        }
    }
}