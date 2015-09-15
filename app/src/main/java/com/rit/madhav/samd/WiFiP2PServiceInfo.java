package com.rit.madhav.samd;

import android.net.wifi.p2p.WifiP2pDevice;

import java.io.Serializable;

/**
 * Created by maddy on 4/16/15.
 */
public class WiFiP2PServiceInfo implements Serializable {

    private static final long serialVersionUID = 3L;
    WifiP2pDevice device;
    String instanceName = null;
    String serviceRegistrationType = null;
    String id = "0";
}
