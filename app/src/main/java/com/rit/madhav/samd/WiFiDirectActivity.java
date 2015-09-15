package com.rit.madhav.samd;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.rit.madhav.samd.PeerListFragment.WiFiDevicesAdapter;

import static com.rit.madhav.samd.ResultFragment.ResultDisplayListener;

public class WiFiDirectActivity extends ActionBarActivity implements
        View.OnClickListener, PeerListFragment.PeerItemClickListner,
        WifiP2pManager.ConnectionInfoListener, Handler.Callback, ResultDisplayListener {

    TextView mainTextView;
    Button mainButton;
    EditText mainEditText;
    ProgressBar progressIndicator;
    TextView testSample;

    public static final String CONNRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "wifi_p2p";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final String TAG = "SAMD";

    static final int SERVER_PORT = 8999;
    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    private static Integer clientCount;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver receiver = null;
    private BluetoothConnectionManager bluetoothManager;

    private WifiP2pDnsSdServiceRequest serviceRequest;
    private PeerListFragment servicesList;
    private ResultFragment displayView;
    private boolean display;
    private ConnectionManager wifiManager;
    private HashMap<String, WiFiP2PServiceInfo> clientMap = new HashMap<>();
    private List<WiFiP2PServiceInfo> clientList = new ArrayList<>();
    private String deviceID;
    private HashMap<String,List<String>> devMap = new HashMap<>();
    private List<String> devMAC = new ArrayList<>();
    private List<String> devName = new ArrayList<>();
    private String thisDeviceName;
    private boolean isGrpOwner = false;
    private boolean isPeer = false;
    private List<ConnectionManager> connManagerList = new ArrayList<>();

    private Handler handler = new Handler(this);

    public Handler getHandler() {
        return handler;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi_direct);

        Log.d(TAG, "onCreate==============================");

        mainTextView = (TextView)findViewById(R.id.main_textView);
        //testSample = (TextView)findViewById(R.id.value);
        mainButton = (Button)findViewById(R.id.main_button);
        mainButton.setOnClickListener(this);
        //mainEditText = (EditText)findViewById(R.id.main_editText);
        progressIndicator = (ProgressBar)findViewById(R.id.progress_indicator);
        progressIndicator.setVisibility(View.INVISIBLE);

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        servicesList = new PeerListFragment();
        //displayView = ResultDisplay.newInstance("1","2");

        getFragmentManager().beginTransaction()
                .add(R.id.container_root, servicesList, "services").commit();
        display = false;
        clientCount = 0;
        //startRegistration();


        WifiManager wimanager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        //String macAddress = wimanager.getConnectionInfo().getMacAddress();
        //String macAddress = wimanager.getConnectionInfo().get;
        thisDeviceName = BluetoothAdapter.getDefaultAdapter().getName();
//        if (macAddress == null) {
//            macAddress = "Device don't have mac address or wi-fi is disabled";
//        }

        Log.d(TAG,"this Device Name..... : "+thisDeviceName);
        createDeviceMac();
        createTempDevMap();
    }

    public void onImageClick(View view) {
        Reduction temp = new Reduction();
        //appendStatus(String.valueOf(temp.getMean()));
        //testSample.setText(String.valueOf(temp.getMean()));
    }

    private void createDeviceMac() {
        devMAC.add("f8:a9:d0:1c:03:16"); // D1
        devMAC.add("f8:a9:d0:03:e0:1b"); // D2 //no
        devMAC.add("2c:54:cf:e5:55:6d"); // D3 // 2C:54:CF:73:8C:EF
        devMAC.add("F4:09:D8:FA:F4:53"); // D4 S
        devMAC.add("40:0E:85:0B:34:7c"); // S4

        devName.add("D1");
        devName.add("D2");
        devName.add("D3");
    }

    private void createTempDevMap () {
        Log.d(TAG,"createTempDevMap......");
        List<String> tempList = new ArrayList<>();
        tempList.add("D3");
        devMap.put("D2",tempList);

        tempList = new ArrayList<>();
        tempList.add("D1");
        devMap.put("D3",tempList);

        tempList = new ArrayList<>();
        tempList.add("D2");
        devMap.put("D1",tempList);
    }

    @Override
    protected void onRestart() {
        Fragment frag = getFragmentManager().findFragmentByTag("services");
        if (frag != null) {
            getFragmentManager().beginTransaction().remove(frag).commit();
        }
        Log.d(TAG, "onRestart///////////////////==============================");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "onStop Disconnect failed :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "onStop Disconnect success :");
                }

            });
        }
        Log.d(TAG, "onStop.........///////////////////==============================");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wi_fi_direct, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.direct_discover) {
            discoverService();
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean containsClient (WifiP2pDevice info) {
        for (WiFiP2PServiceInfo cInfo : clientList) {
            if (cInfo.device.deviceAddress.equals(info.deviceAddress)) {
                return true;
            }
        }

        return false;
    }

    private WiFiP2PServiceInfo addToClientList(WifiP2pDevice info) {
        if (!containsClient(info)) {
            WiFiP2PServiceInfo service = new WiFiP2PServiceInfo();
            service.device = info;
            service.instanceName = SERVICE_INSTANCE;
            service.serviceRegistrationType = SERVICE_REG_TYPE;
            service.id = String.valueOf(clientCount);
            clientCount++;
            clientList.add(service);
            return service;
        }

        return null;
    }

    private void startRegistration () {
        Map<String, String> record = new HashMap<String, String>();
        record.put(CONNRECORD_PROP_AVAILABLE, "visible");
        //record.put("DeviceName", Build.MODEL);
        record.put("DeviceName", "mydevice");

        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Local service added");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Local service failed to add");
            }
        });

        //discoverService();
        mManager.setDnsSdResponseListeners(mChannel,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType,
                                                        WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?

                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {

                            // update the UI and add the item the discovered
                            // device.
                            PeerListFragment fragment = (PeerListFragment) getFragmentManager()
                                    .findFragmentByTag("services");
                            if (fragment != null) {
                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
                                        .getListAdapter());
                                WiFiP2PServiceInfo service = addToClientList(srcDevice);
                                if (service != null) {
                                    //appendStatus("Device name... " + service.device.deviceName + "   device address..." + service.device.deviceAddress);
                                    adapter.add(service);
                                    adapter.notifyDataSetChanged();
                                }
//                                WiFiP2PServiceInfo service = new WiFiP2PServiceInfo();
//                                service.device = srcDevice;
//                                service.instanceName = instanceName;
//                                service.serviceRegistrationType = registrationType;
                            }
                        }

                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get("available"));
                    }
                });

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mManager.addServiceRequest(mChannel, serviceRequest,
                new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        appendStatus("Added service discovery request");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });
    }

    private void discoverService () {
//        mManager.setDnsSdResponseListeners(mChannel,
//                new WifiP2pManager.DnsSdServiceResponseListener() {
//
//                    @Override
//                    public void onDnsSdServiceAvailable(String instanceName,
//                                                        String registrationType,
//                                                        WifiP2pDevice srcDevice) {
//
//                        // A service has been discovered. Is this our app?
//
//                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
//
//                            // update the UI and add the item the discovered
//                            // device.
//                            PeerListFragment fragment = (PeerListFragment) getFragmentManager()
//                                    .findFragmentByTag("services");
//                            if (fragment != null) {
//                                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
//                                        .getListAdapter());
//                                WiFiP2PServiceInfo service = addToClientList(srcDevice);
//                                if (service != null) {
//                                    adapter.add(service);
//                                    adapter.notifyDataSetChanged();
//                                }
////                                WiFiP2PServiceInfo service = new WiFiP2PServiceInfo();
////                                service.device = srcDevice;
////                                service.instanceName = instanceName;
////                                service.serviceRegistrationType = registrationType;
//                            }
//                        }
//
//                    }
//                }, new WifiP2pManager.DnsSdTxtRecordListener() {
//
//                    /**
//                     * A new TXT record is available. Pick up the advertised
//                     * buddy name.
//                     */
//                    @Override
//                    public void onDnsSdTxtRecordAvailable(
//                            String fullDomainName, Map<String, String> record,
//                            WifiP2pDevice device) {
//                        Log.d(TAG,
//                                device.deviceName + " is "
//                                        + record.get("available"));
//                    }
//                });
//
//        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
//        mManager.addServiceRequest(mChannel, serviceRequest,
//                new WifiP2pManager.ActionListener() {
//
//                    @Override
//                    public void onSuccess() {
//                        appendStatus("Added service discovery request");
//                    }
//
//                    @Override
//                    public void onFailure(int arg0) {
//                        appendStatus("Failed adding service discovery request");
//                    }
//                });

        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                appendStatus("Service discovery failed");
            }
        });
    }

    public void startBluetoothConnection() {
        bluetoothManager = BluetoothConnectionManager.getInstance(this);
        // Register for broadcasts when a device is discovered
        registerReceiver(bluetoothManager.mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(bluetoothManager.mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Register for broadcasts when discovery has finished
        registerReceiver(bluetoothManager.mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        appendStatus("setup Bluetooth Connections");
        if (!bluetoothManager.enableBluetooth()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        bluetoothManager.startBTListening();
        appendStatus("Waiting for peers");

        Log.d(WiFiDirectActivity.TAG,"makeDeviceDiscoverable..........");
        if (bluetoothManager.mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }

        Log.d(WiFiDirectActivity.TAG,"makeDeviceDiscoverable..........enddddd");
    }



    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        mainTextView.setText("Starting...");
        progressIndicator.setVisibility(View.VISIBLE);
        servicesList.updateDeviceMapWithList(clientList);
        Log.d(TAG,"connection manager in main"+wifiManager.toString());
        //Log.d(TAG,"device map"+servicesList.getDeviceMap());
       // wifiManager.write(servicesList.getDeviceMap());
        String test = "connected";
        for (ConnectionManager manager : connManagerList) {
            Log.d(WiFiDirectActivity.TAG,"send from GO");
            manager.write(thisDeviceName.getBytes());
        }
        mainButton.setEnabled(false);
//        displayView = new ResultFragment();
//
//        getFragmentManager().beginTransaction()
//                .replace(R.id.container_root, displayView).commit();
        //SensorDataActivity sensorReading = new SensorDataActivity();
        //float result = displayView.getMean();




        //displayView = ResultDisplay.newInstance("1","2");
        //getFragmentManager().beginTransaction().replace(R.id.container_root, displayView);


                //.add(R.id.container_root, displayView, "result").commit();

//        wifiManager.write(test.getBytes());

        //startRegistration();
//            displayView = new ResultDisplay();
//            getFragmentManager().beginTransaction()
//                    .add(R.id.container_root, displayView, "display").commit();
//


        //startRegistration();
        //discoverService();
//        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
//            @Override
//            public void onSuccess() {
//                // Code for when the discovery initiation is successful goes here.
//                // No services have actually been discovered yet, so this method
//                // can often be left blank.  Code for peer discovery goes in the
//                // onReceive method, detailed below.
//            }
//
//            @Override
//            public void onFailure(int reasonCode) {
//                // Code for when the discovery initiation fails goes here.
//                // Alert the user that something went wrong.
//            }
//        });
    }

    public void sendBackToGOWithMean(double mean) {
        for (ConnectionManager manager : connManagerList) {
            Log.d(WiFiDirectActivity.TAG,"send to GO........");
            String.valueOf(mean).getBytes();
            manager.write(String.valueOf(mean).getBytes());
        }
    }

    public void appendStatus(String status) {
        String current = mainTextView.getText().toString();
        mainTextView.setText(current + "\n" + status);
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume--------------.........///////////////////==============================");
        receiver = new WiFiP2PBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(receiver, intentFilter);
        startRegistration();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause+++++++++++++++++.........///////////////////==============================");
        unregisterReceiver(receiver);
    }

    @Override
    public void onDestroy() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "onStop Disconnect failed :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "onStop Disconnect success :");
                }

            });
        }
        super.onDestroy();
        if (bluetoothManager != null) {
            bluetoothManager.cancel();
            unregisterReceiver(bluetoothManager.mReceiver);
        }
    }

    @Override
    public void connectP2p(WiFiP2PServiceInfo wifiP2pService) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = wifiP2pService.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        if (serviceRequest != null)
