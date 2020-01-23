# EV3Sensors
***

This project extends the sensor and monitoring capabilities of an EV3 robot by adding an Android device.

The only coding required on the robot is to open and manage a TCP/IP (text) connection (to the Android device).

1. Any text sent to the Android socket from the robot will be displayed on the Android screen in a scrollable list.

    Since this is achieved in a Handler object in the main Activity class it is easy to add your own interpretations.  Add a method call to the "MESSAGE_READ" case in the handler code.

2. The Android app can send messages asynchronously to the EV3 by calling send_message(text) in the main Activity.

   At present this is done in the Camera module after any (novel) QR code is seen.  This openCV module also finds all BLUE blobs and the centre of the largest, but does nothing with this information - it could send something (say a recognised face) to the EV3.

3. The Android app also registers an Intent to receive notifications of NFC tags.  It extracts the text from such tags and sends this to the EV3.

4. Finally, for simple control, there is an EditText View in which you can type a message and press send.  This text also goes to the EV3.

It would be straightforward to add Android voice recognition or openCV face recognition or indeed any other Android sensor data to be sent to the robot.

***

# Usage.

You will need to set up the app by installing openCV manager 3.43. The download is available [here](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.4.3/), you can then install the applicable apk provided.

I use the Droid Info app to find out the correct architecture for an apk on an Android device (it is free).

You also need to connect to the robot over TCP/IP.  If the robot has a WiFi dongle then all well and good.  For Bluetooth you need to "reverse tether" your Android Bluetooth paired connection.  Simply pair the devices and then, when connected, use the setting icon in the Bluetooth screen to "allow internet" using this connection.

The app always listens on port 1234 (hard coded).  However it gets its IP address either from being on WiFi or from the EV3 Bluetooth AP.  In either case the EV3 will need to know this address in order to connect as there is no service discovery coded.

To this end we have a menu item - show IPv4 internet addresses - which will work to show you all of the available addresses.  Now you need to find a way to enter such addresses into a running EV3 program - Your problem not mine.

Happy Hacking.

***

# Requirements

Android Device.  EV3 robot (why not run Lejos or some other language making TCP/IP connections easy?)

OpenCV Manager on Android.

It uses zxing - the most excellent framework for reading and DESIGNING QR codes (zxing.com)

I have used (modified) several small pieces of code from the internet and have acknowledged them in the source code.

The architecture for this code merges the BlueToothChat Android example and the BlobDetector Android OpenCV example and still has some code from both sources.

---

To make this work on your LeJOS robot, create an AndroidSensor class on the EV3, with an internal running Thread reading the Socket.  

It should "parse" text received from the Android device and have several getters allowing you to monitor available (new) data of different types (QR, NFC, Face, Text, etc.,)

For example have a "String getQR()" and a "void markQRread".  Then you can easily use the data from the Android device in the "takeControl" methods of your behaviours, by calling getQR(). Check if the QR is relevant and only mark it read in the "action" method.

Your LeJOS code can also send messages to the Android APP.  There is commented out code in the Android App expecting a "connect" message.  Any text sent to Android can be displayed, or acted on.

Of course you can miss QR codes if another is seen before you have processed the previous one.
