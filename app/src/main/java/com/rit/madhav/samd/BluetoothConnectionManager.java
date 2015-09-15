package com.rit.madhav.samd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.List;
import java.util.Set;


/**
 * Created by maddy on 4/17/15.
 */
public class BluetoothConnectionManager {

    public static final int BT_MESSAGE_READ = 0x400 + 1;

    private WiFiDirectActivity mActivity;
    private static BluetoothConnectionManager connectionManager;
    public BluetoothAdapter mBluetoothAdapter = null;

    public WifiP2pDevice peerDevice;
    public String peerDeviceAddress;
    public BluetoothConnectionService connService;
    private Reduction reductionManager;


    private BluetoothConnectionManager (WiFiDirectActivity main) {
        mActivity = main;
        connService = new BluetoothConnectionService(mHandler);
        reductionManager = new Reduction();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case MY_HANDLE:
//                    Log.d(TAG,"setting connection manager");
//                    Object obj = msg.obj;
//                    setConnectionManager((ConnectionManager) obj);
//                    break;

                case BT_MESSAGE_READ:
                    Log.d(WiFiDirectActivity.TAG,"message read");
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d(WiFiDirectActivity.TAG, "messageeee"+readMessage);
                    if (readMessage.equals("send")) {
                        double tempMean = reductionManager.getMean();
                        Log.d(WiFiDirectActivity.TAG,"mean from peerss"+tempMean);
                        mActivity.sendBackToGOWithMean(tempMean);
                    }
//                Object mapObj = msg.obj;
//                clientMap = (HashMap) mapObj;
//                Log.d(TAG, "map received ......... =="+clientMap.keySet());
                    break;
            }
        }
    };

    public static BluetoothConnectionManager getInstance (WiFiDirectActivity main) {
        if (connectionManager == null) {
            connectionManager = new BluetoothConnectionManager(main);
        }

        return connectionManager;
    }

    public boolean enableBluetooth () {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d(WiFiDirectActivity.TAG, "Device does not support Bluetooth");
        }

        return mBluetoothAdapter.isEnabled();
    }

    public void makeDeviceDiscoverable() {
        Log.d(WiFiDirectActivity.TAG,"makeDeviceDiscoverable..........");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            //startActivity(discoverableIntent);
        }
    }

    public void startBTListening () {
        connService.start();
    }

//    public void connectToDevice (WifiP2pDevice device) {
//        mActivity.appendStatus("Setting Bluetooth Connections");
//        BluetoothDevice btDevice = checkPairedDevicesForDevice(device);
//        if (btDevice != null) {
//            Log.d(WiFiDirectActivity.TAG,"Device found in paired connections");
//            connectBT(btDevice);
//        } else {
//            this.peerDevice = device;
//            mBluetoothAdapter.startDiscovery();
//            Log.d(WiFiDirectActivity.TAG,"Discovery started");
//        }
//    }

    public void connectToDevice (String deviceAddress) {
        mActivity.appendStatus("Setting Bluetooth Connections");
        BluetoothDevice btDevice = checkPairedDevicesForDevice(deviceAddress);
        if (btDevice != null) {
            Log.d(WiFiDirectActivity.TAG,"Device found in paired connections");
            Log.d(WiFiDirectActivity.TAG,"paired connection deviceeeee"+btDevice.getName());
            connectBT(btDevice);

        } else {
            this.peerDeviceAddress = deviceAddress;
            mBluetoothAdapter.startDiscovery();
            Log.d(WiFiDirectActivity.TAG,"Discovery started");
        }
    }

    public void startSending () {
        String btSend = "send";
        connService.write(btSend.getBytes());
    }

    public void connectBT(BluetoothDevice device) {
       // BluetoothDevice btDevice = mBluetoothAdapter.getRemoteDevice(device.deviceAddress);

        this.connService.connect(device);
        mActivity.appendStatus("Connected with peer");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startSending();
    }

//   private BluetoothDevice checkPairedDevicesForDevice (WifiP2pDevice device) {
//       Set <BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//       BluetoothDevice btDevice = null;
//       // If there are paired devices
//       if (pairedDevices.size() > 0) {
//           // Loop through paired devices
//           for (BluetoothDevice pairedDev : pairedDevices) {
//               if (pairedDev.getAddress().equals(device.deviceAddress)) {
//                   btDevice = pairedDev;
//                   return btDevice;
//               }
//           }
//       }
//
//       return btDevice;
//    }

    private BluetoothDevice checkPairedDevicesForDevice (String deviceAddress) {
        Set <BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d(WiFiDirectActivity.TAG,"checking for paired devices"+pairedDevices.toString());
        BluetoothDevice btDevice = null;
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice pairedDev : pairedDevices) {
                Log.d(WiFiDirectActivity.TAG,"paired device"+pairedDev.getName());
                if (pairedDev.getName().equalsIgnoreCase(deviceAddress)) {
                    btDevice = pairedDev;
                    return btDevice;
                }
            }
        }

        return btDevice;
    }

    public void cancel () {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(WiFiDirectActivity.TAG,"Discovery cancelled");
        }
    }

    // Create a BroadcastReceiver for Actions
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (peerDeviceAddress != null && device.getName().equals(peerDeviceAddress)) {
                    Log.d(WiFiDirectActivity.TAG,"Peer device found");

                    //connect(peerDevice);
                    connectBT(device);
                }

                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(WiFiDirectActivity.TAG,"Bluetooth turned off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(WiFiDirectActivity.TAG,"Bluetooth turned on");
                        break;
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(WiFiDirectActivity.TAG, "Discovery finished");
            }
        }
    };
}
