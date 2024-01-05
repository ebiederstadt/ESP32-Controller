# ESP32 Controller

An android app and associated server code for controlling an ESP microcontroller from your phone. The ESP creates a peer to peer WIFI network, meaning no wider internet connection is required. I'm using it for controlling a garage door from my phone, but it could have many uses. Built with Jetpack Compose and Kotlin (android) along with PlatformIO and C++ (ESP Server).

## Development Setup:
Install the following tools onto your computer:
- Android Studio
- Visual Studio Code with the PlatformIO extension

You will also need the following hardware:
- An android phone.
- An ESP32.
- Breadboard, resistors, LEDs, wires.
- Cables for connecting the ESP and android phone to your computer

After cloning the repository, create a file called `password.h` in `server/lib/password`. The file should contain the password you want to use for the WIFI network the ESP will create.

```c++
#pragma once
#define WIFI_PASSWORD "your_password"
```

Setup the ESP hardware. Connect an LED to ground and to pin 12 (through the appropriate resistor, for example 220omh). You can check your circuit by connecting the LED to positive (through a resistor).

Configure the server code with PlatformIO, then compile and upload it to the ESP. The serial console will print some information about the WIFI network created. On linux you may have to give permission to upload to the ESP:
```sh
sudo chmod a+rw /dev/ttyUSB0
```

Build the android app with Android Studio. It's necessary to use a real android device because the emulator does not have networking capabilities. I recommend using wired debugging, as the phone will lose it's wireless connection after switching to the ESP network and you will no longer have access to the logs.
