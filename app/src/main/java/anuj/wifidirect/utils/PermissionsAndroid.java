/*
 *
 *
 *  * Copyright © 2016, Mobilyte Inc. and/or its affiliates. All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  *
 *  * - Redistributions of source code must retain the above copyright
 *  *    notice, this list of conditions and the following disclaimer.
 *  *
 *  * - Redistributions in binary form must reproduce the above copyright
 *  * notice, this list of conditions and the following disclaimer in the
 *  * documentation and/or other materials provided with the distribution.
 *
 * /
 */

package anuj.wifidirect.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;


/**
 * Created by Percy on 14/09/17.
 */
public class PermissionsAndroid {

    private static PermissionsAndroid permissionsAndroid;

    public static PermissionsAndroid getInstance() {
        if (permissionsAndroid == null)
            permissionsAndroid = new PermissionsAndroid();
        return permissionsAndroid;
    }

    private PermissionsAndroid() {

    }

    // Request Code for request Permissions Must be between 0 to 255.
    //Write External Storage Permission.
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 100;

    public boolean checkWriteExternalStoragePermission(Activity activity) {
        return boolValue(ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    public void requestForWriteExternalStoragePermission(Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(activity, "Allow Write External Storage Permission to use this functionality.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    // function to return true or false based on the permission result
    private boolean boolValue(int value) {
        return value == PackageManager.PERMISSION_GRANTED;
    }

}
