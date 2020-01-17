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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.Exception;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.annotation.SuppressLint;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

class RealPathUtil {
    @TargetApi(Build.VERSION_CODES.KITKAT)
    static String getRealPathFromURI(final Context context, final Uri uri) throws IOException {

        final boolean isKitKat = Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else {
                    final int splitIndex = docId.indexOf(':', 1);
                    final String tag = docId.substring(0, splitIndex);
                    final String path = docId.substring(splitIndex + 1);

                    String nonPrimaryVolume = getPathToNonPrimaryVolume(context, tag);
                    if (nonPrimaryVolume != null) {
                        String result = nonPrimaryVolume + "/" + path;
                        File file = new File(result);
                        if (file.exists() && file.canRead()) {
                            return result;
                        }
                        return null;
                    }
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * If an image/video has been selected from a cloud storage, this method
     * should be call to download the file in the cache folder.
     *
     * @param context The context
     * @param fileName donwloaded file's name
     * @param uri file's URI
     * @return file that has been written
     */
    private static File writeToFile(Context context, String fileName, Uri uri) {
        String tmpDir = context.getCacheDir() + "/react-native-image-crop-picker";
        Boolean created = new File(tmpDir).mkdir();
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        File path = new File(tmpDir);
        File file = new File(path, fileName);
        try {
            FileOutputStream oos = new FileOutputStream(file);
            byte[] buf = new byte[8192];
            InputStream is = context.getContentResolver().openInputStream(uri);
            int c = 0;
            while ((c = is.read(buf, 0, buf.length)) > 0) {
                oos.write(buf, 0, c);
                oos.flush();
            }
            oos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String[] projection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                // Fall back to writing to file if _data column does not exist
                final int index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                String path = index > -1 ? cursor.getString(index) : null;
                if (path != null) {
                    return cursor.getString(index);
                } else {
                    final int indexDisplayName = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                    String fileName = cursor.getString(indexDisplayName);
                    File fileWritten = writeToFile(context, fileName, uri);
                    return fileWritten.getAbsolutePath();
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getPathToNonPrimaryVolume(Context context, String tag) {
        File[] volumes = context.getExternalCacheDirs();
        if (volumes != null) {
            for (File volume : volumes) {
                if (volume != null) {
                    String path = volume.getAbsolutePath();
                    if (path != null) {
                        int index = path.indexOf(tag);
                        if (index != -1) {
                            return path.substring(0, index) + tag;
                        }
                    }
                }
            }
        }
        return null;
    }

}

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
                        result.put("uri", getRealPath(this, uri));

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