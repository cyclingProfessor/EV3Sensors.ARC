package uk.ac.rhul.cyclingprofessor.ev3sensors;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class Server {
    private final static int SERVER_PORT = 1234;
    private Handler mHandler;

    private int mState;
    private int mNewState;  // field so that we can track state in the debug log.

    private ServerThread serverThread;
    private ConnectionThread connectionThread;

    // Constants that indicate the current connection state
    static final int STATE_NONE = 0;       // we're doing nothing
    static final int STATE_LISTEN = 1;     // now listening for incoming connections
    static final int STATE_CONNECTED = 2;  // now connected to a remote device

    private static final String TAG = "EV3Sensors::Server";

    public Server(Activity activity, Handler handler) {
        mHandler = handler;
        Activity mActivity = activity;
        mState = STATE_NONE;
        mNewState = mState;
    }

    /**
     * Stops the service. Specifically stop all Threads
     * Called by Activity.OnPause
     */
    synchronized void stop() {
        Log.d(TAG, "start");

        // Cancel any thread currently running a connection
        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the service. Specifically start ServerThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread currently running a connection
        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        if (serverThread == null) {
            serverThread = new ServerThread();
            serverThread.start();
        }
        // Update UI title
        updateUserInterfaceTitle();
    }


    /**
     * Start the ConnectedThread to begin managing an EV3 connection
     *
     * @param socket The Socket on which the connection was made
     * @param EV3name The EV3 that has been connected
     */
    private synchronized void connected(Socket socket, String EV3name) {
        Log.d(TAG, "connected");

        // Cancel any thread currently running a connection
        if (connectionThread != null) {
            connectionThread.cancel();
            connectionThread = null;
        }

        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectionThread = new ConnectionThread(socket);
        connectionThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, EV3name);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUserInterfaceTitle();
    }


    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    synchronized int getState() {
        return mState;
    }

    /*
     * This internal class runs a socket server in a separate thread.
     */
    private class ServerThread extends Thread {
        private final ServerSocket serverSocket;

        ServerThread() {
            ServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                Log.e(TAG, "Server Socket listen() failed", e);
            }
            serverSocket = tmp;
            mState = STATE_LISTEN;
        }
        @Override
        public void run() {
            Log.d(TAG, "BEGIN  Listening (server) Thread");

            Socket socket;

            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Server Socket accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (Server.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                                // Situation normal. Start the connected thread.
                                String name = getEV3name(socket);
                                if (name == null) {
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        Log.e(TAG, "Could not close unwanted socket", e);
                                    }
                                } else {
                                    connected(socket, name);
                                }
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END Server Accept Thread");
        }
        void cancel() {
            Log.d(TAG, " Server Thread cancelled");
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "closing Server Socket failed", e);
            }
        }

    }

    /**
     * Get the EV3 name
     */
    private String getEV3name(Socket socket) {
        final String name_code = "EV3_NAME(";

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, name_code);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        return name_code;
//        final int MSG_LEN = name_code.length();
//        final byte END = ')';
//        int index = 0;
//        byte[] nBuffer = new byte[50]; // temporary name buffer
//
//        try {
//            socket.setSoTimeout(200);
//            InputStream in = socket.getInputStream();
//            in.read(nBuffer, 0, MSG_LEN);
//            String param_name = new String(nBuffer, 0, MSG_LEN);
//            if (!name_code.equals(param_name)) {
//                return null;
//            }
//            in.read(nBuffer, index, 1);
//            while  (index < 49 && nBuffer[index] != END) {
//                in.read(nBuffer, ++index, 1);
//            }
//            socket.setSoTimeout(0);
//        } catch (SocketTimeoutException ex) {
//            Log.e(TAG, "Timeout reading name from EV3 Socket - perhaps you forgot to send the name?", ex);
//            return null;
//        } catch (IOException e) {
//            Log.e(TAG, "Failure reading name from EV3 Socket", e);
//            return null;
//        }
//        return new String(nBuffer, 0, index);
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectionThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectionThread(Socket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }



    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectionThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectionThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = connectionThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }


    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        Server.this.start();
    }


}
