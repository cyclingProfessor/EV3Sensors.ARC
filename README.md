#EV3Sensors
***

This project extends the sensor and monitoring capabilities of an EV3 robot by adding an Android device.

The only coding required on the robot is to open and manage a TCP/IP (text) connection (to the Android device).

1. Any text sent to the Android socket from the robot will be displayed on the Android screen in a scrollable list.

    Since this is achieved in a Handler object in the main Activity class it is easy to add your own interpretations.  Add a method call to the "MESSAGE_READ" case in the handler code.

2. The Android app can send messages asynchronously to the EV3 by calling send_message(text) in the main Activity.

   At present this is done in the Camera module after any (novel) QR code is seen.  This openCV module also finds all BLUE blobs and the centre of the largest, but does nothing with this information - it could send something (say a recognised face) to the EV3.

3. The Android app also registers an Intent to receive notifications of NFC tags.  It extracts the text from such tags and sends this to the EV3.

4. Finally, for simple control, there is an EditText View in which you can type a message and press send.  This text also goes to the EV3.

It is expected that Android voice recognition or openCV face recognition could easily be added to this app.

***

#Usage.

You will need to set up the app by installing openCV manager 3.43. The download is available [here](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.4.3/), you can then install the applicable apk provided.
Then install the apk.

You also need to connect to the robot over TCP/IP.  If the robot has a WiFi dongle then all well and good.  For Bluetooth you need to "reverse tether" your Android Bluetooth paired connection.  Simply pair the devices and then, when connected, use the setting icon in the Bluetooth screen to "allow internet" using this connection.

The app always listens on port 1234 (hard coded).  However it gets its IP address either from being on WiFi or from the EV3 Bluetooth AP.  In either case the EV3 will need to know this address in order to connect as there is no service discovery coded.

To this end we have a menu item - show IPv4 internet addresses - which will work to show you all of the available addresses.  Now you need to find a way to enter such addresses into a running EV3 program - Your problem not mine.

Happy Hacking.

***

#Requirements

Android Device.  EV3 robot (why not run Lejos or some other language making TCP/IP connections easy?)

OpenCV Manager on Android.

It uses zxing - the most excellent framework for reading and DESIGNING QR codes (zxing.com)

I have used (modified) several small pieces of code from the internet and have acknowledged them in the source code.

The architecture for this code merges the BlueToothChat Android example and the BlobDetector Android OpenCV example and still has some code from both sources.

---

Suggestion for Lejos and ev3dev use: Why not create an AndroidSensor class on the EV3, with an internal running Thread reading the Socket.  It is unusual in that you can send it a "connect" message, or indeed a "send" message to send text to Android.  It should "parse" text from the Android device and have several getters allowing you to monitor available (new) data of different types (QR, NFC, Face, Text, etc.,)

I propose have an "String getQR()"" etc., and a "void markQRread" etc., then you can easily use the data from the Android device in the "takeControl" methods of your behaviours, since each take control method can get any pending QR code, check if it its relevant and only mark it read in its "action" method.

Of course you can miss QR codes if another is read before you have processed the previous one.  If this is likely then you will need to code some sort of buffer in the AndroidSensor class. 
