package com.rit.madhav.samd;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by maddy on 4/13/15.
 */
public class WiFiDirectClientSocketHandler extends Thread {

    private Handler clientHandler;
    private ConnectionManager manager;
    private InetAddress groupOwnerAddress;

    public WiFiDirectClientSocketHandler(Handler handler, InetAddress mAddress) {
        this.clientHandler = handler;
        this.groupOwnerAddress = mAddress;
    }

    @Override
    public void run() {
        Socket clientSocket = new Socket();
        try {
            clientSocket.bind(null);
            clientSocket.connect(new InetSocketAddress(this.groupOwnerAddress.getHostAddress(),
                    WiFiDirectActivity.SERVER_PORT), 5000);
            //clientSocket.setKeepAlive();
            Log.d(WiFiDirectActivity.TAG, "Launching I/O handler for client");

            manager = new ConnectionManager(clientSocket, clientHandler);
            new Thread(manager).start();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, "client socket connection failed");
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }
}
