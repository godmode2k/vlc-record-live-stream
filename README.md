# VLC-Record-Live-Stream


Summary
----------
> LibVLC-based Record-Live-Stream



Environment
----------
```sh
Android:
 - Android Studio (android-studio-2024.2.1.12, gradle 8.9)
 - LibVLC (org.videolan.android:libvlc-all:4.0.0-eap23)
```



Features
----------
```sh
Android:
 - Open URL List: Add, Delete, Edit, Open
 - Recorded List: Copy recorded file (Internal Storage) to <Public Download Directory>, Delete, Play recorded file (Internal Storage)
 - Playback: Open URL, Play, Pause, Stop, Mute, Record Stream, Recorded List, Full-Screen
```



Notes
----------
```sh
Android:
[Issue #1]
    Android libvlc: File-based HTTP stream H.264 playback error, but sometimes playback works good.

    Test URL: Big Buck Bunny, http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
    Error log:
    ../../src/player/timer.c:318: void vlc_player_UpdateTimerSource(vlc_player_t *, struct vlc_player_timer_source *, double, vlc_tick_t, vlc_tick_t): assertion "ts >= VLC_TICK_0" failed
    Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE) in tid 15219 (AudioTrack)
```



LibVLC License
----------
```sh
LibVLC: GNU LGPLv2.1
 - https://code.videolan.org/videolan/vlc
 - https://code.videolan.org/videolan/vlc/-/blob/master/COPYING
```



Screenshots
----------

> Android App: Stream URL List
<img src="https://github.com/godmode2k/vlc-record-live-stream/raw/main/screenshot_20251111_03.jpg" width="40%" height="40%">

> Android App: Playback Live Stream
<img src="https://github.com/godmode2k/vlc-record-live-stream/raw/main/screenshot_20251111_04.jpg" width="40%" height="40%">

> Android App: Recording Live Stream
<img src="https://github.com/godmode2k/vlc-record-live-stream/raw/main/screenshot_20251111_05.jpg" width="40%" height="40%">

> Android App: Recorded List
<img src="https://github.com/godmode2k/vlc-record-live-stream/raw/main/screenshot_20251111_06.jpg" width="40%" height="40%">

> Android App: Playback Recorded File (Internal Storage)
<img src="https://github.com/godmode2k/vlc-record-live-stream/raw/main/screenshot_20251111_07.jpg" width="40%" height="40%">

> Android App: Playback File-based HTTP Stream
<img src="https://github.com/godmode2k/vlc-record-live-stream/raw/main/screenshot_20251111_08.jpg" width="40%" height="40%">
