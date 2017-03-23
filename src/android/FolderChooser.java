package com.example.cordova.folderChooser;
 
import org.json.JSONArray;
import org.json.JSONException;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import android.R;
import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import android.widget.ArrayAdapter;

public class FolderChooser extends CordovaPlugin {
    private boolean m_isNewFolderEnabled = true;
    private String m_sdcardDirectory = "";
    private Context m_context;
    private TextView m_titleView;
    private String m_chosenDir = "";
    private String chosenDir = "";
    private String m_dir = "";
    private List<String> m_subdirs = null;
    private ArrayAdapter<String> m_listAdapter = null;
    private CallbackContext c = null;
    public boolean execute(String action, JSONArray args,final CallbackContext callbackContext) throws JSONException {
        if ("open".equals(action)){
            this.c = callbackContext;
            final String path = args.getString(0);
            this.open(path);
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            this.c.sendPluginResult(result);
            return true;
        }else{
            callbackContext.error("not found :"+action);
            return false;
        }
    }
    public void open(String path){
        this.m_chosenDir = path;
        boolean m_newFolderEnabled = true;
                // Create DirectoryChooserDialog and register a callback 
                DirectoryChooserDialog directoryChooserDialog = 
                new DirectoryChooserDialog(this.cordova.getActivity(), 
                    new DirectoryChooserDialog.ChosenDirectoryListener() 
                {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
                    @Override
                    public void onChosenDir(String chosenDir) 
                    {
                        PluginResult result = new PluginResult(PluginResult.Status.OK, chosenDir);
                        FolderChooser.this.setM_chosenDir(chosenDir);
                        Toast.makeText(
                        FolderChooser.this.cordova.getActivity().getApplicationContext(), "Chosen directory: " + 
                          chosenDir, Toast.LENGTH_LONG).show();
                        result.setKeepCallback(false);
                        if (FolderChooser.this.getC() != null) {
                            FolderChooser.this.getC().sendPluginResult(result);
                            FolderChooser.this.setC(null);
                        }
                    }
                }); 
                // Toggle new folder button enabling
                directoryChooserDialog.setNewFolderEnabled(m_newFolderEnabled);
                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.                               
                directoryChooserDialog.chooseDirectory(m_chosenDir);
                m_newFolderEnabled = ! m_newFolderEnabled;
    }
    public void setM_chosenDir(String p){
        this.m_chosenDir = p;
    }
    public String getM_chosenDir(){
        return this.m_chosenDir;
    }
     public void setChosenDir(String p){
        this.chosenDir = p;
    }
    public String getChosenDir(){
        return this.chosenDir;
    }
    public CallbackContext getC(){
        return this.c;
    }
    public void setC(CallbackContext c){
        this.c = c;
    }



}