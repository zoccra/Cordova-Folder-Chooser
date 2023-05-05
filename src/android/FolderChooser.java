package com.example.cordova.folderChooser;


import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

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

import android.os.ParcelFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.io.FileNotFoundException;

import android.support.v4.provider.DocumentFile;

import java.io.FileInputStream;


import java.util.ArrayList;

public class FolderChooser extends CordovaPlugin {
    private static final String ACTION_SAVE_FILE_TO_USB = "saveFileToUSB";
    private static final String ACTION_GET_BACKUPS_FROM_USB = "getBackupsFromUSB";
    private static final String ACTION_MOVE_BACKUP_FROM_USB = "moveBackupFromUSB";
    private String inputFileName = null;

    private CallbackContext callback;

    private static final String ACTION_OPEN = "open";
    private static final int PICK_FOLDER_REQUEST = 1;
    private static final int CREATE_REQUEST_CODE = 2;

    @Override
    public boolean execute(
            String action,
            JSONArray args,
            CallbackContext callbackContext
    ) {
        try {
            if (action.equals(FolderChooser.ACTION_SAVE_FILE_TO_USB)) {
                this.chooseFile(callbackContext, args.getString(0));
                return true;
            } else if (action.equals(FolderChooser.ACTION_GET_BACKUPS_FROM_USB)) {
                this.getBackupsListByUri(callbackContext, args.getString(0));
                return true;
            } else if (action.equals(FolderChooser.ACTION_MOVE_BACKUP_FROM_USB)) {
                this.moveBackupFromUSB(callbackContext, args.getString(0), args.getString(1));
                return true;
            }
        } catch (JSONException err) {
            this.callback.error("Execute failed: " + err.toString());
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FolderChooser.PICK_FOLDER_REQUEST && this.callback != null) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    JSONObject result = new JSONObject();
                    Uri uri = data.getData();

                    String errorCopy = copyFile(this.inputFileName, uri);

                    result.put("error", errorCopy);
                    result.put("uri", uri);

                    this.callback.success(result);
                } catch (Exception err) {
                    this.callback.error("Failed to copy file: " + err.toString());
                }
            } else {
                this.callback.error("Folder URI was null.");
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            this.callback.success("RESULT_CANCELED");
        } else {
            this.callback.error(resultCode);
        }
    }

    private void chooseFile(CallbackContext callbackContext, String fileName) {
        this.inputFileName = fileName;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        Intent chooser = Intent.createChooser(intent, "Open folder");
        cordova.startActivityForResult(this, chooser, FolderChooser.PICK_FOLDER_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        this.callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    private void getBackupsListByUri(CallbackContext callbackContext, String uri) {
        try {
            JSONObject result = new JSONObject();
            DocumentFile backupsDir = DocumentFile.fromTreeUri(cordova.getActivity(), Uri.parse(uri));
            DocumentFile[] documents = backupsDir.listFiles();

            for (final DocumentFile file: documents) {
                JSONObject resultFile = new JSONObject();
                if (file.isFile()) {
                    resultFile.put("isFile", file.isFile());
                    resultFile.put("name", file.getName());
                    resultFile.put("url", file.getUri());
                    resultFile.put("type", file.getType());
                    result.put(file.getName(), resultFile);
                }
            }

            callbackContext.success(result);
        } catch (Exception err) {
            callbackContext.error("Failed to read file: " + err.toString());
        }
    }

    private static String getFileMimeType(String fileName) {
        String mimeType = "application/" + fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        return mimeType;
    }

    private void moveBackupFromUSB(CallbackContext callbackContext, String fileUri, String fileName) {
        InputStream in = null;
        OutputStream out = null;
        String error = null;
        String mimeType = getFileMimeType(fileName);

        String targetPath = cordova.getActivity().getApplicationContext().getExternalFilesDir(null).getAbsolutePath() + "/" + fileName;

        try {
            JSONObject result = new JSONObject();


            try {
                in = cordova.getActivity().getContentResolver().openInputStream(Uri.parse(fileUri));
                out = cordova.getActivity().getContentResolver().openOutputStream(Uri.fromFile(new File(targetPath)));

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.flush();
                out.close();


            } catch (FileNotFoundException fnfe1) {
                error = fnfe1.getMessage();
            } catch (Exception e) {
                error = e.getMessage();
            }
            result.put("error", error);
            result.put("fileName", fileName);
            result.put("fileUri", fileUri);
            result.put("url", targetPath);
            callbackContext.success(result);
        } catch (Exception err) {
            callbackContext.error("Failed to move file: " + err.toString());
        }
    }

    private String copyFile(String inputFile, Uri treeUri) {
        String inputPath = cordova.getActivity().getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        InputStream in = null;
        OutputStream out = null;
        String error = null;
        DocumentFile pickedDir = DocumentFile.fromTreeUri(cordova.getActivity(), treeUri);
        String mimeType = getFileMimeType(inputFile);

        try {
            DocumentFile newFile = pickedDir.createFile(mimeType, inputFile);
            out = cordova.getActivity().getContentResolver().openOutputStream(newFile.getUri());
            in = new FileInputStream(inputPath + "/" + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();

        } catch (FileNotFoundException fnfe1) {
            error = fnfe1.getMessage();
        } catch (Exception e) {
            error = e.getMessage();
        }

        return error;
    }
}