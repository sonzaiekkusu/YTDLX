# Notices and references

YTDLX uses the Android library `io.github.junkfood02.youtubedl-android` as its local yt-dlp engine. The library is distributed under GPL-3.0 and is based on the `yausername/youtubedl-android` project.

The runtime and package-management architecture was studied from [YTDLnis](https://github.com/deniscerri/ytdlnis) and [ytdlnis-packages](https://github.com/deniscerri/ytdlnis-packages). In particular, YTDLnis's `RuntimeManager` demonstrates initialization and execution of packaged Python, yt-dlp, FFmpeg, and JavaScript runtimes on Android.

YTDLX does not copy YTDLnis source files. The reference is acknowledged here for architectural study and implementation direction. Before distributing a release, review GPL-3.0 obligations, dependency licenses, attribution requirements, and any naming restrictions of referenced projects.

## Links

- https://github.com/deniscerri/ytdlnis
- https://github.com/deniscerri/ytdlnis-packages
- https://github.com/yausername/youtubedl-android
- https://central.sonatype.com/artifact/io.github.junkfood02.youtubedl-android/library/0.18.1
- https://www.gnu.org/licenses/gpl-3.0.html
