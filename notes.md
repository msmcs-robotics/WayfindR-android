## Software overview

- need to add settings/dropdown menu to select the LLM server URL and port
- need to add "continous conversation" mode selection in dropdown menu where the app keeps the chat history and sends the last message to the LLM server without user input
- move delete chat history button to settings menu
- move "share chat history" button to settings menu


I have the following basic chat up written in Kotlin for Android devices. I want to add two items to the drop down menu. Item will be a copy or clone of the item that allows the user to change the base url of the llm app or backend server and port. Want to have this Clone version effectively store a string that will be a password for the next item with the default password being "wayfindr". Label this item Change kiosk password. Then want to add the next item to the menu labeled start kiosk mode I then want to add kiosk mode to my app where when that item in the menu is clicked the entire app is overlaid with a simple circle animation that continuously listens for speech through on board microphone and speech to text recognition and continuously sends speech to the back end llm server url. In order to close kiosk mode a user has to tap on the screen inside the circle animation or icon and then the user will be prompted to enter a password and if that password hash does not match the hash of the previously configured password then the app will stay in kiosk mode where the user cannot configure any other aspects of the app. Please create  KioskMode.kt And integrate it into my existing code and add the password functionality for kiosk mode and add these items to the menu. Also in kiosk mode I do want to have the UI state indicator Shown through the basic circle animation. The circle animation when you first start up kiosk mode with the same blue as the ui state indicator but then if there's an error change the circle to red, but if there is no error keep the circle animation the same blue color. In terms of handling the response message while in kiosk mode I would likethe response to show up centered under the circle animation in a text view that is also centered. The text view should be large enough to display the response message and should have a scrollable feature if the message is too long. However I would also like to use onboard text to speechfrom Android to read off the message and onlystart listening again when reading off the message is complete.

I want to make an app on a galaxy tab a9+ that is a chat interface for a user to the tablet that supports audo STT and TTS using onboard android text to speech and speech to text to send a string of text to an LLM server over the network at url http://192.168.0.100:5000/chat

i want to have the user be able to type in a chat box centerd on the bottom and the rest of the screen be displaying the chat between the user and the LLM. display the llm responses in the chat app. I want the user to be able to click a microphone icon to do speech then the app interprets the text and sends it or the user can select the message icon and use the on screen keyboard to type a message and send to the llm url.

I am using a basic activity template in android studio so everything is written in kotlin. please create mainactivity.kt, better build.gradle.kts, etc... and create supporting kt files.



## Hardware Configuration

Built for a galaxy tab A9+

[**Device specs**](https://www.samsung.com/levant/tablets/galaxy-tab-a/galaxy-tab-a9-lte-graphite-64gb-sm-x115nzaamea/)

- API 31 android 12.0, Google API
- intel x86_64 atom
- 1340 x 800 resolution

[Helping with emulator skin](https://developer.samsung.com/galaxy-emulator-skin/guide.html#:~:text=What%20You%20Need,by%20clicking%20Show%20Advanced%20Settings.)

[Galaxy Tab Skins (pick the A9+)](https://developer.samsung.com/galaxy-emulator-skin/galaxy-tab.html)

## Useful

```powershell
Get-ChildItem -Recurse | Where-Object {
    $_.FullName -notmatch '\\.gradle\\' -and $_.FullName -notmatch '\\.idea\\' -and $_.FullName -notmatch '\\.cxx\\' -and $_.FullName -notmatch '\\test\\' -and $_.FullName -notmatch '\\includes\\' -and $_.FullName -notmatch '\\androidTest\\' -and $_.FullName -notmatch '\\build\\'
} | Select-Object -ExpandProperty FullName | Set-Clipboard
```

> File → Invalidate Caches / Restart → Invalidate and Restart




I am trying to create an app that provides a fronted for users to chat with a backend LLM server. Basically the user uses the onscreen keyboard or onboard Speech to text and a submit button to send a string of text to a backed server url. From there the LLM should provide a response message and i want that message added to the conversation and then if the user clicks a "speech" button the app will voice that response using onboard text to speech. Please refactor any necessary code to improve it by simplifying it but not diminishing quality. also please let me know a list of files or code pieces that I can remove all together or code pieces that are redundant.


given the changes you suggested, apply the same goals i listed earlier about simplification and refactoring to the xml files please. let me know which files i don't need or any other chnages.


i want to create a good backup_rules.xml and data_extraction_rules.xml, what exactly can i specify to backup or extract?

I want to backup all chat messages I also want to make sure the user can delete this data if they so choose. please create the backup rules and data extraction ruls xml files. i want to add a "Delete all chat history" button in my app and also a way to share chat history has a markdown file and export all chat messages in markdown format. this way the user can make an offline backup/reference while also clearing out their cloud storage or google drive impact.