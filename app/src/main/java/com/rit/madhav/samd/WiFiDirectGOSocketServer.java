package com.rit.madhav.samd;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by maddy on 4/13/15.
 */
public class WiFiDirectGOSocketServer extends Thread {
    ServerSocket groupOwnerSocket = null;
    Socket socket;
    private final int THREAD_COUNT = 10;
    private Handler groupOwnerHandler;
    private ConnectionManager manager;
    private int count = 0;
    private static final String TAG = "WiFiDirectGOSocketServer";

    /**
     * A ThreadPool for client nodes.
     */
    private final ThreadPoolExecutor nodePool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    public WiFiDirectGOSocketServer(Handler handler) throws IOException {
        try {
            this.groupOwnerSocket = new ServerSocket(WiFiDirectActivity.SERVER_PORT);
            this.groupOwnerHandler = handler;
            Log.d(TAG, "Socket Started");
        } catch (IOException e) {
            e.printStackTrace();
            nodePool.shutdownNow();
            throw e;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                // A blocking operation. Initiate a ConnectionManager instance when
                // there is a new connection
                socket = groupOwnerSocket.accept();
                manager = new ConnectionManager(socket, groupOwnerHandler);
                //new Thread(manager).start();
                nodePool.execute(manager);
                Log.d(TAG, "Launching the I/O handler");

                // change the exception to IOException
            } catch (Exception e) {
                try {
                    Log.d(WiFiDirectActivity.TAG, "server socket connection failed");
                    if (this.groupOwnerSocket != null && !this.groupOwnerSocket.isClosed())
                        this.groupOwnerSocket.close();
                } catch (IOException ioe) {

                }
                e.printStackTrace();
                nodePool.shutdownNow();
                break;
            }
        }
    }
}
