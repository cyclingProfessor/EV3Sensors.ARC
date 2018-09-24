package uk.ac.rhul.cyclingprofessor.ev3sensors;

/**
 * Defines several constants used to glue interface to Server code.
 */
interface Constants {

    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_DEVICE_NAME = 3;
    int MESSAGE_TOAST = 4;

    // Key names received from the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

}

