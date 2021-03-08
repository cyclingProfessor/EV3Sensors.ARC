package uk.ac.rhul.cyclingprofessor.ev3sensors;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ConnectionListActivity extends Activity {

    /**
     * Tag for Log
     */
    @SuppressWarnings("unused")
    private static final String TAG = "ConnectionListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        setContentView(R.layout.activity_connection_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        /*
      Network Connections
     */
        ArrayAdapter<String> connectionsArrayAdapter = new ArrayAdapter<>(this, R.layout.connection_name);

        // Find and set up the ListView for paired devices
        ListView netAddresses = findViewById(R.id.devices);
        netAddresses.setAdapter(connectionsArrayAdapter);
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface net = interfaces.nextElement();
                Enumeration<InetAddress> add = net.getInetAddresses();
                while (add.hasMoreElements()) {
                    InetAddress a = add.nextElement();
                    if (!a.isLoopbackAddress()
                            && !a.getHostAddress().contains(":")) {
                        connectionsArrayAdapter.add(a.getHostAddress());
                        setResult(Activity.RESULT_OK);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.setFinishOnTouchOutside(true);

    }

}

