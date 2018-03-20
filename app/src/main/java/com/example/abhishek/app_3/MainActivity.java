package com.example.abhishek.app_3;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.WriteAbortedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private Button btn;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    List<String[]> cntctArray = new ArrayList<String[]>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = (Button) findViewById(R.id.btn);

        checkAndRequestPermissions();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkAndRequestPermissions()) {

                   // expensive method call wrapped in observable.fromCallable()
                    Observable.fromCallable(new Callable<String>() {

                        @Override
                        public String call() throws Exception {
                            return readContacts();
                        }
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(res -> Snackbar.make(view, res+" Zipping Done....", Snackbar.LENGTH_LONG).show());

                }
                else
                    Toast.makeText(getApplicationContext() , "Reboot app and give permissions to CONTACT and STORAGE..", Toast.LENGTH_SHORT).show();
            }
        });


    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    private  boolean checkAndRequestPermissions() {
        int permissionSendMessage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    ////////////////////////////////////////////EXPENSIVE CALL/////////////////////////////////////////////////////////
    public String readContacts(){

            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

            while (cursor.moveToNext()) {

                String strA[] = new String[2];

                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                Cursor phoneCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                strA[0] = name;
                int cnt2 = 0;
                String str = "";
                while (phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    if (cnt2 > 0)
                        str += ", " + phoneNumber;
                    else
                        str = phoneNumber;
                    cnt2++;
                }

                strA[1] = str;
                cntctArray.add(strA);
            }

            return exportCSV();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    //write into csv
    private String exportCSV(){

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        File mediaStorageDir = new File(baseDir, "DataFiles");
        mediaStorageDir.mkdir();
        String fName = "Contacts.csv";
        String filePath = mediaStorageDir.getPath() + File.separator + fName;
        File f = new File(filePath);

        if(f.exists()){
            try {
                FileWriter mFileWriter = new FileWriter(filePath, true);
                CSVWriter writer = new CSVWriter(mFileWriter);
                writer.writeAll(cntctArray);
                writer.close();

            }catch(Exception e){
                Log.i("ERROR", e.getMessage());
            }
        }
        else{
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(filePath));
                writer.writeAll(cntctArray);
                writer.close();
            }catch(Exception e){
                Log.i("ERROR", e.getMessage()+"YAHI HAI");
            }
        }

        FileHelper.zip(mediaStorageDir.getPath()+File.separator, mediaStorageDir.getPath()+File.separator, "Contacts.zip", false);

        return mediaStorageDir.getPath();
    }



}
