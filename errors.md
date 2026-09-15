# Known Issues & Diagnostic Log

## [RESOLVED] 4K AV1 1 FPS Lag & Non-functional Quality Switching

### Root Causes
1. **AV1 Software Decoding Disaster (1 FPS)**:
   - For movies with 4K available, Alloha includes a 4K UHD variant `master.m3u8` which uses the AV1 codec (`video/av01`, `codecs=av01.0.12M.08`, `3840x1606`).
   - `AllohaRuntimeParser.parseAllohaBNsiStream` unconditionally assigned `itemMasterURL = urls.firstOrNull { isMasterM3u8(it) }` from the `"4k"` key, and set `pickedURL = itemMasterURL`. This forced "Авто" to default to 4K AV1.
   - Most TV chips and emulators lack hardware AV1 decoders (`NoSupport [sizeAndRate.support, 3840x1606@23.974] [c2.android.av1-dav1d.decoder, video/av01]`), falling back to CPU software decoding (`c2.android.av1-dav1d.decoder`), pegging the CPU at 100% and playing at ~1 FPS.
2. **Quality Switching Doing Nothing**:
   - In `PlayerViewModel.selectQuality(quality)`, `currentVideoUrl` was never updated (`// We do NOT replace currentVideoUrl...`).
   - The individual qualities (1080p, 720p, etc.) are separate URLs (`index-1080.m3u8`, `index-720.m3u8`). Because `currentVideoUrl` stayed on `master.m3u8`, ExoPlayer was forced to keep playing `master.m3u8`.
   - In `PlayerScreen.kt`, the track selector tested `h in (targetHeight - 80)..(targetHeight + 80)`. For widescreen 2.39:1 movies, 1080p is 1920x800 and 720p is 1280x534, so height matching failed completely.

### Fixes Applied
1. **`CodecHelper.kt`**: Introduced hardware decoder detection using `MediaCodecList` to verify hardware AV1 capabilities.
2. **`AllohaRuntimeParser.kt`**:
   - Do not let 4K `master.m3u8` override `itemMasterURL` as the default movie stream.
   - Default "Авто" to the best available safe <=1080p stream (`index-1080.m3u8`), guaranteeing smooth 60 FPS hardware-accelerated playback on all Android TV devices.
   - Retain 2160p (4K), 1080p, 720p, 480p, etc. in `qualityVariants` so the user can choose any resolution.
   - Normalize widescreen dimensions in `parseMasterPlaylistQualities` (1920x800 -> 1080p, 1280x534 -> 720p).
3. **`PlayerViewModel.kt`**:
   - `selectQuality(quality)` now sets `currentVideoUrl = effectiveProxyUrl`, triggering ExoPlayer's `LaunchedEffect(state.currentVideoUrl)` to immediately reload at `lastPreservedPositionMs`.
   - `selectAudioTrack` preserves active non-Auto quality across translations.
4. **`PlayerScreen.kt`**: Updated resolution matching for widescreen dimensions (checking both width and height) and cleared size constraints for dedicated single-rendition streams.
5. **`HlsProxyServer.kt`**: Only filter AV1 variants if the device lacks hardware AV1 support (`!CodecHelper.isHardwareAv1Supported()`).

---

## Raw Session Logs (Previous)

