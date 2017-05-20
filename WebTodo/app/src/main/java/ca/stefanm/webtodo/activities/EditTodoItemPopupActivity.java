package ca.stefanm.webtodo.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import ca.stefanm.webtodo.R;
import ca.stefanm.webtodo.StorageController;
import ca.stefanm.webtodo.ToDoListGPS;
import ca.stefanm.webtodo.localstorage.Session;
import ca.stefanm.webtodo.models.TodoItem;
import hugo.weaving.DebugLog;
import kotlin.Unit;

/**
 * Created by Stefan on 3/5/2017.
 */

public class EditTodoItemPopupActivity extends AppCompatActivity {

    public static final String TAG = "EditTodoPopupActivity";

    Context mContext = null;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_todo_item_popup);
        ButterKnife.bind(this);

        mContext = this;

        int id = getIntent().getIntExtra("itemid", -1);
        if (id != -1) {
            new LoadItemTask(id).execute();
        } else {
            currentlyEditedTodoItem = new TodoItem(0, false, "", 0, new Session(this).getCurrentUser(), 0L, 0L);
        }
    }

    TodoItem currentlyEditedTodoItem = null;


    @BindView(R.id.et_editTodoItem)
    EditText edit_item_editView;


    @BindView(R.id.cb_completed)
    CheckBox completed;

    @OnClick(R.id.btn_save)
    protected void saveClick(){
        new SaveItemTask().execute();
    }

    @OnCheckedChanged(R.id.cb_completed)
    protected void setCompleted(boolean checked) {
        currentlyEditedTodoItem.setCompleted(checked);
    }

    @OnClick(R.id.btn_delete)
    protected void deleteItem(){
        new DeleteItemTask().execute();
    }

    @OnClick(R.id.btn_get_location)
    protected void getLocation(){
        if(checkLocationPerms()){
            ToDoListGPS GPS = new ToDoListGPS(this);
            try{
                currentlyEditedTodoItem.setGeoLat(GPS.getCurrentLocation().getLatitude());
                currentlyEditedTodoItem.setGeoLng(GPS.getCurrentLocation().getLongitude());
                Toast.makeText(mContext, "Location\nLat: " + GPS.getCurrentLocation().getLatitude() + "\nLng: " + GPS.getCurrentLocation().getLongitude(), Toast.LENGTH_SHORT).show();
            }
            catch (NullPointerException e){
                return;
            }
        }
    }

    private boolean checkLocationPerms(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            int PERMISSION_ALL = 1;
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();

        } else {
            checkLocationPerms();
        }
        return;
    }

    //TODO On editor defocus update model.
    @OnTextChanged(R.id.et_editTodoItem)
    @DebugLog
    void updateModelFromEditor(){

        if (currentlyEditedTodoItem != null) {
            currentlyEditedTodoItem.setContents(edit_item_editView.getText().toString());
        }
    }

    private class LoadItemTask extends AsyncTask<Void, Void, StorageController.StorageControllerResult<TodoItem>> {

        ProgressDialog loadingDialog = null;

        int mId = 0;

        LoadItemTask(int id){
            mId = id;
        }

        @Override
        protected void onPreExecute() {
            loadingDialog = ProgressDialog.show(mContext, "Loading", "Fetching TODO Item...");
        }

        @Override
        protected StorageController.StorageControllerResult<TodoItem> doInBackground(Void... params){
            int id = mId;
            Log.d(TAG, "Id: " + id);

            StorageController.StorageControllerResult<TodoItem> fetchResult =
                    StorageController.INSTANCE.getTodoItemById(mContext, id);
            Log.d(TAG, "Fetch result: " + fetchResult);
            return fetchResult;

        }

        @Override
        protected void onPostExecute(StorageController.StorageControllerResult<TodoItem> fetchResult) {

            if (fetchResult.getSuccess() && fetchResult.getData() != null){
                currentlyEditedTodoItem = fetchResult.getData();
                edit_item_editView.setText(currentlyEditedTodoItem.getContents());
                completed.setChecked(currentlyEditedTodoItem.getCompleted());

                loadingDialog.dismiss();
            } else {
                loadingDialog.dismiss();
                new AlertDialog.Builder(mContext)
                    .setMessage("Loading item failed.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    private class DeleteItemTask extends AsyncTask<Void, Void, StorageController.StorageControllerResult<Unit>>{
        ProgressDialog loadingDialog = null;

        @Override
        protected void onPreExecute() {
            loadingDialog = ProgressDialog.show(mContext, "Loading", "Fetching TODO Item...");
        }

        @Override
        protected StorageController.StorageControllerResult<Unit> doInBackground(Void... params){
            StorageController.StorageControllerResult<Unit> fetchResult =
                    StorageController.INSTANCE.deleteTodoItem(mContext, currentlyEditedTodoItem);

            return fetchResult;
        }

        @Override
        protected void onPostExecute(StorageController.StorageControllerResult<Unit> fetchResult) {

            loadingDialog.dismiss();

            if (!fetchResult.getSuccess() && fetchResult.getData() != null){
                new AlertDialog.Builder(mContext)
                        .setMessage("Deleting item failed.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .create()
                        .show();
            }
            finish();
        }
    }


    private class SaveItemTask extends AsyncTask<Void, Void, StorageController.StorageControllerResult<Unit>> {

        ProgressDialog loadingDialog = null;

        @Override
        protected void onPreExecute() {
            loadingDialog = ProgressDialog.show(EditTodoItemPopupActivity.this, "Loading", "Saving TODO Item...");
        }

        @Override
        protected StorageController.StorageControllerResult<Unit> doInBackground(Void... params){

            if (currentlyEditedTodoItem.getId() == 0){
                return StorageController.INSTANCE.createTodoItem(mContext, currentlyEditedTodoItem);
            } else {
                return StorageController.INSTANCE.updateTodoItem(mContext, currentlyEditedTodoItem);
            }

        }

        @Override
        protected void onPostExecute(StorageController.StorageControllerResult<Unit> result) {

            loadingDialog.dismiss();

            if (!result.getSuccess() && result.getData() != null){
                new AlertDialog.Builder(mContext)
                        .setMessage("Updating/Creating item failed.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .create()
                        .show();
            } else {
                finish();
            }
        }

    }

}