//            mManager.removeServiceRequest(mChannel, serviceRequest,
//                    new WifiP2pManager.ActionListener() {
//
//                        @Override
//                        public void onSuccess() {
//                        }
//
//                        @Override
//                        public void onFailure(int arg0) {
//                        }
//                    });

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Connecting to service");
            }

            @Override
            public void onFailure(int errorCode) {
                appendStatus("Failed connecting to service");
            }
        });
    }

    /**
     * The requested connection info is available
     *
     * @param info Wi-Fi p2p connection info
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Thread connection = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */

        Log.d(TAG, "peer list start");
        for (WifiP2pDevice dev : servicesList.getPeerList()) {
            Log.d(TAG, dev.deviceName);

        }

        Log.d(TAG, "peer list start handler"+getHandler());
        if (info.isGroupOwner && info.groupFormed) {
            Log.d(TAG, "Connected as group owner");

            try {
                if (!isGrpOwner) {
                    appendStatus("This is Group Owner");
                    isGrpOwner = true;
                    connection = new WiFiDirectGOSocketServer(this.getHandler());
                    connection.start();
                }
            } catch (Exception e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");

            mainButton.setEnabled(false);

            //startBluetoothConnection();
            //appendStatus("setup Bluetooth Connections");
            if (!isPeer) {
                appendStatus("This is Peer");
                isPeer = true;
                connection = new WiFiDirectClientSocketHandler(this.getHandler(), info.groupOwnerAddress);
                connection.start();
                startBluetoothConnection();
            }

            //bluetoothManager.makeDeviceDiscoverable();
//            PeerListFragment fragment = (PeerListFragment) getFragmentManager()
//                    .findFragmentByTag("services");
//            if (fragment != null) {
//                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
//                        .getListAdapter());
//
//            }

            //appendStatus("Waiting for peers");
        }
    }

    private void setConnectionManager (ConnectionManager manager) {
        connManagerList.add(manager);
        wifiManager = manager;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MY_HANDLE:
                Log.d(TAG,"setting connection manager");
                Object obj = msg.obj;
                setConnectionManager((ConnectionManager) obj);
                break;

            case MESSAGE_READ:
                Log.d(TAG,"message read");
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, "messageeee......"+readMessage);
                if (devName.contains(readMessage)) {
                    Log.d(TAG,"thisDeviceName......"+thisDeviceName);
                    if (thisDeviceName != null) {
                        List <String> devAddr = devMap.get(thisDeviceName);
                        Log.d(TAG,"tempmac..."+devMap.keySet());
                        Log.d(TAG,"tempmac2...."+devAddr);
                        if (devAddr != null && devAddr.size() > 0) {
                            Log.d(TAG, "start BT connection.............");
                            String connDev = devAddr.get(0);
                            if (!connDev.equalsIgnoreCase(readMessage)) {
                                Log.d(TAG,"connDev....."+connDev);
                                bluetoothManager.connectToDevice(connDev);
                            } else {
                                Log.d(TAG,"this is GO..........");
                            }
                        }
                    }
                } else {
                    Log.d(TAG,"mean........."+readMessage);
                    displayView = new ResultFragment();

                    getFragmentManager().beginTransaction()
                            .replace(R.id.container_root, displayView).commit();
                    //appendStatus("Result Received");
                }
//                Object mapObj = msg.obj;
//                String temp = (String) mapObj;
//                Log.d(TAG, "map received ......... =="+temp);
                break;
        }
        return true;
    }

    @Override
    public void display(float result) {
        mainTextView.setText("Done");
        appendStatus("Ambient Light Intensity in lux");
        mainTextView.setTextSize(24.0f);
        displayView.displayResult(String.valueOf(result));

        progressIndicator.setVisibility(View.INVISIBLE);
    }
}
