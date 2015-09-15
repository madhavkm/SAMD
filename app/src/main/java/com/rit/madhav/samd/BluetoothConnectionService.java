package com.rit.madhav.samd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.os.Handler;
import android.os.Message;

/**
 * Created by maddy on 4/20/15.
 */
public class BluetoothConnectionService {

    private static final String NAME = "SAMDService";
    private static final UUID UUID_APP =
            UUID.fromString("08368b55-40e2-44e9-bb64-9541f5ca0da1");

    public static final int CONNECTION_STATE_NONE = 0;       // none
    public static final int CONNECTION_STATE_LISTEN = 1;     // listening
    public static final int CONNECTION_STATE_CONNECTING = 2; // connecting
    public static final int CONNECTION_STATE_CONNECTED = 3;  // connected

    private int connectionState;
    private BluetoothAdapter mBluetoothAdapter = null;
    private PeerConnectionThread btPeerConnectionThread;
    private ConnectionAcceptThread btConnectionAcceptThread;
    private ConnectionThread btConnectionThread;
    private Handler btHandler;

    public BluetoothConnectionService (Handler handler) {
        this.setConnectionState(CONNECTION_STATE_NONE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btHandler = handler;
    }

    private synchronized void setConnectionState(int state) {
        Log.d(WiFiDirectActivity.TAG, "setting state..."+state);
        this.connectionState = state;
    }

    public synchronized int getState() {
        return this.connectionState;
    }

    public void start () {

        if (btPeerConnectionThread != null) {
            btPeerConnectionThread.cancel();
            btPeerConnectionThread = null;
        }

        if (btConnectionThread != null) {
            btConnectionThread.cancel();
            btConnectionThread = null;
        }

        Log.d(WiFiDirectActivity.TAG, "start connection listen thread");
        this.setConnectionState(CONNECTION_STATE_LISTEN);
        if (this.btConnectionAcceptThread == null) {
            Log.d(WiFiDirectActivity.TAG, "startedddd connection listen thread");
            this.btConnectionAcceptThread = new ConnectionAcceptThread();
            this.btConnectionAcceptThread.start();
        }
    }

    public void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (connectionState == CONNECTION_STATE_CONNECTING) {
            if (btPeerConnectionThread != null) {
                btPeerConnectionThread.cancel();
                btPeerConnectionThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (btConnectionThread != null) {
            btConnectionThread.cancel();
            btConnectionThread = null;
        }

        btPeerConnectionThread = new PeerConnectionThread(device);
        btPeerConnectionThread.start();
        setConnectionState(CONNECTION_STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        if (btPeerConnectionThread != null) {
            btPeerConnectionThread.cancel();
            btPeerConnectionThread = null;
        }

        if (btConnectionThread != null) {
            btConnectionThread.cancel();
            btConnectionThread = null;
        }

        if (btConnectionAcceptThread != null) {
            btConnectionAcceptThread.cancel();
            btConnectionAcceptThread = null;
        }

        Log.d(WiFiDirectActivity.TAG, "bluetooth connected");
        btConnectionThread = new ConnectionThread(socket);
        btConnectionThread.start();
        setConnectionState(CONNECTION_STATE_CONNECTED);
    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectionThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (connectionState != CONNECTION_STATE_CONNECTED) return;
            r = btConnectionThread;
        }
        // Perform the write unsynchronized
        Log.d(WiFiDirectActivity.TAG,"start write");
        r.write(out);
    }

    private class PeerConnectionThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public PeerConnectionThread(BluetoothDevice device) {
            btDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID_APP);
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "PeerConnectionThread createRfcomm failed", e);
            }
                btSocket = tmp;
        }

        public void run() {
            Log.d(WiFiDirectActivity.TAG, "Begin PeerConnectionThread");
            setName("PeerConnectionThread");
            mBluetoothAdapter.cancelDiscovery();

            try {
                btSocket.connect();
            } catch (IOException e1) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.e(WiFiDirectActivity.TAG, "PeerConnectionThread unable to close socket", e2);
                }
                return;
            }

            synchronized (BluetoothConnectionService.this) {
                btPeerConnectionThread = null;
            }

            // Start ConnectionThread
            connected(btSocket, btDevice);
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "PeerConnectionThread unable to close socket", e);
            }
        }
    }

    private class ConnectionThread extends Thread {
        private final BluetoothSocket btSocket;
        private final InputStream btInStream;
        private final OutputStream btOutStream;

        public ConnectionThread(BluetoothSocket socket) {
            Log.d(WiFiDirectActivity.TAG, "created ConnectionThread ");
            btSocket = socket;
            InputStream tmpInStream = null;
            OutputStream tmpOutStream = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpInStream = socket.getInputStream();
                tmpOutStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "temp sockets not created", e);
            }

            btInStream = tmpInStream;
            btOutStream = tmpOutStream;
        }

        public void run() {
            Log.i(WiFiDirectActivity.TAG, "run ConnectionThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Listen
            while (true) {
                try {
                    // Reading
                    bytes = btInStream.read(buffer);

                    // Store received bytes
                    Log.d(WiFiDirectActivity.TAG,"bt mesagge receiveddddd"+String.valueOf(buffer));
                    btHandler.obtainMessage(BluetoothConnectionManager.BT_MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, "bluetooth disconnected", e);

                    // Restart the server service
                    BluetoothConnectionService.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                Log.e(WiFiDirectActivity.TAG, "write started at bt");
                btOutStream.write(buffer);

                // Share the sent message back to the UI Activity
//                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
//                        .sendToTarget();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "Error during write", e);
            }
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "ConnectionThread unable to close socket", e);
            }
        }
    }

    private class ConnectionAcceptThread extends Thread {
        // The server socket for this device
        private final BluetoothServerSocket btServerSocket;
        private String mSocketType;

        public ConnectionAcceptThread() {
            BluetoothServerSocket tmpServerSkt = null;

            try {
                tmpServerSkt = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, UUID_APP);
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "device bluetooth socket listening failed", e);
            }
            btServerSocket = tmpServerSkt;
        }

        public void run() {
            Log.d(WiFiDirectActivity.TAG, "Socket run ConnectionAcceptThread");
            setName("ConnectionAcceptThread");

            BluetoothSocket socket = null;

            while (connectionState != CONNECTION_STATE_CONNECTED) {
                try {
                    socket = btServerSocket.accept();
                } catch (IOException e) {
                    Log.e(WiFiDirectActivity.TAG, "bluetooth socket start failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothConnectionService.this) {
                        switch (connectionState) {
                            case CONNECTION_STATE_LISTEN:
                            case CONNECTION_STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case CONNECTION_STATE_NONE:
                            case CONNECTION_STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(WiFiDirectActivity.TAG, "Could not close the bluetooth socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(WiFiDirectActivity.TAG, "END ConnectionAcceptThread");

        }

        public void cancel() {
            Log.d(WiFiDirectActivity.TAG, "cancel");
            try {
                btServerSocket.close();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "bluetooth socket cancel of server failed", e);
            }
        }
    }
}
