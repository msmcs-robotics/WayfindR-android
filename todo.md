OK so I have the project that I'm working on locally that is simply an Android app and I am not wanting to develop it locally but on a remote server seen in connections.Md. This is because I'm currently on WSL and that remote server is an actual metal based installation of Ubuntu with Android Studio. So I wanted to making the following changes to my app I need an actual kiosk mode where I can't just exit out of the app Because I press the home button or I shut the tablet off or something or I scrolled up from the very bottom of the screen if that makes sense I can put controls or physical security controls and cover up the home button or cover up the power button and sound buttons But from within the app I don't want to be able to scroll away from the kiosk mode unless I put in the password. Does this all make sense And then yeah that's pretty much it you should be able to get a read on what I'm actually trying to build and please keep it clean please keep it modular let me know any suggestions that you have to make because I want to be able to make a simple chat interface to have a chat with remote LLM and then I also want to have kiosk mode where people will just simply have voice chats and the LLM response will be printed out in text on the screen but then also the tablet will use text to voice using Google's built in features to do that to speak out the app. And I have not yet added the emulator for what I'm trying to build this on is gonna be a Samsung Galaxy tablet a 7 and I would need to download the skin for that tablet and then also make a proper emulator for it. Thank you.




APK app-debug.apk is not compatible with 16 KB devices. Some libraries have LOAD segments not aligned at 16 KB boundaries:
lib/x86_64/libimage_processing_util_jni.so
Starting November 1st, 2025, all new apps and updates to existing apps submitted to Google Play and targeting Android 15+ devices must support 16 KB page sizes. For more information about compatibility with 16 KB devices, visit developer.android.com/16kb-page-size.


> Task :app:compileDebugKotlin
w: file:///home/devel/Desktop/WayfindR-android/app/src/main/java/com/example/wayfindr/ChatMessageItem.kt:82:61 'val Icons.Filled.VolumeUp: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.VolumeUp.



also gradle build version was out of data, unless this was a local android studio problem, but i just installed the system very recently in the past few days.



when entering kiosk mode, i still see a "tap anywhere to exit kiosk mode" and when i tap, the buttons for home, viewing apps, and more popup from the bottom...

also when entering kiosk mode, a user is prompted with "swipe down from the top to exit full screen mode", when doing so, it has the same effect as a tap, the user can hit the home button, view apps, and back button like the main control buttons on android all without entering the password.



so is there a better way to enter a true kiosk mode where users absolutely cannot exit using buttons or physical controls without entering a password on an android app? 

I am okay with needing to give this app special system permissions to do so, just make it so that i can allow these permissions in app and not have to navigate to any system settings or anything...