2026-09-15 00:18:16.030  7098-7161  okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v2/movie/1137844?v=5
2026-09-15 00:18:16.104  7098-7216  com.sloosh.tv           com.sloosh.tv                        W  JNI critical lock held for 23.756ms on Thread[52,tid=7216,Runnable,Thread*=0xe00f1010,peer=0x29ea720,"arch_disk_io_1"]
2026-09-15 00:18:16.399  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 5236KB AllocSpace bytes, 26(524KB) LOS objects, 49% free, 7739KB/15MB, paused 1.847ms,2.839ms total 137.197ms
2026-09-15 00:18:16.778  7098-7161  okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v2/movie/1137844?v=5 (746ms, unknown-length body)
2026-09-15 00:18:17.030  7098-7205  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.068  7098-7152  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.082  7098-7153  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.122  7098-7153  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.200  7098-7155  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.206  7098-7205  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.216  7098-7154  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.282  7098-7205  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:17.632  7098-7112  com.sloosh.tv           com.sloosh.tv                        I  Compiler allocated 6341KB to compile void com.sloosh.tv.ui.details.DetailsScreenKt.SidePosterDetailsLayout(com.sloosh.tv.data.api.MediaDetailsDto, com.sloosh.tv.ui.details.DetailsUiState, com.sloosh.tv.ui.details.DetailsViewModel, androidx.compose.ui.focus.FocusRequester, kotlin.jvm.functions.Function0, androidx.compose.runtime.Composer, int, int)
2026-09-15 00:18:19.619  7098-7205  AllohaRepository        com.sloosh.tv                        D  Fetching Alloha catalog: https://api.alloha.tv/?token=ffbd312217e27c4245f2678afe1881&kp=6446910
2026-09-15 00:18:20.367  7098-7205  AllohaRepository        com.sloosh.tv                        D  Alloha API HTTP response: 200 for kp=6446910
2026-09-15 00:18:20.411  7098-7205  CompatChangeReporter    com.sloosh.tv                        D  Compat change id reported: 247079863; UID 10093; state: ENABLED
2026-09-15 00:18:20.426  7098-7205  AllohaRepository        com.sloosh.tv                        D  Successfully parsed Alloha data: title=Мэйдэй, isSerial=false, seasons=0, movieTranslations=3
2026-09-15 00:18:22.036  7098-7098  libc                    com.sloosh.tv                        W  Access denied finding property "vendor.display-size"
2026-09-15 00:18:22.142  7098-7098  ExoPlayerImpl           com.sloosh.tv                        I  Init d89ecb6 [AndroidXMedia3/1.3.1] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:18:22.242  7098-7098  AudioSystem             com.sloosh.tv                        D  onNewService: media.audio_policy service obtained 0x8b3a9650
2026-09-15 00:18:22.251  7098-7098  AudioSystem             com.sloosh.tv                        D  getService: checking for service media.audio_policy: 0x8b3a9650
2026-09-15 00:18:22.429  7098-7098  com.sloosh.tv           com.sloosh.tv                        I  hiddenapi: Accessing hidden method Landroid/media/AudioTrack;->getLatency()I (runtime_flags=0, domain=platform, api=unsupported) from Landroidx/media3/exoplayer/audio/AudioTrackPositionTracker; (domain=app) using reflection: allowed
2026-09-15 00:18:22.521  7098-7098  AudioSystem             com.sloosh.tv                        D  onNewServiceWithAdapter: media.audio_flinger service obtained 0x8b0c68d0
2026-09-15 00:18:22.555  7098-7098  AudioSystem             com.sloosh.tv                        D  getService: checking for service media.audio_flinger: 0x8b3a7160
2026-09-15 00:18:22.672  7098-7154  AllohaRepository        com.sloosh.tv                        D  Fetching Alloha catalog: https://api.alloha.tv/?token=ffbd312217e27c4245f2678afe1881&kp=1137844
2026-09-15 00:18:22.689  7098-7203  HWUI                    com.sloosh.tv                        I  Davey! duration=756ms; Flags=0, FrameTimelineVsyncId=139297, IntendedVsync=7137917830520, Vsync=7137917830520, InputEventId=865039362, HandleInputStart=7137931527580, AnimationStart=7137931544680, PerformTraversalsStart=7138650001980, DrawStart=7138650072580, FrameDeadline=7137934497186, FrameStartTime=7137931515380, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=7138660013680, SyncStart=7138660260780, IssueDrawCommandsStart=7138660371980, SwapBuffers=7138671925880, FrameCompleted=7138674655680, DequeueBufferDuration=23000, QueueBufferDuration=401900, GpuCompleted=7138674541280, SwapBuffersCompleted=7138674655680, DisplayPresentTime=0, CommandSubmissionCompleted=7138671925880,
2026-09-15 00:18:22.988  7098-7155  HlsProxy                com.sloosh.tv                        D  HLS proxy started on port 8181
2026-09-15 00:18:23.073  7098-7154  AllohaRepository        com.sloosh.tv                        D  Alloha API HTTP response: 200 for kp=1137844
2026-09-15 00:18:23.077  7098-7154  AllohaRepository        com.sloosh.tv                        W  Alloha response has no 'data' object
2026-09-15 00:18:23.195  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 7601KB AllocSpace bytes, 24(640KB) LOS objects, 49% free, 7179KB/14MB, paused 2.290ms,3.918ms total 226.950ms
2026-09-15 00:18:23.350  7098-7098  com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xe8e0da54) locale list changing from [] to [en-US]
2026-09-15 00:18:23.352  7098-7098  WebViewFactory          com.sloosh.tv                        I  Loading com.google.android.webview version 151.0.7922.202 (code 792220206)
2026-09-15 00:18:23.355  7098-7098  com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xe8e13f94) locale list changing from [] to [en-US]
2026-09-15 00:18:23.363  7098-7098  com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xe8e16ed4) locale list changing from [] to [en-US]
2026-09-15 00:18:23.366  7098-7098  com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xe8e15cd4) locale list changing from [] to [en-US]
2026-09-15 00:18:23.369  7098-7098  ResourcesManager        com.sloosh.tv                        V  The following library key has been added: ResourcesKey{ mHash=622f21af mResDir=null mSplitDirs=[] mOverlayDirs=[] mLibDirs=[/data/app/~~gyFkwrIgbqFttmjDz0SYIw==/com.google.android.webview-ndFeIeXIOie8syAQMYRSlw==/base.apk,/system/framework/android.test.base.jar] mDisplayId=0 mOverrideConfig=v36 mCompatInfo={320dpi always-compat} mLoaders=[]}
2026-09-15 00:18:23.375  7098-7098  ApplicationLoaders      com.sloosh.tv                        D  Returning zygote-cached class loader: /system/framework/android.test.base.jar
2026-09-15 00:18:23.395  7098-7098  nativeloader            com.sloosh.tv                        D  Configuring clns-8 for other apk /data/app/~~gyFkwrIgbqFttmjDz0SYIw==/com.google.android.webview-ndFeIeXIOie8syAQMYRSlw==/base.apk. target_sdk_version=36, uses_libraries=, library_path=/data/app/~~gyFkwrIgbqFttmjDz0SYIw==/com.google.android.webview-ndFeIeXIOie8syAQMYRSlw==/lib/x86:/data/app/~~gyFkwrIgbqFttmjDz0SYIw==/com.google.android.webview-ndFeIeXIOie8syAQMYRSlw==/base.apk!/lib/x86, permitted_path=/data:/mnt/expand
2026-09-15 00:18:23.874  7098-7098  cr_WVCFactoryProvider   com.sloosh.tv                        I  version=151.0.7922.202 (792220206) minSdkVersion=29 multiprocess=true packageId=2 splits=<none>
2026-09-15 00:18:23.905  7098-7098  nativeloader            com.sloosh.tv                        D  Load /data/app/~~gyFkwrIgbqFttmjDz0SYIw==/com.google.android.webview-ndFeIeXIOie8syAQMYRSlw==/base.apk!/lib/x86/libwebviewchromium.so using class loader ns clns-8 (caller=/data/app/~~gyFkwrIgbqFttmjDz0SYIw==/com.google.android.webview-ndFeIeXIOie8syAQMYRSlw==/base.apk): ok
2026-09-15 00:18:23.919  7098-7098  nativeloader            com.sloosh.tv                        D  Load /system/lib/libwebviewchromium_plat_support.so using class loader ns clns-8 (caller=/data/app/~~gyFkwrIgbqFttmjDz0SYIw==/com.google.android.webview-ndFeIeXIOie8syAQMYRSlw==/base.apk): ok
2026-09-15 00:18:23.944  7098-7239  chromium                com.sloosh.tv                        E  [0914/191823.932010:ERROR:android_webview/browser/variations/variations_seed_loader.cc:39] Seed missing signature.
2026-09-15 00:18:23.979  7098-7098  cr_LibraryLoader        com.sloosh.tv                        I  Successfully loaded native library
2026-09-15 00:18:23.982  7098-7098  cr_CachingUmaRecorder   com.sloosh.tv                        I  Flushed 45 samples from 22 histograms, 0 samples were dropped.
2026-09-15 00:18:23.995  7098-7098  cr_ChildProcLH          com.sloosh.tv                        I  ScopedServiceBindingBatch.tryActivate: false
2026-09-15 00:18:24.003  7098-7098  cr_CombinedPProvider    com.sloosh.tv                        I  #registerProvider() provider:WV.sj@24ffda3 isPolicyCacheEnabled:false policyProvidersSize:0
2026-09-15 00:18:24.003  7098-7098  cr_PolicyProvider       com.sloosh.tv                        I  #setManagerAndSource() 0
2026-09-15 00:18:24.005  7098-7238  cr_policy               com.sloosh.tv                        I  registerReceiver succeeded after 2ms
2026-09-15 00:18:24.012  7098-7098  cr_DisplayManager       com.sloosh.tv                        I  Is Display Topology available: false
2026-09-15 00:18:24.015  7098-7098  com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xe8e16154) locale list changing from [] to [en-US]
2026-09-15 00:18:24.024  7098-7098  com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xe8e0e7d4) locale list changing from [] to [en-US]
2026-09-15 00:18:24.105  7098-7098  cr_CombinedPProvider    com.sloosh.tv                        I  #linkNativeInternal() 1
2026-09-15 00:18:24.108  7098-7098  cr_AppResProvider       com.sloosh.tv                        I  #getApplicationRestrictionsFromUserManager() Bundle[EMPTY_PARCEL]
2026-09-15 00:18:24.108  7098-7098  cr_PolicyProvider       com.sloosh.tv                        I  #notifySettingsAvailable() 0
2026-09-15 00:18:24.108  7098-7098  cr_CombinedPProvider    com.sloosh.tv                        I  #onSettingsAvailable() 0
2026-09-15 00:18:24.109  7098-7098  cr_CombinedPProvider    com.sloosh.tv                        I  #flushPolicies()
2026-09-15 00:18:24.633  7098-7253  cr_media                com.sloosh.tv                        W  BLUETOOTH_CONNECT permission is missing.
2026-09-15 00:18:24.642  7098-7253  cr_media                com.sloosh.tv                        W  getBluetoothAdapter() requires BLUETOOTH permission
2026-09-15 00:18:24.652  7098-7253  cr_media                com.sloosh.tv                        W  registerBluetoothIntentsIfNeeded: Requires BLUETOOTH permission
2026-09-15 00:18:24.853  7098-7098  cr_AppResProvider       com.sloosh.tv                        I  #getApplicationRestrictionsFromUserManager() Bundle[EMPTY_PARCEL]
2026-09-15 00:18:24.854  7098-7098  cr_PolicyProvider       com.sloosh.tv                        I  #notifySettingsAvailable() 0
2026-09-15 00:18:24.854  7098-7098  cr_CombinedPProvider    com.sloosh.tv                        I  #onSettingsAvailable() 0
2026-09-15 00:18:24.854  7098-7098  cr_CombinedPProvider    com.sloosh.tv                        I  #flushPolicies()
2026-09-15 00:18:24.861  7098-7098  Choreographer           com.sloosh.tv                        I  Skipped 94 frames!  The application may be doing too much work on its main thread.
2026-09-15 00:18:24.918  7098-7272  libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.log"
2026-09-15 00:18:24.927  7098-7272  libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.log.file"
2026-09-15 00:18:24.929  7098-7272  MESA                    com.sloosh.tv                        E  Failed to open rendernode: No such file or directory
2026-09-15 00:18:24.964  7098-7281  CameraManagerGlobal     com.sloosh.tv                        I  Connecting to camera service
2026-09-15 00:18:25.241  7098-7272  libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.vk.trace.per.submit"
2026-09-15 00:18:25.241  7098-7272  libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.gallium.print.optio"
2026-09-15 00:18:25.242  7098-7272  libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.vk.trace.frame"
2026-09-15 00:18:25.278  7098-7272  libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.vk.wsi.headless.swa"
2026-09-15 00:18:25.434  7098-7098  Choreographer           com.sloosh.tv                        I  Skipped 33 frames!  The application may be doing too much work on its main thread.
2026-09-15 00:18:25.458  7098-7118  HWUI                    com.sloosh.tv                        I  Davey! duration=2151ms; Flags=0, FrameTimelineVsyncId=139520, IntendedVsync=7139284497132, Vsync=7140851163736, InputEventId=0, HandleInputStart=7140862982180, AnimationStart=7140863039080, PerformTraversalsStart=7140867667980, DrawStart=7140885568380, FrameDeadline=7139351163796, FrameStartTime=7140861429580, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=7141359177080, SyncStart=7141359327280, IssueDrawCommandsStart=7141359535780, SwapBuffers=7141431517180, FrameCompleted=7141436355980, DequeueBufferDuration=16200, QueueBufferDuration=580000, GpuCompleted=7141436227880, SwapBuffersCompleted=7141436355980, DisplayPresentTime=0, CommandSubmissionCompleted=7141431517180,
2026-09-15 00:18:25.534  7098-7098  chromium                com.sloosh.tv                        I  [INFO:CONSOLE:30] "Allow attribute will take precedence over 'allowfullscreen'.", source: https://antipathic-as.stravers.live/ (30)
2026-09-15 00:18:25.903  7098-7262  VideoCapabilities       com.sloosh.tv                        W  Unrecognized profile/level 0/3 for video/mpeg2
2026-09-15 00:18:26.423  7098-7262  VideoCapabilities       com.sloosh.tv                        W  Unrecognized profile/level 0/3 for video/mpeg2
2026-09-15 00:18:26.423  7098-7262  VideoCapabilities       com.sloosh.tv                        W  Unrecognized profile/level 0/3 for video/mpeg2
2026-09-15 00:18:28.277  7098-7098  chromium                com.sloosh.tv                        I  [INFO:CONSOLE:177] "Access to image at 'android-webview-video-poster:default_video_poster/-3880865160802689881' from origin 'https://antipathic-as.stravers.live' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.", source: https://antipathic-as.stravers.live/?token_movie=637b293f26f35a89598b6ca31e91ab&token=ffbd312217e27c4245f2678afe1881&translation=276 (177)
2026-09-15 00:18:32.958  7098-7098  chromium                com.sloosh.tv                        I  [INFO:CONSOLE:1] "test ready", source: https://antipathic-as.stravers.live/build/app.039336e4.js (1)
2026-09-15 00:18:35.637  7098-7098  chromium                com.sloosh.tv                        I  [INFO:CONSOLE:0] "Access to image at 'android-webview-video-poster:default_video_poster/-3880865160802689881' from origin 'https://antipathic-as.stravers.live' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.", source: https://antipathic-as.stravers.live/?token_movie=637b293f26f35a89598b6ca31e91ab&token=ffbd312217e27c4245f2678afe1881&translation=276 (0)
2026-09-15 00:18:36.066  7098-7098  AllohaResolver          com.sloosh.tv                        D  Resolved stream successfully: videoUrl=https://97-65-e1-r502.vkvideo.cloud/1/i3WCVDltKql2S00dViU9RK0iTrcfBL0NtDolY-y6S93aT7A1mFa5zY52x7bZmV0Ea5FlXgVvJuPUtPl_i2fBQ2-0C-Qm5_LB71Dl8zMqB2_6KbNFrSUrqJ_ia7MXxv95TSu7mDjwRoopUIqqvr4AVHdBL-3eN39VQXG2UBYifgTyJu8pSd37rtq5EGCcLnJ93U3t4EoVf6m6cheeD8TuHMwYLAn1pHK7WUa-6AsgR8Zws2H8vsmqlb4tlxK5jeT-yZQy5uyiQoTDTU4LV9SZo8g_y71bevNvxY-s1ryD8HhjkXwYr5fKY70FX6OjXajmAPwEXIOiQTKOWD0aIeN_A7WGks8urm1l5kNWlOefqbaf4T-ZGY0kRxIXftQyggbT3l5pBnUXw4sqvYwXku2SEOY_cErhx_0HG8hGRhvHfA-dkPXNnitSJsDi6BoqqTZmiiFCw7Xw5H574l5YC8gnKiw1uaZhpHUPp2bZcPTrbnklSaah5FtWTfE1GtNv6SGF7huNNLTLEN65ixtYeB52WSV36NPBvJRFidGGDmqjaXqo3EohRLLXESkwYwl_71tudGXBlwaRx2vXuGlfLA2RIlQfuYueItm7_cXnp-p4P7_Kf_JLDgJsBwANWXHZl1KnLqjVjtHD7-K-dhIE2_9gMW7cqJUBPkC0dw8y9TRCrZy1_SEXDjHNkdezb39vKL1XjdztZcNkFtq0lhhWY0rIVS4VTzHIQjpoyKtypl_vMmQ2BSTkSWVmB2tGJiwpR41hg363aYlls3WSRtenW81Bs8onrtPV1V5w6t-5CvRUS5Q-8ln4rjt5Q6Jlp2vlKBy6wq4zU0zs8VEe5gSXz1SStg79n32j72ok40EAKYZLXADOSIWuSlqjtU1tm2LiBunFeCMnF5CbMPwumqCseMBQstMZQU40tQiyD8Xfp03ejToXw6YBEBaFmH4GbpOtx0dOAsvhTgyWCpGVLE8lGOKCdj-vC53LZYrobMirLSb3068Q1qykElg8wS4Y8DR_boamjuGZINPuU7mF5iKIIUclxCAK5Kg9IgOh7oRkKAW42ZXMdvRRBUhaodzb-KjGjQsZfaHfaITvyXDx01xiMcymmnd-g38u9BAyyHW4VHn5EyQdB2qk7zmSMpoN5bZN290IdWOXK0zHz8MnTPEuokIng47XRL5yXCJrkTF1jo2MRV3f15hlc0rHGfn7sIB4fgY5QwKDWSEsyhUjR_wO_aMwK6CcgZXmVCe86jRf2R9VyZpo11EGoaG8rfIUATJsoN4ZTbJ0u_ye9pTS44ea--_aMsXGS5QAsKuMH_NVuM3Xh-VjSmwXbPg5HvS4zsOTa7sOG7SMKXY9beYdWmO9PQjymYsdSi5zKpSxqdlJ9Fo-Ubz2ct5J_ucbbSysIp-UDz8jii71_I7FsxsbeNZP80vmisRo-98qfCZo-QgkUq5-SddK4bjRwC5WdrcyBoAjwsiM9gkRo2DLzHdFVC49Z53CbV0jZbEjbXI6UBz6T6yeqs8LowPPsOlgXSXZUVj7IzZwy7D60ow5IubuxrkCbx2X2EG9NCD7xjNtUPbB2foAqRfxcEgmu9yz-NkCn-D7TWcbvtBjrlPxnhWcHFV1KT75HW8GTAQ_uRmUuprk7aW_U4GG3Hyd2pGRx0hNFbrk5IqU2ezXn-7DxCL5HjuCMhTFNJcjPaKUyqeR5njcrRHRVbWPsc2MJlvcef5if_Hf112WYqsh5osnBzHdfDMXy_H4vPxagmCKNq06Jo5LSJezSob3Dfx-cdYh8Pa4LeWnBQIh10LI4sACCNnVYM2wBqKWi4sbxGTX8yW0-q0enS-uDrgF4rw6C00BGTeV4pvd-HMCETp41F_qVl84rtfwiuLFinRjIWC4x-b1DAFncBR8y2usKFG5x98o9YZ0lHVtdMjNtipWuLpC1FDKNKR5art986I_8imTCPaziTTdoedzcvGOAVynqqLAnMbPb2yx0ZbTcy_tS1pNtxugRit4bRwCV62PRqOAcaJsF8svkI7F7zRoB2wzns1o932PDn85glDd9MbvxQpNT1xlmqWDXrNtAdHAsrTnc53GnmjMCMj7RcQp1OO2yYMScqs7hUE/master.m3u8, audioVariants=3
2026-09-15 00:18:36.067  7098-7098  HlsProxy                com.sloosh.tv                        D  Active master URL updated: https://97-65-e1-r502.vkvideo.cloud/1/i3WCVDltKql2S00dViU9RK0iTrcfBL0NtDolY-y6S93aT7A1mFa5zY52x7bZmV0Ea5FlXgVvJuPUtPl_i2fBQ2-0C-Qm5_LB71Dl8zMqB2_6KbNFrSUrqJ_ia7MXxv95TSu7mDjwRoopUIqqvr4AVHdBL-3eN39VQXG2UBYifgTyJu8pSd37rtq5EGCcLnJ93U3t4EoVf6m6cheeD8TuHMwYLAn1pHK7WUa-6AsgR8Zws2H8vsmqlb4tlxK5jeT-yZQy5uyiQoTDTU4LV9SZo8g_y71bevNvxY-s1ryD8HhjkXwYr5fKY70FX6OjXajmAPwEXIOiQTKOWD0aIeN_A7WGks8urm1l5kNWlOefqbaf4T-ZGY0kRxIXftQyggbT3l5pBnUXw4sqvYwXku2SEOY_cErhx_0HG8hGRhvHfA-dkPXNnitSJsDi6BoqqTZmiiFCw7Xw5H574l5YC8gnKiw1uaZhpHUPp2bZcPTrbnklSaah5FtWTfE1GtNv6SGF7huNNLTLEN65ixtYeB52WSV36NPBvJRFidGGDmqjaXqo3EohRLLXESkwYwl_71tudGXBlwaRx2vXuGlfLA2RIlQfuYueItm7_cXnp-p4P7_Kf_JLDgJsBwANWXHZl1KnLqjVjtHD7-K-dhIE2_9gMW7cqJUBPkC0dw8y9TRCrZy1_SEXDjHNkdezb39vKL1XjdztZcNkFtq0lhhWY0rIVS4VTzHIQjpoyKtypl_vMmQ2BSTkSWVmB2tGJiwpR41hg363aYlls3WSRtenW81Bs8onrtPV1V5w6t-5CvRUS5Q-8ln4rjt5Q6Jlp2vlKBy6wq4zU0zs8VEe5gSXz1SStg79n32j72ok40EAKYZLXADOSIWuSlqjtU1tm2LiBunFeCMnF5CbMPwumqCseMBQstMZQU40tQiyD8Xfp03ejToXw6YBEBaFmH4GbpOtx0dOAsvhTgyWCpGVLE8lGOKCdj-vC53LZYrobMirLSb3068Q1qykElg8wS4Y8DR_boamjuGZINPuU7mF5iKIIUclxCAK5Kg9IgOh7oRkKAW42ZXMdvRRBUhaodzb-KjGjQsZfaHfaITvyXDx01xiMcymmnd-g38u9BAyyHW4VHn5EyQdB2qk7zmSMpoN5bZN290IdWOXK0zHz8MnTPEuokIng47XRL5yXCJrkTF1jo2MRV3f15hlc0rHGfn7sIB4fgY5QwKDWSEsyhUjR_wO_aMwK6CcgZXmVCe86jRf2R9VyZpo11EGoaG8rfIUATJsoN4ZTbJ0u_ye9pTS44ea--_aMsXGS5QAsKuMH_NVuM3Xh-VjSmwXbPg5HvS4zsOTa7sOG7SMKXY9beYdWmO9PQjymYsdSi5zKpSxqdlJ9Fo-Ubz2ct5J_ucbbSysIp-UDz8jii71_I7FsxsbeNZP80vmisRo-98qfCZo-QgkUq5-SddK4bjRwC5WdrcyBoAjwsiM9gkRo2DLzHdFVC49Z53CbV0jZbEjbXI6UBz6T6yeqs8LowPPsOlgXSXZUVj7IzZwy7D60ow5IubuxrkCbx2X2EG9NCD7xjNtUPbB2foAqRfxcEgmu9yz-NkCn-D7TWcbvtBjrlPxnhWcHFV1KT75HW8GTAQ_uRmUuprk7aW_U4GG3Hyd2pGRx0hNFbrk5IqU2ezXn-7DxCL5HjuCMhTFNJcjPaKUyqeR5njcrRHRVbWPsc2MJlvcef5if_Hf112WYqsh5osnBzHdfDMXy_H4vPxagmCKNq06Jo5LSJezSob3Dfx-cdYh8Pa4LeWnBQIh10LI4sACCNnVYM2wBqKWi4sbxGTX8yW0-q0enS-uDrgF4rw6C00BGTeV4pvd-HMCETp41F_qVl84rtfwiuLFinRjIWC4x-b1DAFncBR8y2usKFG5x98o9YZ0lHVtdMjNtipWuLpC1FDKNKR5art986I_8imTCPaziTTdoedzcvGOAVynqqLAnMbPb2yx0ZbTcy_tS1pNtxugRit4bRwCV62PRqOAcaJsF8svkI7F7zRoB2wzns1o932PDn85glDd9MbvxQpNT1xlmqWDXrNtAdHAsrTnc53GnmjMCMj7RcQp1OO2yYMScqs7hUE/master.m3u8
2026-09-15 00:18:36.149  7098-7098  HlsProxy                com.sloosh.tv                        D  Active master URL updated: https://97-65-e1-r502.vkvideo.cloud/1/i3WCVDltKql2S00dViU9RK0iTrcfBL0NtDolY-y6S93aT7A1mFa5zY52x7bZmV0Ea5FlXgVvJuPUtPl_i2fBQ2-0C-Qm5_LB71Dl8zMqB2_6KbNFrSUrqJ_ia7MXxv95TSu7mDjwRoopUIqqvr4AVHdBL-3eN39VQXG2UBYifgTyJu8pSd37rtq5EGCcLnJ93U3t4EoVf6m6cheeD8TuHMwYLAn1pHK7WUa-6AsgR8Zws2H8vsmqlb4tlxK5jeT-yZQy5uyiQoTDTU4LV9SZo8g_y71bevNvxY-s1ryD8HhjkXwYr5fKY70FX6OjXajmAPwEXIOiQTKOWD0aIeN_A7WGks8urm1l5kNWlOefqbaf4T-ZGY0kRxIXftQyggbT3l5pBnUXw4sqvYwXku2SEOY_cErhx_0HG8hGRhvHfA-dkPXNnitSJsDi6BoqqTZmiiFCw7Xw5H574l5YC8gnKiw1uaZhpHUPp2bZcPTrbnklSaah5FtWTfE1GtNv6SGF7huNNLTLEN65ixtYeB52WSV36NPBvJRFidGGDmqjaXqo3EohRLLXESkwYwl_71tudGXBlwaRx2vXuGlfLA2RIlQfuYueItm7_cXnp-p4P7_Kf_JLDgJsBwANWXHZl1KnLqjVjtHD7-K-dhIE2_9gMW7cqJUBPkC0dw8y9TRCrZy1_SEXDjHNkdezb39vKL1XjdztZcNkFtq0lhhWY0rIVS4VTzHIQjpoyKtypl_vMmQ2BSTkSWVmB2tGJiwpR41hg363aYlls3WSRtenW81Bs8onrtPV1V5w6t-5CvRUS5Q-8ln4rjt5Q6Jlp2vlKBy6wq4zU0zs8VEe5gSXz1SStg79n32j72ok40EAKYZLXADOSIWuSlqjtU1tm2LiBunFeCMnF5CbMPwumqCseMBQstMZQU40tQiyD8Xfp03ejToXw6YBEBaFmH4GbpOtx0dOAsvhTgyWCpGVLE8lGOKCdj-vC53LZYrobMirLSb3068Q1qykElg8wS4Y8DR_boamjuGZINPuU7mF5iKIIUclxCAK5Kg9IgOh7oRkKAW42ZXMdvRRBUhaodzb-KjGjQsZfaHfaITvyXDx01xiMcymmnd-g38u9BAyyHW4VHn5EyQdB2qk7zmSMpoN5bZN290IdWOXK0zHz8MnTPEuokIng47XRL5yXCJrkTF1jo2MRV3f15hlc0rHGfn7sIB4fgY5QwKDWSEsyhUjR_wO_aMwK6CcgZXmVCe86jRf2R9VyZpo11EGoaG8rfIUATJsoN4ZTbJ0u_ye9pTS44ea--_aMsXGS5QAsKuMH_NVuM3Xh-VjSmwXbPg5HvS4zsOTa7sOG7SMKXY9beYdWmO9PQjymYsdSi5zKpSxqdlJ9Fo-Ubz2ct5J_ucbbSysIp-UDz8jii71_I7FsxsbeNZP80vmisRo-98qfCZo-QgkUq5-SddK4bjRwC5WdrcyBoAjwsiM9gkRo2DLzHdFVC49Z53CbV0jZbEjbXI6UBz6T6yeqs8LowPPsOlgXSXZUVj7IzZwy7D60ow5IubuxrkCbx2X2EG9NCD7xjNtUPbB2foAqRfxcEgmu9yz-NkCn-D7TWcbvtBjrlPxnhWcHFV1KT75HW8GTAQ_uRmUuprk7aW_U4GG3Hyd2pGRx0hNFbrk5IqU2ezXn-7DxCL5HjuCMhTFNJcjPaKUyqeR5njcrRHRVbWPsc2MJlvcef5if_Hf112WYqsh5osnBzHdfDMXy_H4vPxagmCKNq06Jo5LSJezSob3Dfx-cdYh8Pa4LeWnBQIh10LI4sACCNnVYM2wBqKWi4sbxGTX8yW0-q0enS-uDrgF4rw6C00BGTeV4pvd-HMCETp41F_qVl84rtfwiuLFinRjIWC4x-b1DAFncBR8y2usKFG5x98o9YZ0lHVtdMjNtipWuLpC1FDKNKR5art986I_8imTCPaziTTdoedzcvGOAVynqqLAnMbPb2yx0ZbTcy_tS1pNtxugRit4bRwCV62PRqOAcaJsF8svkI7F7zRoB2wzns1o932PDn85glDd9MbvxQpNT1xlmqWDXrNtAdHAsrTnc53GnmjMCMj7RcQp1OO2yYMScqs7hUE/master.m3u8
2026-09-15 00:18:36.159  7098-7205  PlayerViewModel         com.sloosh.tv                        D  Scheduling proactive stream refresh in 95s (TTL=120s)
2026-09-15 00:18:36.162  7098-7098  PlayerViewModel         com.sloosh.tv                        D  Stream resolved: masterUrl=https://97-65-e1-r502.vkvideo.cloud/1/i3WCVDltKql2S00dViU9RK0iTrcfBL0NtDolY-y6S93aT7A1mFa5zY52x7bZmV0Ea5FlXgVvJuPUtPl_i2fBQ2-0C-Qm5_LB71Dl8zMqB2_6KbNFrSUrqJ_ia7MXxv95TSu7mDjwRoopUIqqvr4AVHdBL-3eN39VQXG2UBYifgTyJu8pSd37rtq5EGCcLnJ93U3t4EoVf6m6cheeD8TuHMwYLAn1pHK7WUa-6AsgR8Zws2H8vsmqlb4tlxK5jeT-yZQy5uyiQoTDTU4LV9SZo8g_y71bevNvxY-s1ryD8HhjkXwYr5fKY70FX6OjXajmAPwEXIOiQTKOWD0aIeN_A7WGks8urm1l5kNWlOefqbaf4T-ZGY0kRxIXftQyggbT3l5pBnUXw4sqvYwXku2SEOY_cErhx_0HG8hGRhvHfA-dkPXNnitSJsDi6BoqqTZmiiFCw7Xw5H574l5YC8gnKiw1uaZhpHUPp2bZcPTrbnklSaah5FtWTfE1GtNv6SGF7huNNLTLEN65ixtYeB52WSV36NPBvJRFidGGDmqjaXqo3EohRLLXESkwYwl_71tudGXBlwaRx2vXuGlfLA2RIlQfuYueItm7_cXnp-p4P7_Kf_JLDgJsBwANWXHZl1KnLqjVjtHD7-K-dhIE2_9gMW7cqJUBPkC0dw8y9TRCrZy1_SEXDjHNkdezb39vKL1XjdztZcNkFtq0lhhWY0rIVS4VTzHIQjpoyKtypl_vMmQ2BSTkSWVmB2tGJiwpR41hg363aYlls3WSRtenW81Bs8onrtPV1V5w6t-5CvRUS5Q-8ln4rjt5Q6Jlp2vlKBy6wq4zU0zs8VEe5gSXz1SStg79n32j72ok40EAKYZLXADOSIWuSlqjtU1tm2LiBunFeCMnF5CbMPwumqCseMBQstMZQU40tQiyD8Xfp03ejToXw6YBEBaFmH4GbpOtx0dOAsvhTgyWCpGVLE8lGOKCdj-vC53LZYrobMirLSb3068Q1qykElg8wS4Y8DR_boamjuGZINPuU7mF5iKIIUclxCAK5Kg9IgOh7oRkKAW42ZXMdvRRBUhaodzb-KjGjQsZfaHfaITvyXDx01xiMcymmnd-g38u9BAyyHW4VHn5EyQdB2qk7zmSMpoN5bZN290IdWOXK0zHz8MnTPEuokIng47XRL5yXCJrkTF1jo2MRV3f15hlc0rHGfn7sIB4fgY5QwKDWSEsyhUjR_wO_aMwK6CcgZXmVCe86jRf2R9VyZpo11EGoaG8rfIUATJsoN4ZTbJ0u_ye9pTS44ea--_aMsXGS5QAsKuMH_NVuM3Xh-VjSmwXbPg5HvS4zsOTa7sOG7SMKXY9beYdWmO9PQjymYsdSi5zKpSxqdlJ9Fo-Ubz2ct5J_ucbbSysIp-UDz8jii71_I7FsxsbeNZP80vmisRo-98qfCZo-QgkUq5-SddK4bjRwC5WdrcyBoAjwsiM9gkRo2DLzHdFVC49Z53CbV0jZbEjbXI6UBz6T6yeqs8LowPPsOlgXSXZUVj7IzZwy7D60ow5IubuxrkCbx2X2EG9NCD7xjNtUPbB2foAqRfxcEgmu9yz-NkCn-D7TWcbvtBjrlPxnhWcHFV1KT75HW8GTAQ_uRmUuprk7aW_U4GG3Hyd2pGRx0hNFbrk5IqU2ezXn-7DxCL5HjuCMhTFNJcjPaKUyqeR5njcrRHRVbWPsc2MJlvcef5if_Hf112WYqsh5osnBzHdfDMXy_H4vPxagmCKNq06Jo5LSJezSob3Dfx-cdYh8Pa4LeWnBQIh10LI4sACCNnVYM2wBqKWi4sbxGTX8yW0-q0enS-uDrgF4rw6C00BGTeV4pvd-HMCETp41F_qVl84rtfwiuLFinRjIWC4x-b1DAFncBR8y2usKFG5x98o9YZ0lHVtdMjNtipWuLpC1FDKNKR5art986I_8imTCPaziTTdoedzcvGOAVynqqLAnMbPb2yx0ZbTcy_tS1pNtxugRit4bRwCV62PRqOAcaJsF8svkI7F7zRoB2wzns1o932PDn85glDd9MbvxQpNT1xlmqWDXrNtAdHAsrTnc53GnmjMCMj7RcQp1OO2yYMScqs7hUE/master.m3u8, proxyUrl=http://127.0.0.1:8181/proxy/stream.m3u8?url=aHR0cHM6Ly85Ny02NS1lMS1yNTAyLnZrdmlkZW8uY2xvdWQvMS9pM1dDVkRsdEtxbDJTMDBkVmlVOVJLMGlUcmNmQkwwTnREb2xZLXk2UzkzYVQ3QTFtRmE1elk1Mng3YlptVjBFYTVGbFhnVnZKdVBVdFBsX2kyZkJRMi0wQy1RbTVfTEI3MURsOHpNcUIyXzZLYk5GclNVcnFKX2lhN01YeHY5NVRTdTdtRGp3Um9vcFVJcXF2cjRBVkhkQkwtM2VOMzlWUVhHMlVCWWlmZ1R5SnU4cFNkMzdydHE1RUdDY0xuSjkzVTN0NEVvVmY2bTZjaGVlRDhUdUhNd1lMQW4xcEhLN1dVYS02QXNnUjhad3MySDh2c21xbGI0dGx4SzVqZVQteVpReTV1eWlRb1REVFU0TFY5U1pvOGdfeTcxYmV2TnZ4WS1zMXJ5RDhIaGprWHdZcjVmS1k3MEZYNk9qWGFqbUFQd0VYSU9pUVRLT1dEMGFJZU5fQTdXR2tzOHVybTFsNWtOV2xPZWZxYmFmNFQtWkdZMGtSeElYZnRReWdnYlQzbDVwQm5VWHc0c3F2WXdYa3UyU0VPWV9jRXJoeF8wSEc4aEdSaHZIZkEtZGtQWE5uaXRTSnNEaTZCb3FxVFptaWlGQ3c3WHc1SDU3NGw1WUM4Z25LaXcxdWFaaHBIVVBwMmJaY1BUcmJua2xTYWFoNUZ0V1RmRTFHdE52NlNHRjdodU5OTFRMRU42NWl4dFllQjUyV1NWMzZOUEJ2SlJGaWRHR0RtcWphWHFvM0VvaFJMTFhFU2t3WXdsXzcxdHVkR1hCbHdhUngydlh1R2xmTEEyUklsUWZ1WXVlSXRtN19jWG5wLXA0UDdfS2ZfSkxEZ0pzQndBTldYSFpsMUtuTHFqVmp0SEQ3LUstZGhJRTJfOWdNVzdjcUpVQlBrQzBkdzh5OVRSQ3JaeTFfU0VYRGpITmtkZXpiMzl2S0wxWGpkenRaY05rRnRxMGxoaFdZMHJJVlM0VlR6SElRanBveUt0eXBsX3ZNbVEyQlNUa1NXVm1CMnRHSml3cFI0MWhnMzYzYVlsbHMzV1NSdGVuVzgxQnM4b25ydFBWMVY1dzZ0LTVDdlJVUzVRLThsbjRyanQ1UTZKbHAydmxLQnk2d3E0elUwenM4VkVlNWdTWHoxU1N0Zzc5bjMyajcyb2s0MEVBS1laTFhBRE9TSVd1U2xxanRVMXRtMkxpQnVuRmVDTW5GNUNiTVB3dW1xQ3NlTUJRc3RNWlFVNDB0UWl5RDhYZnAwM2VqVG9YdzZZQkVCYUZtSDRHYnBPdHgwZE9Bc3ZoVGd5V0NwR1ZMRThsR09LQ2RqLXZDNTNMWllyb2JNaXJMU2IzMDY4UTFxeWtFbGc4d1M0WThEUl9ib2FtanVHWklOUHVVN21GNWlLSUlVY2x4Q0FLNUtnOUlnT2g3b1JrS0FXNDJaWE1kdlJSQlVoYW9kemItS2pHalFzWmZhSGZhSVR2eVhEeDAxeGlNY3ltbW5kLWczOHU5QkF5eUhXNFZIbjVFeVFkQjJxazd6bVNNcG9ONWJaTjI5MElkV09YSzB6SHo4TW5UUEV1b2tJbmc0N1hSTDV5WENKcmtURjFqbzJNUlYzZjE1aGxjMHJIR2ZuN3NJQjRmZ1k1UXdLRFdTRXN5aFVqUl93T19hTXdLNkNjZ1pYbVZDZTg2alJmMlI5VnlacG8xMUVHb2FHOHJmSVVBVEpzb040WlRiSjB1X3llOXBUUzQ0ZWEtLV9hTXNYR1M1UUFzS3VNSF9OVnVNM1h
2026-09-15 00:18:36.179  7098-7098  HlsProxy                com.sloosh.tv                        D  Active master URL updated: https://97-65-e1-r502.vkvideo.cloud/1/DSBia41_q9YMk7-O1lgmdsBuHffvGcckmWyjrBTauG1Rm1ak11ekJyNOOxb3ZrFp_l4J9uUO99gLUv2DNBp2hBcDgUgBRnr0yE8GQSJ7sC3gf4XkdfKOCVeJ5kguYLWPaWC5OrL0-GRW57HPTJt1IlHlZj0nloFRS8kVCf4XEiUV5jh5-0i-mG6OYWc2IIXulyc16zSlm8YSqrXmC_XeN5tCBtecjIN_xwBWejfbalpzWVYEv4DynOfah7uZcZnDRXN6uBdranLmRGgYqHgXrQxYFN0L-2WzLZfmDADviypxIhVWO11sYAsd0gMPYTLsNTGP8sa_W3mJZZxA81aq_HKAZhQDIefO9zqC1qpo1yvks1rR6TWfWS_mh6WU7ZvLMe-sw7wlHTVRwYRV5x5uFiN8cB815tVAhbntrM6N_tFPZR3ipTL2ap-IgVAYXUiI288a6z1oNIMfJSOxwOgbXIwJAnsxpGNnqOuU9T2vxz9qNIYmElFUUYi-6U4VB-le9fEERaqoNsEAVRm3cykVZ0HJCfEmLP8Wab9_KqkPQkGKLiSsX4ae0QG0ohHcUJwVfzE0i5jP6Z4ZeKNiTc0efD1H8BUc8KVWigcJniMXaPk1xpzwLV_EfB1t7ITbm7RffOV1TTTjZhBhG1x7g6Dc9xkfLKZj2V2iPDw9TTEBjgZtcNV1v7o5A_XHhpSYG79iVZzpXgYKu1lzvUFoQJsS0AOuMR_eGSc646zS5c2Q6oiPpf0FHOfkmmsNX0yM4pk8rMojGcfU5ryrJJK_GJxvQhMexnfVZgX5jSmbdb2Cqrnqdwx4Sg9e2mnS9TBEd6-3xPnXFOfprT8tydxN6klInM9uZkmvDnVhNyNUpvhJlo2KL8ppaAc5iYu6e1oUr2JW0Ee4azxIdCUUxkCsiVH8NOQisQnfKoEXxl2v8usN-sc1q4vePGFW3yTvqKDW82dyqt_7wluj95h30jL_IR5zM4DeacUkDqLdidpiEkf4RK-ofzOtZvPkY7wmTpqxAtQLKg-ZsUhty7SVl-jg5180pFe8JOr2g8J-OahiWmOJefrqxhy9yTmO1fdU9JFAdpQm7jIh91IE6fbomFYV5qaLALQY3VP9H7bY5Kh-KcyxD8WaOnb0KNiK48FdpASOQuUrmbURWgy3IWYDGogXwzjN-4kONhGyPMgOVRhW1G0_6jPx37E-e0kXjR102DcOkm9FDlJybxkyPJNUmRdmUXvo-yuFVfiUJRpoAPvYoXHuUhSIry5uc_8ZD0vQVvsUw_NZXCT6nG7WQo1zYfDDXCMH_J5GbHamW-7ebr-MumUFh0kNHp9zsMPLrGNJAP2QEhjHdUxf80YgxheYIJYQzKsvSuuiXZOdZ8yC7cGzUp05d2rnuEVNgceDuNu4QeIYWcLoOam-7jxY4rLvQ87-iI-YHNqXznq59EKSMSGOWvNmhPKQxghhbUAROVWQhw0MPdgNs4bdIKIMBWbJtK0vtypLd1xrloSkUT742IzxZe__Tg8ZPt1vwSE7wH0g-_rp72NphNx2NYIM1BGBUSijdAya4i2m2A3Q1r6jqmt6r-_22SafiNNyXSv8fHCAaKQ3R5tUDzAB-kHO6Dpgh-NnUGY5P2ltFXmiTltiUzBiVegQCHh5yHfarnnPop3uFB8a4hWRg7A9iMyXKxd8728sfqyl_jrz4BSisZ4KXDtvSuslL3CYLe-nX3mq2O8kh6RDQYq-vEoy7ICHBHIy6OwUE1efYrrP8EKkYqe-EshbAemxDKguAd5DJkqZWSzn-02GRkRJ4g2kUyKC-t_-XNmYv5T3uhfRUuddHlP5t-oAZqzwMdYMEydyCh_tHcWwbYPZG0SNEPykwudjFu6AcX46KYgoj7j89Lezu1E3DmzoEIHwfDbKUB_8DLH1VjePHRlzCb2jKNjqk5PEghqLGbLJX_j894d-xWZS42pPZYJGgmng5hKe2E2-MnmY22gDD0o37S75DaZWfQ-OClI-k3dSMgkB7o0V6nCUgpMGSSTWxxwiDV8cHdAuzxF2_3Nn5QSnOR-bJT4ys785BkdGpjXzyyHYJ2taEJ_S0m2pr5fqyn1S1HeG03LQizuTxKx2i4M/master.m3u8
2026-09-15 00:18:36.310  7098-7098  HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-15 00:18:36.352  7098-7113  com.sloosh.tv           com.sloosh.tv                        W  Suspending all threads took: 12.976ms
2026-09-15 00:18:36.533  7098-7098  PlayerScreen            com.sloosh.tv                        D  ExoPlayer video quality set to Auto (adaptive)
2026-09-15 00:18:36.892  7098-7205  HlsProxy                com.sloosh.tv                        W  rewriteM3u8: all variants were filtered! Falling back to unfiltered variants.
2026-09-15 00:18:37.318  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 5914KB AllocSpace bytes, 41(2284KB) LOS objects, 49% free, 10MB/21MB, paused 19.042ms,9.152ms total 1.317s
2026-09-15 00:18:38.081  7098-7113  com.sloosh.tv           com.sloosh.tv                        W  Suspending all threads took: 59.854ms
2026-09-15 00:18:40.640  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 15MB AllocSpace bytes, 9(360KB) LOS objects, 49% free, 19MB/38MB, paused 61.326ms,2.418ms total 2.831s
2026-09-15 00:18:41.820  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 29MB AllocSpace bytes, 0(0B) LOS objects, 44% free, 30MB/54MB, paused 1.469ms,31.639ms total 405.693ms
2026-09-15 00:18:42.239  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:18:42.240  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:18:42.240  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:18:42.240  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:18:42.451  7098-7225  DMCodecAdapterFactory   com.sloosh.tv                        I  Creating an asynchronous MediaCodec adapter for track type video
2026-09-15 00:18:42.560  7098-7322  CCodec                  com.sloosh.tv                        D  allocate(c2.android.av1-dav1d.decoder)
2026-09-15 00:18:42.677  7098-7322  ApexCodecsLazy          com.sloosh.tv                        I  Failed to load libcom.android.media.swcodec.apexcodecs.so: dlopen failed: library "libcom.android.media.swcodec.apexcodecs.so" not found
2026-09-15 00:18:42.678  7098-7322  Codec2Client            com.sloosh.tv                        I  Available Codec2 services: "default" "software"
2026-09-15 00:18:42.754  7098-7098  PlayerScreen            com.sloosh.tv                        D  ExoPlayer video quality set to Auto (adaptive)
2026-09-15 00:18:42.782  7098-7322  CCodec                  com.sloosh.tv                        I  setting up 'default' as default (vendor) store
2026-09-15 00:18:43.327  7098-7322  CCodec                  com.sloosh.tv                        I  Created component [c2.android.av1-dav1d.decoder] for [c2.android.av1-dav1d.decoder]
2026-09-15 00:18:43.337  7098-7322  CCodecConfig            com.sloosh.tv                        D  read media type: video/av01
2026-09-15 00:18:43.361  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: algo.buffers.max-count.values
2026-09-15 00:18:43.364  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: output.subscribed-indices.values
2026-09-15 00:18:43.365  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: input.buffers.allocator-ids.values
2026-09-15 00:18:43.366  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: output.buffers.allocator-ids.values
2026-09-15 00:18:43.368  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: algo.buffers.allocator-ids.values
2026-09-15 00:18:43.368  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: output.buffers.pool-ids.values
2026-09-15 00:18:43.370  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: algo.buffers.pool-ids.values
2026-09-15 00:18:43.381  7098-7322  ReflectedParamUpdater   com.sloosh.tv                        D  ignored struct field coded.color-format.locations
2026-09-15 00:18:43.393  7098-7322  CCodecConfig            com.sloosh.tv                        D  ignoring local param raw.size (0xd2001800) as it is already supported
2026-09-15 00:18:43.398  7098-7322  CCodecConfig            com.sloosh.tv                        D  ignoring local param default.color (0x5200180b) as it is already supported
2026-09-15 00:18:43.399  7098-7322  CCodecConfig            com.sloosh.tv                        D  ignoring local param raw.hdr-static-info (0xd200180a) as it is already supported
2026-09-15 00:18:43.419  7098-7322  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:43.420  7098-7322  CCodecConfig            com.sloosh.tv                        D  c2 config diff is Dict {
c2::u32 algo.low-latency.value = 0
c2::u32 coded.pl.level = 36865
c2::u32 coded.pl.profile = 36864
c2::u32 coded.vui.color.matrix = 0
c2::u32 coded.vui.color.primaries = 0
c2::u32 coded.vui.color.range = 2
c2::u32 coded.vui.color.transfer = 0
c2::u32 default.color.matrix = 0
c2::u32 default.color.primaries = 0
c2::u32 default.color.range = 0
c2::u32 default.color.transfer = 0
c2::u32 input.buffers.max-size.value = 2097152
c2::u32 input.delay.value = 0
Buffer input.hdr10-plus-info.value = {
}
string input.media-type.value = "video/av01"
c2::u32 output.delay.value = 4
Buffer output.hdr10-plus-info.value = {
}
string output.media-type.value = "video/raw"
c2::u32 raw.color.matrix = 0
c2::u32 raw.color.primaries = 0
c2::u32 raw.color.range = 2
c2::u32 raw.color.transfer = 0
c2::float raw.hdr-static-info.mastering.blue.x = 0
c2::float raw.hdr-static-info.mastering.blue.y = 0
c2::float raw.hdr-static-info.mastering.green.x = 0
c2::floa
2026-09-15 00:18:43.427  7098-7322  ColorUtils              com.sloosh.tv                        W  expected specified color aspects (2:0:0:0)
2026-09-15 00:18:43.465  7098-7321  MediaCodec              com.sloosh.tv                        I  MediaCodec will operate in async mode
2026-09-15 00:18:43.473  7098-7225  MediaCodec              com.sloosh.tv                        E  Media Quality Service not found.
2026-09-15 00:18:43.481  7098-7321  SurfaceUtils            com.sloosh.tv                        D  connecting to surface 0xe18eafd8, reason connectToSurface
2026-09-15 00:18:43.483  7098-7321  MediaCodec              com.sloosh.tv                        I  [c2.android.av1-dav1d.decoder] setting surface generation to 7268353
2026-09-15 00:18:43.484  7098-7321  SurfaceUtils            com.sloosh.tv                        D  disconnecting from surface 0xe18eafd8, reason connectToSurface(reconnect)
2026-09-15 00:18:43.484  7098-7321  SurfaceUtils            com.sloosh.tv                        D  connecting to surface 0xe18eafd0, reason connectToSurface(reconnect-with-listener)
2026-09-15 00:18:43.490  7098-7322  CCodecBufferChannel     com.sloosh.tv                        I  Using latch times for frame rendered signals - present fences not supported
2026-09-15 00:18:43.494  7098-7322  CCodec                  com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder] buffers are bound to CCodec for this session
2026-09-15 00:18:43.497  7098-7322  CCodecConfig            com.sloosh.tv                        D  no c2 equivalents for log-session-id
2026-09-15 00:18:43.497  7098-7322  CCodecConfig            com.sloosh.tv                        D  no c2 equivalents for color-standard
2026-09-15 00:18:43.498  7098-7322  CCodecConfig            com.sloosh.tv                        D  no c2 equivalents for native-window
2026-09-15 00:18:43.498  7098-7322  CCodecConfig            com.sloosh.tv                        D  no c2 equivalents for native-window-generation
2026-09-15 00:18:43.499  7098-7322  CCodecConfig            com.sloosh.tv                        D  no c2 equivalents for flags
2026-09-15 00:18:43.510  7098-7322  CCodecConfig            com.sloosh.tv                        D  c2 config diff is   c2::u32 default.color.matrix = 1
c2::u32 default.color.primaries = 1
c2::u32 default.color.range = 2
c2::u32 default.color.transfer = 3
c2::u32 input.buffers.max-size.value = 4792320
c2::u32 raw.max-size.height = 1606
c2::u32 raw.max-size.width = 3840
c2::u32 raw.pixel-format.value = 34
c2::u32 raw.size.height = 1606
c2::u32 raw.size.width = 3840
2026-09-15 00:18:43.521  7098-7322  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1107298332.
2026-09-15 00:18:43.521  7098-7322  CCodec                  com.sloosh.tv                        D  client requested max input size 4625280, which is smaller than what component recommended (4792320); overriding with component recommendation.
2026-09-15 00:18:43.521  7098-7322  CCodec                  com.sloosh.tv                        W  This behavior is subject to change. It is recommended that app developers double check whether the requested max input size is in reasonable range.
2026-09-15 00:18:43.521  7098-7322  CCodec                  com.sloosh.tv                        D  encoding statistics level = 0
2026-09-15 00:18:43.521  7098-7322  CCodec                  com.sloosh.tv                        D  setup formats input: AMessage(what = 0x00000000) = {
int32_t height = 1606
int32_t level = 2
int32_t max-input-size = 4792320
string mime = "video/av01"
int32_t profile = 1
int32_t width = 3840
Rect crop(0, 0, 3839, 1605)
}
2026-09-15 00:18:43.522  7098-7322  CCodec                  com.sloosh.tv                        D  setup formats output: AMessage(what = 0x00000000) = {
int32_t android._color-format = 2130708361
int32_t android._video-scaling = 1
int32_t android._dataspace = 281411584
int32_t color-standard = 6
int32_t color-range = 2
int32_t color-transfer = 3
int32_t sar-height = 1
int32_t rotation-degrees = 0
Buffer hdr-static-info = {
00000000:  00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00  ................
00000010:  00 00 00 00 00 00 00 00  00                       .........
}
int32_t sar-width = 1
Rect crop(0, 0, 3839, 1605)
int32_t width = 3840
Buffer hdr10-plus-info = {
}
int32_t height = 1606
int32_t max-height = 1606
int32_t max-width = 3840
string mime = "video/raw"
int32_t color-format = 2130708361
}
2026-09-15 00:18:43.525  7098-7322  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:43.526  7098-7322  CCodecConfig            com.sloosh.tv                        D  c2 config diff is   c2::u32 raw.color.matrix = 1
c2::u32 raw.color.primaries = 1
c2::u32 raw.color.transfer = 3
2026-09-15 00:18:43.528  7098-7322  com.sloosh.tv           com.sloosh.tv                        E  Failed to query component interface for required system resources: 6
2026-09-15 00:18:43.534  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:43.613  7098-7322  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:43.614  7098-7322  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:43.614  7098-7322  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:43.620  7098-7322  C2Store                 com.sloosh.tv                        D  Using DMABUF Heaps
2026-09-15 00:18:43.657  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Created input block pool with allocatorID 16 => poolID 17 - OK (0)
2026-09-15 00:18:43.663  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Query output surface allocator returned 0 params => BAD_INDEX (6)
2026-09-15 00:18:43.703  7098-7322  CCodecBufferChannel     com.sloosh.tv                        I  [c2.android.av1-dav1d.decoder#550] Created output block pool with allocatorID 18 => poolID 29 - OK
2026-09-15 00:18:43.710  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Configured output block pool ids 29 => OK
2026-09-15 00:18:43.716  7098-7322  Codec2-Out...ufferQueue com.sloosh.tv                        D  C2SurfaceSyncMemory created 20(20)
2026-09-15 00:18:43.718  7098-7322  Codec2-Out...ufferQueue com.sloosh.tv                        D  remote graphic buffer migration 0/0
2026-09-15 00:18:43.731  7098-7322  Codec2Client            com.sloosh.tv                        D  setOutputSurface -- failed to set consumer usage (6/BAD_INDEX)
2026-09-15 00:18:43.731  7098-7322  Codec2Client            com.sloosh.tv                        D  setOutputSurface -- generation=7268353 consumer usage=0x900 sync
2026-09-15 00:18:43.762  7098-7322  Codec2Client            com.sloosh.tv                        D  Surface configure completed
2026-09-15 00:18:43.776  7098-7322  DMABUFHEAPS             com.sloosh.tv                        I  Using DMA-BUF heap named: system
2026-09-15 00:18:44.211  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:18:44.213  7098-7225  MediaCodecRenderer      com.sloosh.tv                        W  Format exceeds selected codec's capabilities [id=0, mimeType=video/av01, bitrate=11624047, codecs=av01.0.12M.08, res=3840x1606, color=BT709/Limited range/SDR SMPTE 170M/8/8, fps=23.974, c2.android.av1-dav1d.decoder]
2026-09-15 00:18:44.217  7098-7225  DMCodecAdapterFactory   com.sloosh.tv                        I  Creating an asynchronous MediaCodec adapter for track type audio
2026-09-15 00:18:44.220  7098-7331  CCodec                  com.sloosh.tv                        D  allocate(c2.android.aac.decoder)
2026-09-15 00:18:44.223  7098-7331  CCodec                  com.sloosh.tv                        I  setting up 'default' as default (vendor) store
2026-09-15 00:18:44.335  7098-7331  CCodec                  com.sloosh.tv                        I  Created component [c2.android.aac.decoder] for [c2.android.aac.decoder]
2026-09-15 00:18:44.335  7098-7331  CCodecConfig            com.sloosh.tv                        D  read media type: audio/mp4a-latm
2026-09-15 00:18:44.349  7098-7331  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: algo.buffers.max-count.values
2026-09-15 00:18:44.349  7098-7331  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: output.subscribed-indices.values
2026-09-15 00:18:44.350  7098-7331  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: input.buffers.allocator-ids.values
2026-09-15 00:18:44.351  7098-7331  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: output.buffers.allocator-ids.values
2026-09-15 00:18:44.353  7098-7331  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: algo.buffers.allocator-ids.values
2026-09-15 00:18:44.355  7098-7331  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: output.buffers.pool-ids.values
2026-09-15 00:18:44.360  7098-7331  ReflectedParamUpdater   com.sloosh.tv                        D  extent() != 1 for single value type: algo.buffers.pool-ids.values
2026-09-15 00:18:44.371  7098-7331  CCodecConfig            com.sloosh.tv                        I  query failed after returning 21 values (BAD_INDEX)
2026-09-15 00:18:44.371  7098-7331  CCodecConfig            com.sloosh.tv                        D  c2 config diff is Dict {
c2::u32 coded.aac-packaging.value = 0
c2::u32 coded.bitrate.value = 64000
c2::u32 coded.pl.level = 0
c2::u32 coded.pl.profile = 8192
c2::i32 coding.drc.album-mode.value = 0
c2::float coding.drc.attenuation-factor.value = 1
c2::float coding.drc.boost-factor.value = 1
c2::i32 coding.drc.compression-mode.value = 3
c2::i32 coding.drc.effect-type.value = 3
c2::float coding.drc.encoded-level.value = 0.25
c2::float coding.drc.reference-level.value = -16
c2::u32 input.buffers.max-size.value = 8192
c2::u32 input.delay.value = 0
string input.media-type.value = "audio/mp4a-latm"
c2::u32 output.delay.value = 2
c2::float output.drc.output-loudness.value = 0.25
c2::u32 output.large-frame.max-size = 0
c2::u32 output.large-frame.threshold-size = 0
string output.media-type.value = "audio/raw"
c2::u32 raw.channel-count.value = 1
c2::u32 raw.channel-mask.value = 0
c2::u32 raw.max-channel-count.value = 8
c2::u32 raw.sample-rate.value = 44100
}
2026-09-15 00:18:44.386  7098-7331  MediaCodec              com.sloosh.tv                        I  MediaCodec will operate in async mode
2026-09-15 00:18:44.388  7098-7225  MediaCodec              com.sloosh.tv                        E  Media Quality Service not found.
2026-09-15 00:18:44.389  7098-7331  CCodec                  com.sloosh.tv                        D  [c2.android.aac.decoder] buffers are bound to CCodec for this session
2026-09-15 00:18:44.389  7098-7331  CCodecConfig            com.sloosh.tv                        D  no c2 equivalents for log-session-id
2026-09-15 00:18:44.389  7098-7331  CCodecConfig            com.sloosh.tv                        D  no c2 equivalents for flags
2026-09-15 00:18:44.391  7098-7331  CCodecConfig            com.sloosh.tv                        D  c2 config diff is   c2::u32 raw.channel-count.value = 2
c2::u32 raw.sample-rate.value = 48000
2026-09-15 00:18:44.391  7098-7331  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1107298332.
2026-09-15 00:18:44.391  7098-7331  CCodec                  com.sloosh.tv                        D  encoding statistics level = 0
2026-09-15 00:18:44.392  7098-7331  CCodec                  com.sloosh.tv                        D  setup formats input: AMessage(what = 0x00000000) = {
int32_t aac-drc-album-mode = 0
int32_t aac-drc-boost-level = 127
int32_t aac-drc-cut-level = 127
int32_t aac-drc-effect-type = 3
int32_t aac-encoded-target-level = -1
int32_t aac-max-output-channel_count = 8
int32_t aac-target-ref-level = 64
int32_t bitrate = 64000
int32_t channel-count = 2
int32_t channel-mask = 0
int32_t level = 0
int32_t max-input-size = 8192
int32_t max-output-channel-count = 8
string mime = "audio/mp4a-latm"
int32_t profile = 2
int32_t sample-rate = 48000
}
2026-09-15 00:18:44.392  7098-7331  CCodec                  com.sloosh.tv                        D  setup formats output: AMessage(what = 0x00000000) = {
int32_t aac-drc-album-mode = 0
int32_t aac-drc-boost-level = 127
int32_t aac-drc-cut-level = 127
int32_t aac-drc-effect-type = 3
int32_t aac-drc-output-loudness = -1
int32_t aac-encoded-target-level = -1
int32_t aac-max-output-channel_count = 8
int32_t aac-target-ref-level = 64
int32_t buffer-batch-max-output-size = 0
int32_t buffer-batch-threshold-output-size = 0
int32_t channel-count = 2
int32_t channel-mask = 0
int32_t max-output-channel-count = 8
string mime = "audio/raw"
int32_t sample-rate = 48000
int32_t android._config-pcm-encoding = 2
}
2026-09-15 00:18:44.392  7098-7331  CCodecConfig            com.sloosh.tv                        I  query failed after returning 21 values (BAD_INDEX)
2026-09-15 00:18:44.392  7098-7331  com.sloosh.tv           com.sloosh.tv                        E  Failed to query component interface for required system resources: 6
2026-09-15 00:18:44.396  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:44.559  7098-7331  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:44.559  7098-7331  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:44.560  7098-7331  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:44.562  7098-7331  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.aac.decoder#632] Created input block pool with allocatorID 16 => poolID 18 - OK (0)
2026-09-15 00:18:44.568  7098-7331  CCodecBufferChannel     com.sloosh.tv                        I  [c2.android.aac.decoder#632] Created output block pool with allocatorID 16 => poolID 31 - OK
2026-09-15 00:18:44.569  7098-7331  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.aac.decoder#632] Configured output block pool ids 31 => OK
2026-09-15 00:18:44.776  7098-7331  CCodecConfig            com.sloosh.tv                        D  c2 config diff is   c2::u32 raw.channel-mask.value = 12
2026-09-15 00:18:44.776  7098-7331  CCodecBuffers           com.sloosh.tv                        D  [c2.android.aac.decoder#632:Output[N]] popFromStashAndRegister: at 1000000000000us, output format changed to AMessage(what = 0x00000000) = {
int32_t aac-drc-album-mode = 0
int32_t aac-drc-boost-level = 127
int32_t aac-drc-cut-level = 127
int32_t aac-drc-effect-type = 3
int32_t aac-drc-output-loudness = -1
int32_t aac-encoded-target-level = -1
int32_t aac-max-output-channel_count = 8
int32_t aac-target-ref-level = 64
int32_t buffer-batch-max-output-size = 0
int32_t buffer-batch-threshold-output-size = 0
int32_t channel-count = 2
int32_t channel-mask = 12
int32_t max-output-channel-count = 8
string mime = "audio/raw"
int32_t sample-rate = 48000
int32_t android._config-pcm-encoding = 2
}
2026-09-15 00:18:45.667  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 14MB AllocSpace bytes, 20(18MB) LOS objects, 49% free, 24MB/48MB, paused 24.575ms,25.487ms total 1.026s
2026-09-15 00:18:45.798  7098-7322  CCodecConfig            com.sloosh.tv                        D  c2 config diff is   c2::u32 raw.crop.height = 1606
c2::u32 raw.crop.left = 0
c2::u32 raw.crop.top = 0
c2::u32 raw.crop.width = 3840
2026-09-15 00:18:46.115   650-909   AppOps                  system_server                        E  attributionTag  not declared in manifest of com.sloosh.tv
2026-09-15 00:18:46.118   650-2857  AppOps                  system_server                        E  attributionTag  not declared in manifest of com.sloosh.tv
2026-09-15 00:18:46.391  7098-7112  com.sloosh.tv           com.sloosh.tv                        I  JIT allocated 54KB for stack maps of void com.sloosh.tv.ui.player.PlayerScreenKt.PlayerScreen(java.lang.String, java.lang.String, kotlin.jvm.functions.Function0, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, com.sloosh.tv.ui.player.PlayerViewModel, androidx.compose.ui.Modifier, androidx.compose.runtime.Composer, int, int)
2026-09-15 00:18:46.391  7098-7112  com.sloosh.tv           com.sloosh.tv                        I  Compiler allocated 8370KB to compile void com.sloosh.tv.ui.player.PlayerScreenKt.PlayerScreen(java.lang.String, java.lang.String, kotlin.jvm.functions.Function0, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, com.sloosh.tv.ui.player.PlayerViewModel, androidx.compose.ui.Modifier, androidx.compose.runtime.Composer, int, int)
2026-09-15 00:18:49.587  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 186/191 (recycle/alloc) - 5/190 (fetch/transfer)
2026-09-15 00:18:49.649  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 4/9 (recycle/alloc) - 5/15 (fetch/transfer)
2026-09-15 00:18:49.750  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:49.751  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:49.752  7098-7321  CCodecConfig            com.sloosh.tv                        D  c2 config diff is   c2::u32 coded.vui.color.matrix = 1
c2::u32 coded.vui.color.primaries = 1
c2::u32 coded.vui.color.transfer = 3
c2::u32 raw.surface-scaling.value = 2
2026-09-15 00:18:49.755  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:49.755  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:49.755  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:49.843  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:49.843  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:50.426  7098-7322  CCodecBuffers           com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550:2D-Output] popFromStashAndRegister: at 1000003211544us, output format changed to AMessage(what = 0x00000000) = {
int32_t android._color-format = 2130708361
int32_t android._video-scaling = 2
int32_t android._dataspace = 260
int32_t color-standard = 1
int32_t color-range = 2
int32_t color-transfer = 3
int32_t sar-height = 1
int32_t rotation-degrees = 0
Buffer hdr-static-info = {
00000000:  00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00  ................
00000010:  00 00 00 00 00 00 00 00  00                       .........
}
int32_t sar-width = 1
Rect crop(0, 0, 3839, 1605)
int32_t width = 3840
Buffer hdr10-plus-info = {
}
int32_t height = 1606
int32_t max-height = 1606
int32_t max-width = 3840
string mime = "video/raw"
int32_t color-format = 2130708361
}
2026-09-15 00:18:51.324  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (14); ignored
2026-09-15 00:18:51.324  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 16, frameIndex = 14
2026-09-15 00:18:51.324  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (15); ignored
2026-09-15 00:18:51.324  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 16, frameIndex = 15
2026-09-15 00:18:51.330  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:51.337  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:51.339  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:51.340  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:51.340  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:52.484  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:52.484  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:53.948  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:53.950  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:53.965  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:53.966  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:53.969  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:54.646  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:54.647  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:55.002  7098-7113  com.sloosh.tv           com.sloosh.tv                        W  Suspending all threads took: 8.000ms
2026-09-15 00:18:55.114  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 2(16384 size) used buffers - 445/450 (recycle/alloc) - 5/448 (fetch/transfer)
2026-09-15 00:18:55.516  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 25/30 (recycle/alloc) - 6/51 (fetch/transfer)
2026-09-15 00:18:55.568  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:55.570  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:55.573  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:55.573  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:55.573  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:55.882  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 2800KB AllocSpace bytes, 23(1064KB) LOS objects, 33% free, 47MB/71MB, paused 12.865ms,4.987ms total 1.546s
2026-09-15 00:18:56.264  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:56.265  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:56.462  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000007382377, 1000009175833)
2026-09-15 00:18:56.654  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000009175833, 1000009217544)
2026-09-15 00:18:56.712  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:56.715  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:56.727  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:56.729  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:56.730  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:56.765  7098-7112  com.sloosh.tv           com.sloosh.tv                        I  Compiler allocated 5997KB to compile void com.sloosh.tv.ui.player.PlayerScreenKt$PlayerScreen$17$15.invoke(androidx.compose.animation.AnimatedVisibilityScope, androidx.compose.runtime.Composer, int)
2026-09-15 00:18:56.918  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:56.919  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:57.203  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:57.205  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (39); ignored
2026-09-15 00:18:57.205  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 40, frameIndex = 39
2026-09-15 00:18:57.207  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:57.211  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:57.212  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:57.212  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:57.331  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:57.332  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:57.414  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000010135122, 1000011011000)
2026-09-15 00:18:57.942  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:57.946  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:57.949  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:57.952  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:57.952  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:58.122  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:58.122  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:58.488  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (52); ignored
2026-09-15 00:18:58.488  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 54, frameIndex = 52
2026-09-15 00:18:58.488  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (53); ignored
2026-09-15 00:18:58.488  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 54, frameIndex = 53
2026-09-15 00:18:58.489  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:58.490  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:58.493  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:58.493  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:58.494  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:58.601  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:58.602  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:59.112  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:18:59.113  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:18:59.143  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:18:59.151  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:18:59.165  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:18:59.477  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:18:59.477  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:18:59.731  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000011970288, 1000012846166)
2026-09-15 00:19:00.381  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 692/697 (recycle/alloc) - 5/696 (fetch/transfer)
2026-09-15 00:19:01.058  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 69/74 (recycle/alloc) - 6/127 (fetch/transfer)
2026-09-15 00:19:01.128  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:01.131  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:01.133  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:01.133  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:01.133  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:01.276  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:01.277  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:01.997  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000012887877, 1000014681333)
2026-09-15 00:19:02.320  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:02.320  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:02.320  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:02.320  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:02.325  7098-7098  PlayerScreen            com.sloosh.tv                        D  ExoPlayer video quality constrained to height<=1080 (1080p)
2026-09-15 00:19:03.497  7098-7113  com.sloosh.tv           com.sloosh.tv                        W  Suspending all threads took: 8.701ms
2026-09-15 00:19:03.611  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 6800KB AllocSpace bytes, 19(868KB) LOS objects, 27% free, 64MB/88MB, paused 11.160ms,3.212ms total 253.051ms
2026-09-15 00:19:05.593  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 936/941 (recycle/alloc) - 5/940 (fetch/transfer)
2026-09-15 00:19:06.467  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 83/88 (recycle/alloc) - 7/153 (fetch/transfer)
2026-09-15 00:19:06.555  7098-7113  com.sloosh.tv           com.sloosh.tv                        W  Suspending all threads took: 10.422ms
2026-09-15 00:19:06.589  7098-7145  com.sloosh.tv           com.sloosh.tv                        W  Weak pointer dereference blocked for 18 milliseconds.
2026-09-15 00:19:06.590  7098-7098  com.sloosh.tv           com.sloosh.tv                        W  Weak pointer dereference blocked for 26 milliseconds.
2026-09-15 00:19:07.807  7098-7117  HWUI                    com.sloosh.tv                        I  Davey! duration=1075ms; Flags=0, FrameTimelineVsyncId=146609, IntendedVsync=7182717828728, Vsync=7183134495378, InputEventId=470099118, HandleInputStart=7183151759880, AnimationStart=7183151780780, PerformTraversalsStart=7183755978580, DrawStart=7183756068680, FrameDeadline=7182751162060, FrameStartTime=7183150683180, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=7183789382280, SyncStart=7183789620580, IssueDrawCommandsStart=7183789732180, SwapBuffers=7183792549080, FrameCompleted=7183793891780, DequeueBufferDuration=21400, QueueBufferDuration=595900, GpuCompleted=7183793783780, SwapBuffersCompleted=7183793891780, DisplayPresentTime=0, CommandSubmissionCompleted=7183792549080,
2026-09-15 00:19:08.418  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 1084KB AllocSpace bytes, 10(364KB) LOS objects, 20% free, 90MB/114MB, paused 15.092ms,20.368ms total 2.490s
2026-09-15 00:19:10.854  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 1183/1188 (recycle/alloc) - 5/1187 (fetch/transfer)
2026-09-15 00:19:11.987  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 90/95 (recycle/alloc) - 8/167 (fetch/transfer)
2026-09-15 00:19:13.603  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:13.606  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:13.608  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:13.608  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:13.608  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:14.539  7098-7098  PlayerScreen            com.sloosh.tv                        D  ExoPlayer video quality constrained to height<=720 (720p)
2026-09-15 00:19:14.541  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:14.541  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:14.542  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:14.543  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:14.995  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:14.997  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:16.382  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 1442/1447 (recycle/alloc) - 5/1446 (fetch/transfer)
2026-09-15 00:19:16.899  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 5752KB AllocSpace bytes, 13(588KB) LOS objects, 17% free, 109MB/133MB, paused 5.558ms,3.220ms total 2.525s
2026-09-15 00:19:16.977  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (92); ignored
2026-09-15 00:19:16.978  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 94, frameIndex = 92
2026-09-15 00:19:16.979  7098-7321  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] MediaCodec discarded an unknown buffer
2026-09-15 00:19:16.982  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:16.991  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:16.995  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:16.997  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:16.997  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:17.571  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 5(23961600 size) used buffers - 105/110 (recycle/alloc) - 10/192 (fetch/transfer)
2026-09-15 00:19:17.574  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:17.575  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:18.546  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000027110422, 1000030739044)
2026-09-15 00:19:19.276  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000030739044, 1000030780755)
2026-09-15 00:19:19.322  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (98); ignored
2026-09-15 00:19:19.322  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 100, frameIndex = 98
2026-09-15 00:19:19.322  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:19.327  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:19.331  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:19.331  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:19.332  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:19.427  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:19.427  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:19.530  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000030780755, 1000033033000)
2026-09-15 00:19:19.922  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000033033000, 1000033074711)
2026-09-15 00:19:19.980  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:19.982  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:19.993  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:19.994  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:19.994  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:20.139  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:20.139  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:20.366  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:20.367  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:20.370  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:20.370  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:20.371  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:20.472  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:20.472  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:20.696  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:20.697  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:20.698  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:20.699  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:20.699  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:20.894  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:20.902  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:21.509  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:21.509  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:21.512  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:21.512  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:21.512  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:21.666  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 1690/1695 (recycle/alloc) - 5/1694 (fetch/transfer)
2026-09-15 00:19:21.761  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:21.761  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:22.687  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 142/147 (recycle/alloc) - 10/257 (fetch/transfer)
2026-09-15 00:19:23.405  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:23.407  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:23.413  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:23.414  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:23.414  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:23.855  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:23.855  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:23.937  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000035535500, 1000037162122)
2026-09-15 00:19:24.099  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (138); ignored
2026-09-15 00:19:24.099  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 140, frameIndex = 138
2026-09-15 00:19:24.099  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:24.101  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:24.104  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:24.105  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:24.105  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:24.268  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:24.268  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:24.390  7098-7098  Choreographer           com.sloosh.tv                        I  Skipped 39 frames!  The application may be doing too much work on its main thread.
2026-09-15 00:19:24.472  7098-7118  HWUI                    com.sloosh.tv                        I  Davey! duration=734ms; Flags=0, FrameTimelineVsyncId=149781, IntendedVsync=7199734494714, Vsync=7200384494688, InputEventId=0, HandleInputStart=7200396967180, AnimationStart=7200396986080, PerformTraversalsStart=7200446367780, DrawStart=7200446456980, FrameDeadline=7200417828020, FrameStartTime=7200390828280, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=7200448644980, SyncStart=7200448789080, IssueDrawCommandsStart=7200449544480, SwapBuffers=7200453696980, FrameCompleted=7200468738880, DequeueBufferDuration=20700, QueueBufferDuration=220000, GpuCompleted=7200468738880, SwapBuffersCompleted=7200454981780, DisplayPresentTime=0, CommandSubmissionCompleted=7200453696980,
2026-09-15 00:19:24.886  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:24.886  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:24.889  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:24.889  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:24.889  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:25.002  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:25.002  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:25.315  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:25.319  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:25.323  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:25.324  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:25.325  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:25.524  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:25.524  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:25.830  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:25.833  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:25.836  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:25.837  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:25.838  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:25.887  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:25.887  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:26.286  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:26.289  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:26.293  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:26.295  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:26.295  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:26.411  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:26.411  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:26.469  7098-7113  com.sloosh.tv           com.sloosh.tv                        W  Suspending all threads took: 11.557ms
2026-09-15 00:19:26.576  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000039581211, 1000039914877)
2026-09-15 00:19:26.724  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (171); ignored
2026-09-15 00:19:26.724  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 172, frameIndex = 171
2026-09-15 00:19:26.726  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:26.728  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:26.731  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:26.731  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:26.731  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:26.834  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:26.834  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:26.894  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000039956588, 1000040373666)
2026-09-15 00:19:26.970  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 1938/1943 (recycle/alloc) - 5/1942 (fetch/transfer)
2026-09-15 00:19:27.375  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (180); ignored
2026-09-15 00:19:27.377  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 182, frameIndex = 180
2026-09-15 00:19:27.377  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (181); ignored
2026-09-15 00:19:27.377  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 182, frameIndex = 181
2026-09-15 00:19:27.378  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:27.379  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:27.385  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:27.386  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:27.386  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:27.448  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:27.448  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:27.562  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000040582211, 1000040832455)
2026-09-15 00:19:27.829  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 207/212 (recycle/alloc) - 10/371 (fetch/transfer)
2026-09-15 00:19:27.883  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (186); ignored
2026-09-15 00:19:27.884  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 188, frameIndex = 186
2026-09-15 00:19:27.886  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:27.889  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:27.892  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:27.892  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:27.894  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:27.934  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:27.934  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:28.091  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (192); ignored
2026-09-15 00:19:28.092  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 193, frameIndex = 192
2026-09-15 00:19:28.094  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:28.098  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:28.102  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:28.106  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:28.106  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:28.115  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:28.115  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:28.693  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:28.693  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:28.694  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1-dav1d.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:28.694  7098-7225  MediaCodecInfo          com.sloosh.tv                        D  NoSupport [sizeAndRate.support, 3840x1606@23.974000930786133] [c2.android.av1.decoder, video/av01] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-15 00:19:28.695  7098-7098  PlayerScreen            com.sloosh.tv                        D  ExoPlayer video quality constrained to height<=1080 (1080p)
2026-09-15 00:19:29.297  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 5868KB AllocSpace bytes, 25(1100KB) LOS objects, 15% free, 135MB/159MB, paused 15.702ms,4.935ms total 3.558s
2026-09-15 00:19:30.829  7098-7327  PipelineWatcher         com.sloosh.tv                        D  onInputBufferReleased: frameIndex not found (209); ignored
2026-09-15 00:19:30.829  7098-7327  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Ignoring stale input buffer done callback: last flush index = 211, frameIndex = 209
2026-09-15 00:19:30.830  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:30.832  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:30.836  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:30.836  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:30.836  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:31.078  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:31.078  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:31.941  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000042292255, 1000044502788)
2026-09-15 00:19:32.288  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 2188/2193 (recycle/alloc) - 5/2192 (fetch/transfer)
2026-09-15 00:19:33.141  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 239/244 (recycle/alloc) - 11/429 (fetch/transfer)
2026-09-15 00:19:33.158  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000044502788, 1000044544500)
2026-09-15 00:19:33.191  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:33.195  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:33.199  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:33.199  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:33.199  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:33.486  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:33.486  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:34.237  7098-7113  com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 3096KB AllocSpace bytes, 19(796KB) LOS objects, 13% free, 157MB/181MB, paused 1.032ms,2.812ms total 1.326s
2026-09-15 00:19:36.753  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:36.755  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:36.757  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:36.757  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:36.757  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:37.163  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:37.163  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:37.639  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 2439/2444 (recycle/alloc) - 5/2443 (fetch/transfer)
2026-09-15 00:19:38.651  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 253/258 (recycle/alloc) - 13/453 (fetch/transfer)
2026-09-15 00:19:38.685  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:38.689  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:38.692  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:38.694  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:38.695  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:38.867  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:38.867  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:40.591  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:40.594  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:40.597  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:40.598  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:40.598  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:40.817  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:40.817  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:41.388  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000052343955, 1000054137422)
2026-09-15 00:19:42.416  7098-7321  VideoRende...ityTracker com.sloosh.tv                        W  Rendered frame is earlier than the next expected frame (1000054137422, 1000054179122)
2026-09-15 00:19:42.467  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:42.469  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:42.473  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:42.474  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:42.476  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:42.927  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:42.927  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
2026-09-15 00:19:42.950  7098-7331  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0x82ee8fb8 : 5(40960 size) total buffers - 1(8192 size) used buffers - 2687/2692 (recycle/alloc) - 5/2691 (fetch/transfer)
2026-09-15 00:19:44.866  7098-7327  BufferPoolAccessor2.0   com.sloosh.tv                        D  bufferpool2 0xe8f91138 : 5(23961600 size) total buffers - 4(19169280 size) used buffers - 274/279 (recycle/alloc) - 15/489 (fetch/transfer)
2026-09-15 00:19:44.921  7098-7225  MediaCodec              com.sloosh.tv                        D  keep callback message for reclaim
2026-09-15 00:19:44.923  7098-7321  CCodecConfig            com.sloosh.tv                        I  query failed after returning 16 values (BAD_INDEX)
2026-09-15 00:19:44.927  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1342179345.
2026-09-15 00:19:44.928  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 2415921170.
2026-09-15 00:19:44.928  7098-7321  Codec2Client            com.sloosh.tv                        W  query -- param skipped: index = 1610614798.
2026-09-15 00:19:45.533  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] flushed work; ignored.
2026-09-15 00:19:45.533  7098-7322  CCodecBufferChannel     com.sloosh.tv                        D  [c2.android.av1-dav1d.decoder#550] Discard frames from previous generation.
