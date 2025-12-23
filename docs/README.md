# WayfindR Documentation

## Quick Links

- [Setup Guide](SETUP.md) - Installation and configuration
- [Architecture](ARCHITECTURE.md) - Technical overview and data flow
- [Kiosk Mode](KIOSK_MODE.md) - Kiosk implementation options and troubleshooting

## What is WayfindR?

WayfindR is an Android kiosk application for voice-based interaction with a remote LLM (Large Language Model). It's designed for public-facing deployments where users can ask questions using voice and receive spoken responses.

## Key Features

### Chat Mode (Normal)
- Text-based chat with LLM
- Speech-to-text input (microphone button)
- Text-to-speech output (speaker icon)
- Chat history with export to Markdown
- Configurable backend URL

### Kiosk Mode
- Voice-only interaction
- Continuous listening with speech confirmation
- Four visual states with distinct animations:
  - Blue (Listening) - Waiting for user
  - Green (Speaking) - User is talking
  - Orange (Processing) - Waiting for LLM
  - Purple (Responding) - TTS speaking
- Camera streaming (front + rear)
- Password-protected exit
- True lockdown via Device Owner mode

## Target Hardware

- Samsung Galaxy Tab A7 (primary target)
- Any Android 11+ tablet with:
  - Microphone
  - Speaker
  - Optional: Front/rear cameras

## Default Configuration

| Setting | Default Value |
|---------|---------------|
| LLM URL | http://192.168.0.100:5000 |
| Kiosk Password | wayfindr |
| Camera Stream Interval | 3 seconds |

## Quick Start

1. Install APK on tablet
2. Grant permissions (microphone, camera)
3. Configure LLM backend URL
4. (Optional) Set up Device Owner for true kiosk
5. Enter kiosk mode from menu

See [SETUP.md](SETUP.md) for detailed instructions.
