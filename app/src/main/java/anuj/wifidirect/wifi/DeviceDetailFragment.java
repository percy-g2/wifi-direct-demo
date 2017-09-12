/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package anuj.wifidirect.wifi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.kbeanie.multipicker.api.FilePicker;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.callbacks.FilePickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import anuj.wifidirect.R;
import anuj.wifidirect.utils.PermissionsAndroid;
import anuj.wifidirect.utils.SharedPreferencesHandler;
import anuj.wifidirect.utils.Utils;
import anuj.wifidirect.wifi.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends android.support.v4.app.Fragment implements ConnectionInfoListener, FilePickerCallback {

    static InterstitialAd mInterstitialAd;

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    private static ProgressDialog mProgressDialog;

    public static String WiFiServerIp = "";
    public static String WiFiClientIp = "";
    static Boolean ClientCheck = false;
    public static String GroupOwnerAddress = "";
    static long ActualFilelength = 0;
    static int Percentage = 0;
    public static String FolderName = "WiFiDirectDemo";

    private FilePicker filePicker;
    private String pickerPath;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("picker_path")) {
                pickerPath = savedInstanceState.getString("picker_path");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getString(R.string.fullscreen_ad_unit_id));
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });

        requestNewInterstitial();

        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                if (config != null && config.deviceAddress != null && device != null) {
                    config.deviceAddress = device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                            "Connecting to :" + device.deviceAddress, true, true
                    );
                    ((DeviceActionListener) getActivity()).connect(config);
                } else {

                }
            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                        String Ip = SharedPreferencesHandler.getStringValues(
                                getActivity(), getString(R.string.pref_WiFiClientIp));
                        String OwnerIp = SharedPreferencesHandler.getStringValues(
                                getActivity(), getString(R.string.pref_GroupOwnerAddress));
                        String ServerBool = SharedPreferencesHandler.getStringValues(getActivity(), getString(R.string.pref_ServerBoolean));
                        if (!TextUtils.isEmpty(ServerBool) && ServerBool.equalsIgnoreCase("true") && !TextUtils.isEmpty(Ip)) {
                            serviceIntent
                                    .putExtra(
                                            FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                            Ip);

                        } else {
                            FileTransferService.PORT = 8888;
                            serviceIntent.putExtra(
                                    FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                    OwnerIp);
                        }

                        getActivity().startService(serviceIntent);
                    }
                });

        return mContentView;
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    private void checkExternalStoragePermission() {
        boolean isExternalStorage = PermissionsAndroid.getInstance().checkWriteExternalStoragePermission(getActivity());
        if (!isExternalStorage) {
            PermissionsAndroid.getInstance().requestForWriteExternalStoragePermission(getActivity());
        } else {
            pickFilesSingle();
        }
    }

    private void pickFilesSingle() {
        filePicker = new FilePicker(getActivity());
        filePicker.setFilePickerCallback(this);
        filePicker.setFolderName(getActivity().getString(R.string.app_name));
        filePicker.pickFile();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == Picker.PICK_FILE) {
                filePicker.submit(data);
            }
        } else {
            CommonMethods.DisplayToast(getActivity(), "Cancelled Request");
        }
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        if (info.groupOwnerAddress.getHostAddress() != null)
            view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        else {
            CommonMethods.DisplayToast(getActivity(), "Host Address not found");
        }
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        try {
            String GroupOwner = info.groupOwnerAddress.getHostAddress();
            if (GroupOwner != null && !GroupOwner.equals(""))
                SharedPreferencesHandler.setStringValues(getActivity(),
                        getString(R.string.pref_GroupOwnerAddress), GroupOwner);
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);

            //first check for file storage permission
            if (!PermissionsAndroid.getInstance().checkWriteExternalStoragePermission(getActivity())) {
                Utils.getInstance().showToast("Please enable storage Permission from application storage option");
                return;
            }

            if (info.groupFormed && info.isGroupOwner) {
            /*
             * set shaerdprefrence which remember that device is server.
        	 */
                SharedPreferencesHandler.setStringValues(getActivity(),
                        getString(R.string.pref_ServerBoolean), "true");

                FileServerAsyncTask FileServerobj = new FileServerAsyncTask(
                        getActivity(), FileTransferService.PORT);
                if (FileServerobj != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        FileServerobj.executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);
                        // FileServerobj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,Void);
                    } else
                        FileServerobj.execute();
                }
            } else {
                // The other device acts as the client. In this case, we enable the
                // get file button.
                if (!ClientCheck) {
                    firstConnectionMessage firstObj = new firstConnectionMessage(
                            GroupOwnerAddress);
                    if (firstObj != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            firstObj.executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR);
                        } else
                            firstObj.execute();
                    }
                }

                FileServerAsyncTask FileServerobj = new FileServerAsyncTask(
                        getActivity(), FileTransferService.PORT);
                if (FileServerobj != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        FileServerobj.executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR);
                    } else
                        FileServerobj.execute();

                }

            }
        } catch (Exception e) {

        }
    }


    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
        /*
         * Remove All the prefrences here
         */
        SharedPreferencesHandler.setStringValues(getActivity(),
                getString(R.string.pref_GroupOwnerAddress), "");
        SharedPreferencesHandler.setStringValues(getActivity(),
                getString(R.string.pref_ServerBoolean), "");
        SharedPreferencesHandler.setStringValues(getActivity(),
                getString(R.string.pref_WiFiClientIp), "");

        ClientCheck = false;
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    static Handler handler;

    @Override
    public void onFilesChosen(List<ChosenFile> list) {
        ChosenFile file = list.get(0);
        String extension = "";
        int i = list.get(0).getDisplayName().lastIndexOf('.');
        if (i > 0) {
            extension = list.get(0).getDisplayName().substring(i + 1);
        }

        ActualFilelength = file.getSize();
        ;

       // TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
       // statusText.setText("Sending: " + file.getOriginalPath());

        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, file.getQueryUri());
                /*
                 * Choose on which device file has to send weather its server or client
    	         */
        String Ip = SharedPreferencesHandler.getStringValues(
                getActivity(), getString(R.string.pref_WiFiClientIp));
        String OwnerIp = SharedPreferencesHandler.getStringValues(
                getActivity(), getString(R.string.pref_GroupOwnerAddress));
        if (!TextUtils.isEmpty(OwnerIp) && OwnerIp.length() > 0) {
            String host = null;
            int sub_port = -1;

            String ServerBool = SharedPreferencesHandler.getStringValues(getActivity(), getString(R.string.pref_ServerBoolean));
            if (!TextUtils.isEmpty(ServerBool) && ServerBool.equalsIgnoreCase("true") && !TextUtils.isEmpty(Ip)) {
                host = Ip;
                sub_port = FileTransferService.PORT;
                serviceIntent
                        .putExtra(
                                FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                Ip);

            } else {
                FileTransferService.PORT = 8888;
                host = OwnerIp;
                sub_port = FileTransferService.PORT;
                serviceIntent.putExtra(
                        FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        OwnerIp);
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, FileTransferService.PORT);

            serviceIntent.putExtra(FileTransferService.Extension, file.getDisplayName());

            serviceIntent.putExtra(FileTransferService.Filelength,
                    String.valueOf(ActualFilelength));

           /* if (sub_port != -1) {
                showprogress("Sending...");
                getActivity().startService(serviceIntent);
            } else {
                CommonMethods.DisplayToast(getActivity(),
                        "Host Address not found, Please Re-Connect");
                DismissProgressDialog();
            }*/

        } else {
            /*DismissProgressDialog();
            CommonMethods.DisplayToast(getActivity(),
                    "Host Address not found, Please Re-Connect");*/
        }
    }

    @Override
    public void onError(String s) {

    }

    private static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        //        private TextView statusText;
        private Context mFilecontext;
        private int PORT;

        /**
         * @param context
         * @param port
         */
        FileServerAsyncTask(Context context, int port) {
            this.mFilecontext = context;
            handler = new Handler();
            this.PORT = port;
            if (mProgressDialog == null)
                mProgressDialog = new ProgressDialog(mFilecontext,
                        ProgressDialog.THEME_HOLO_LIGHT);
        }


        @Override
        protected String doInBackground(Void... params) {
            ServerSocket serverSocket = null;
            Socket client = null;
            DataInputStream inputstream = null;
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(8888));
                client = serverSocket.accept();
                inputstream = new DataInputStream(client.getInputStream());
                String str = inputstream.readUTF();
                WiFiClientIp = client.getInetAddress().getHostAddress();
                SharedPreferencesHandler.setStringValues(mFilecontext,
                        mFilecontext.getString(R.string.pref_WiFiClientIp), WiFiClientIp);
                serverSocket.close();
                return str;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if(inputstream != null){
                    try{
                        inputstream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(client != null){
                    try{
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(serverSocket != null){
                    try{
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(mFilecontext, result, Toast.LENGTH_SHORT).show();
                FileServerAsyncTask FileServerobj = new
                        FileServerAsyncTask(mFilecontext, FileTransferService.PORT);
                FileServerobj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(mFilecontext);
            }
        }
    }



    public void showprogress(final String task) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity(),
                    ProgressDialog.THEME_HOLO_LIGHT);
        }
        Handler handle = new Handler();
        final Runnable send = new Runnable() {

            public void run() {
                // TODO Auto-generated method stub
                mProgressDialog.setMessage(task);
                // mProgressDialog.setProgressNumberFormat(null);
                // mProgressDialog.setProgressPercentFormat(null);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(100);
//				mProgressDialog.setCancelable(false);
                mProgressDialog.setProgressNumberFormat(null);
                mProgressDialog
                        .setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
            }
        };
        handle.post(send);
    }

    public static void DismissProgressDialog() {
        try {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }


    /*
     * Async class that has to be called when connection establish first time. Its main motive is to send blank message
     * to server so that server knows the IP address of client to send files Bi-Directional.
     */
    private class firstConnectionMessage extends AsyncTask<String, Void, String> {

        String GroupOwnerAddress = "";

        firstConnectionMessage(String owner) {
            // TODO Auto-generated constructor stub
            this.GroupOwnerAddress = owner;

        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            CommonMethods.e("On first Connect", "On first Connect");

            Intent serviceIntent = new Intent(getActivity(),
                    FileTransferService.class);

            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);

            if (info.groupOwnerAddress.getHostAddress() != null) {
                serviceIntent.putExtra(
                        FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        info.groupOwnerAddress.getHostAddress());

                serviceIntent.putExtra(
                        FileTransferService.EXTRAS_GROUP_OWNER_PORT,
                        FileTransferService.PORT);
                serviceIntent.putExtra(FileTransferService.inetaddress,
                        FileTransferService.inetaddress);

            }

            getActivity().startService(serviceIntent);

            return "success";
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            if (result != null) {
                if (result.equalsIgnoreCase("success")) {
                    CommonMethods.e("On first Connect",
                            "On first Connect sent to asynctask");
                    ClientCheck = true;
                }
            }

        }

    }
}
