package com.rit.madhav.samd;

import android.app.ListFragment;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PeerListFragment extends ListFragment implements WifiP2pManager.PeerListListener {

    public static String getDeviceStatus(int statusCode) {
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }

    WiFiDevicesAdapter listAdapter = null;
    private List<WifiP2pDevice> peerList = new ArrayList<WifiP2pDevice>();
    private HashMap<String, WiFiP2PServiceInfo> deviceMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_peer_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listAdapter = new WiFiDevicesAdapter(this.getActivity(),
                android.R.layout.simple_list_item_2, android.R.id.text1,
                new ArrayList<WiFiP2PServiceInfo>());
        setListAdapter(listAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO Auto-generated method stub
        ((PeerItemClickListner) getActivity()).connectP2p((WiFiP2PServiceInfo)
                l.getItemAtPosition(position));
        ((TextView) v.findViewById(android.R.id.text2)).setText("Connecting");

    }

    /**
     * The requested peer list is available
     *
     * @param peers List of available peers
     */
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        peerList.clear();
        peerList.addAll(peers.getDeviceList());
        if (peerList.size() == 0) {
            Log.d(WiFiDirectActivity.TAG, "No devices found");
            return;
        }
        //updateDeviceMapWithList(peerList);
    }

    public void updateDeviceMapWithList(List<WiFiP2PServiceInfo> list) {
        for (WiFiP2PServiceInfo service : list) {
            deviceMap.put(service.id, service);
        }
    }

    public HashMap<String,WiFiP2PServiceInfo> getDeviceMap() {
        return deviceMap;
    }

    public List<WifiP2pDevice> getPeerList() {
        return peerList;
    }

    public class WiFiDevicesAdapter extends ArrayAdapter<WiFiP2PServiceInfo> {

        private List<WiFiP2PServiceInfo> items;

        public WiFiDevicesAdapter(Context context, int resource,
                                  int textViewResourceId, List<WiFiP2PServiceInfo> items) {
            super(context, resource, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_2, null);
            }
            WiFiP2PServiceInfo service = items.get(position);
            if (service != null) {
                TextView nameText = (TextView) v
                        .findViewById(android.R.id.text1);

                if (nameText != null) {
                    nameText.setText(service.device.deviceName + " - " + service.instanceName);
                }
                TextView statusText = (TextView) v
                        .findViewById(android.R.id.text2);
                statusText.setText(getDeviceStatus(service.device.status));
            }
            return v;
        }

    }

    public interface PeerItemClickListner {
        public void connectP2p(WiFiP2PServiceInfo wifiP2pService);
    }
}
