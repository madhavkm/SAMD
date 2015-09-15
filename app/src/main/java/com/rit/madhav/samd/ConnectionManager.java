package com.rit.madhav.samd;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by maddy on 4/19/15.
 */
public class ConnectionManager implements Runnable {

    private static final String TAG = "ConnectionManager";
    private Socket connectionSocket = null;
    private Handler connectionHandler;

    private InputStream inStream;
    private OutputStream outStream;

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;


    public ConnectionManager(Socket socket, Handler handler) {
        this.connectionSocket = socket;
       this.connectionHandler = handler;
    }

    /**
     * Starts executing the active part of the class' code. This method is
     * called when a thread is started that has been created with a class which
     * implements {@code Runnable}.
     */
    @Override
    public void run() {
        try {
            Log.d(WiFiDirectActivity.TAG,"in run method of conn manager111");
            Log.d(WiFiDirectActivity.TAG,"in run method of conn manager222"+connectionSocket.getInputStream()
                    +"  .."+connectionSocket.getOutputStream());
            connectionHandler.obtainMessage(WiFiDirectActivity.MY_HANDLE, this)
                    .sendToTarget();
            Log.d(WiFiDirectActivity.TAG, "in run method of conn manager333");
//            inStream = new ObjectInputStream(connectionSocket.getInputStream());
//            outStream = new ObjectOutputStream(connectionSocket.getOutputStream());
            inStream = connectionSocket.getInputStream();
            outStream = connectionSocket.getOutputStream();
            byte[] buffer = new byte[128];
            int bytes;
            Object obj = null;
            Log.d(WiFiDirectActivity.TAG,"in run method of conn manager444");

//            ObjectInputStream tempinputStream = new ObjectInputStream(connectionSocket.getInputStream());
           // outputStream = new ObjectOutputStream(connectionSocket.getOutputStream());
            Log.d(WiFiDirectActivity.TAG,"in run method of conn manager555");


            while (true) {
                try {
                    // Read from the InputStream
//                    try {
//                        obj = tempinputStream.readObject();
//                    } catch (ClassNotFoundException e) {
//                        e.printStackTrace();
//                    }
                    bytes = inStream.read(buffer);
                    if (bytes == -1) {
                        Log.d(WiFiDirectActivity.TAG,"break in read of connection manager");
                        break;
                    }
                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    connectionHandler.obtainMessage(WiFiDirectActivity.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();
//                    Log.d(WiFiDirectActivity.TAG, "read object : " + String.valueOf(obj));
//                    connectionHandler.obtainMessage(WiFiDirectActivity.MESSAGE_READ, obj).sendToTarget();

//                    if (bytes == -1) {
//                        break;
//                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Log.d(WiFiDirectActivity.TAG,"close socket..connection manager");
                connectionSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            Log.d(WiFiDirectActivity.TAG,"write method"+outStream);
            outStream.write(buffer);
            //outStream.flush();
            //outStream.reset();
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }
}
