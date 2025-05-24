## Software overview

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
