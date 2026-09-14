2026-09-07 03:39:58.621  3059-3143  Finsky                  com.android.vending                  E  [52] VerifyApps: APK Analysis scan failed for com.sloosh.tv (Fix with AI)
com.google.android.finsky.verifier.apkanalysis.client.ApkAnalysisException: DOWNLOAD_FILE_NOT_FOUND_EXCEPTION while analyzing APK
at ijf.b(PG:571)
at aqtc.u(PG:5)
at aqzq.run(PG:109)
at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1156)
at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:651)
at mfe.run(PG:278)
at java.lang.Thread.run(Thread.java:1119)
2026-09-07 03:40:11.288  3059-3157  Finsky                  com.android.vending                  E  [62] ItemStore: getItems RPC failed for item com.sloosh.tv
2026-09-07 03:40:11.291  3059-3157  Finsky                  com.android.vending                  E  [62] ItemStore: getItems RPC failed for item com.sloosh.tv
2026-09-07 03:40:14.887 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  Late-enabling -Xcheck:jni
2026-09-07 03:40:17.137 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  Using CollectorTypeCMC GC.
2026-09-07 03:40:17.150 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Unexpected CPU variant for x86: x86.
Known variants: atom, sandybridge, silvermont, goldmont, goldmont-plus, goldmont-without-sha-xsaves, tremont, kabylake, alderlake, default
2026-09-07 03:40:17.535 21645-21645 nativeloader            com.sloosh.tv                        D  Load libframework-connectivity-tiramisu-jni.so using APEX ns com_android_tethering for caller /apex/com.android.tethering/javalib/framework-connectivity-t.jar: ok
2026-09-07 03:40:17.822 21645-21645 CompatChangeReporter    com.sloosh.tv                        D  Compat change id reported: 242716250; UID 10093; state: ENABLED
2026-09-07 03:40:23.287 21645-21645 nativeloader            com.sloosh.tv                        D  Configuring clns-7 for other apk /data/app/~~QRh_LprAlaU9IPYpUr5usQ==/com.sloosh.tv-GkHbvJgM-nIIf6GrONE50g==/base.apk. target_sdk_version=34, uses_libraries=, library_path=/data/app/~~QRh_LprAlaU9IPYpUr5usQ==/com.sloosh.tv-GkHbvJgM-nIIf6GrONE50g==/lib/x86, permitted_path=/data:/mnt/expand:/data/user/0/com.sloosh.tv
2026-09-07 03:40:23.330 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c02654) locale list changing from [] to [en-US]
2026-09-07 03:40:23.417 21645-21645 GraphicsEnvironment     com.sloosh.tv                        V  Currently set values for:
2026-09-07 03:40:23.418 21645-21645 GraphicsEnvironment     com.sloosh.tv                        V    angle_gl_driver_selection_pkgs=[]
2026-09-07 03:40:23.418 21645-21645 GraphicsEnvironment     com.sloosh.tv                        V    angle_gl_driver_selection_values=[]
2026-09-07 03:40:23.418 21645-21645 GraphicsEnvironment     com.sloosh.tv                        V  com.sloosh.tv is not listed in per-application setting
2026-09-07 03:40:23.423 21645-21645 GraphicsEnvironment     com.sloosh.tv                        V  ANGLE allowlist from config:
2026-09-07 03:40:23.423 21645-21645 GraphicsEnvironment     com.sloosh.tv                        V  com.sloosh.tv is not listed in ANGLE allowlist or settings, returning default
2026-09-07 03:40:23.424 21645-21645 GraphicsEnvironment     com.sloosh.tv                        V  Neither updatable production driver nor prerelease driver is supported.
2026-09-07 03:40:23.718 21645-21660 DisplayManager          com.sloosh.tv                        I  Choreographer implicitly registered for the refresh rate.
2026-09-07 03:40:23.733 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c07994) locale list changing from [] to [en-US]
2026-09-07 03:40:23.779 21645-21645 ashmem                  com.sloosh.tv                        E  Pinning is deprecated since Android Q. Please use trim or other methods.
2026-09-07 03:40:23.862 21645-21645 DesktopModeFlags        com.sloosh.tv                        D  Toggle override initialized to: OVERRIDE_UNSET
2026-09-07 03:40:23.936 21645-21660 EGL_emulation           com.sloosh.tv                        I  Opening libGLESv1_CM_emulation.so
2026-09-07 03:40:23.951 21645-21660 EGL_emulation           com.sloosh.tv                        I  Opening libGLESv2_emulation.so
2026-09-07 03:40:24.438 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Verification of kotlin.sequences.Sequence kotlin.sequences.SequencesKt___SequencesKt.zip(kotlin.sequences.Sequence, kotlin.sequences.Sequence, kotlin.jvm.functions.Function2) took 476.346ms (48.28 bytecodes/s) (0B arena alloc)
2026-09-07 03:40:24.530 21645-21660 HWUI                    com.sloosh.tv                        W  Failed to choose config with EGL_SWAP_BEHAVIOR_PRESERVED, retrying without...
2026-09-07 03:40:24.532 21645-21660 HWUI                    com.sloosh.tv                        W  Failed to initialize 101010-2 format, error = EGL_SUCCESS
2026-09-07 03:40:24.589 21645-21645 HWUI                    com.sloosh.tv                        W  Unknown dataspace 0
2026-09-07 03:40:26.387 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Method boolean androidx.compose.runtime.snapshots.SnapshotStateList.conditionalUpdate(boolean, kotlin.jvm.functions.Function1) failed lock verification and will run slower.
Common causes for lock verification issues are non-optimized dex code
and incorrect proguard optimizations.
2026-09-07 03:40:26.390 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Method boolean androidx.compose.runtime.snapshots.SnapshotStateList.conditionalUpdate$default(androidx.compose.runtime.snapshots.SnapshotStateList, boolean, kotlin.jvm.functions.Function1, int, java.lang.Object) failed lock verification and will run slower.
2026-09-07 03:40:26.390 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Method java.lang.Object androidx.compose.runtime.snapshots.SnapshotStateList.mutate(kotlin.jvm.functions.Function1) failed lock verification and will run slower.
2026-09-07 03:40:26.392 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Method void androidx.compose.runtime.snapshots.SnapshotStateList.update(boolean, kotlin.jvm.functions.Function1) failed lock verification and will run slower.
2026-09-07 03:40:26.392 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Method void androidx.compose.runtime.snapshots.SnapshotStateList.update$default(androidx.compose.runtime.snapshots.SnapshotStateList, boolean, kotlin.jvm.functions.Function1, int, java.lang.Object) failed lock verification and will run slower.
2026-09-07 03:40:26.597 21645-21677 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v1/anime?page=1&order=NUM_VOTE
2026-09-07 03:40:26.599 21645-21679 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v1/tv/top-rated?page=1
2026-09-07 03:40:26.602 21645-21678 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v1/cartoons?page=1
2026-09-07 03:40:26.607 21645-21676 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v1/cartoons?page=1
2026-09-07 03:40:26.696 21645-21675 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v1/trending?page=1&window=week
2026-09-07 03:40:27.059 21645-21645 Compose Focus           com.sloosh.tv                        D  Owner FocusChanged(true)
2026-09-07 03:40:27.223 21645-21679 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v1/tv/top-rated?page=1 (620ms, unknown-length body)
2026-09-07 03:40:27.241 21645-21675 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v1/trending?page=1&window=week (543ms, unknown-length body)
2026-09-07 03:40:27.255 21645-21676 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v1/cartoons?page=1 (645ms, unknown-length body)
2026-09-07 03:40:27.285 21645-21678 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v1/cartoons?page=1 (682ms, unknown-length body)
2026-09-07 03:40:27.409 21645-21677 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v1/anime?page=1&order=NUM_VOTE (811ms, unknown-length body)
2026-09-07 03:40:27.748 21645-21714 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v1/movies/popular?page=1
2026-09-07 03:40:27.764 21645-21715 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v1/anime?page=1&order=NUM_VOTE
2026-09-07 03:40:27.788 21645-21651 com.sloosh.tv           com.sloosh.tv                        W  Suspending all threads took: 17.728ms
2026-09-07 03:40:27.870 21645-21714 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v1/movies/popular?page=1 (120ms, unknown-length body)
2026-09-07 03:40:27.886 21645-21715 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v1/anime?page=1&order=NUM_VOTE (120ms, unknown-length body)
2026-09-07 03:40:28.537 21645-21655 HWUI                    com.sloosh.tv                        I  Davey! duration=3300ms; Flags=1, FrameTimelineVsyncId=4264509, IntendedVsync=53987837157228, Vsync=53987853823894, InputEventId=0, HandleInputStart=53987865918560, AnimationStart=53987865935360, PerformTraversalsStart=53987865959360, DrawStart=53990272667760, FrameDeadline=53987853823894, FrameStartTime=53987865906360, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=53990436330860, SyncStart=53990446577860, IssueDrawCommandsStart=53990446844260, SwapBuffers=53991092193760, FrameCompleted=53991147437760, DequeueBufferDuration=23600, QueueBufferDuration=746200, GpuCompleted=53991147295460, SwapBuffersCompleted=53991147437760, DisplayPresentTime=0, CommandSubmissionCompleted=53991092193760,
2026-09-07 03:40:29.135 21645-21645 Choreographer           com.sloosh.tv                        I  Skipped 230 frames!  The application may be doing too much work on its main thread.
2026-09-07 03:40:30.154 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Verification of void androidx.compose.ui.graphics.vector.VectorComponent.<init>(androidx.compose.ui.graphics.vector.GroupComponent) took 141.169ms (616.28 bytecodes/s) (0B arena alloc)
2026-09-07 03:40:31.600 21645-21645 Choreographer           com.sloosh.tv                        I  Skipped 145 frames!  The application may be doing too much work on its main thread.
2026-09-07 03:40:32.146 21645-21728 com.sloosh.tv           com.sloosh.tv                        W  Verification of void androidx.profileinstaller.ProfileInstaller.<clinit>() took 622.374ms (24.10 bytecodes/s) (0B arena alloc)
2026-09-07 03:40:32.241 21645-21657 HWUI                    com.sloosh.tv                        I  Davey! duration=6312ms; Flags=0, FrameTimelineVsyncId=4264532, IntendedVsync=53988170490548, Vsync=53992003823728, InputEventId=0, HandleInputStart=53992017681860, AnimationStart=53992017710660, PerformTraversalsStart=53993266814660, DrawStart=53993488225560, FrameDeadline=53991170490428, FrameStartTime=53992008845160, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=53993738023460, SyncStart=53993738097360, IssueDrawCommandsStart=53993738214760, SwapBuffers=53994424340760, FrameCompleted=53994483202360, DequeueBufferDuration=23800, QueueBufferDuration=42914700, GpuCompleted=53994483073960, SwapBuffersCompleted=53994483202360, DisplayPresentTime=0, CommandSubmissionCompleted=53994424340760,
2026-09-07 03:40:33.858 21645-21728 com.sloosh.tv           com.sloosh.tv                        W  Verification of void androidx.profileinstaller.ProfileInstaller.writeProfile(android.content.Context, java.util.concurrent.Executor, androidx.profileinstaller.ProfileInstaller$DiagnosticsCallback, boolean) took 329.880ms (485.02 bytecodes/s) (0B arena alloc)
2026-09-07 03:40:33.865 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Verification of java.lang.Object coil.compose.AsyncImagePainter$onRemembered$1.invokeSuspend(java.lang.Object) took 184.469ms (477.04 bytecodes/s) (0B arena alloc)
2026-09-07 03:40:33.978 21645-21651 com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 5356KB AllocSpace bytes, 2(40KB) LOS objects, 49% free, 7469KB/14MB, paused 30.234ms,5.073ms total 2.545s
2026-09-07 03:40:34.012 21645-21728 ProfileInstaller        com.sloosh.tv                        D  Installing profile for com.sloosh.tv
2026-09-07 03:40:35.219 21645-21657 HWUI                    com.sloosh.tv                        I  Davey! duration=6025ms; Flags=0, FrameTimelineVsyncId=4264649, IntendedVsync=53992053823726, Vsync=53994470490296, InputEventId=0, HandleInputStart=53994516489960, AnimationStart=53994516508060, PerformTraversalsStart=53995385787360, DrawStart=53995386129060, FrameDeadline=53994503823628, FrameStartTime=53994477821160, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=53998044336560, SyncStart=53998044487060, IssueDrawCommandsStart=53998044646260, SwapBuffers=53998076023160, FrameCompleted=53998079470960, DequeueBufferDuration=24200, QueueBufferDuration=403600, GpuCompleted=53998079336960, SwapBuffersCompleted=53998079470960, DisplayPresentTime=0, CommandSubmissionCompleted=53998076023160,
2026-09-07 03:40:35.240 21645-21645 Choreographer           com.sloosh.tv                        I  Skipped 215 frames!  The application may be doing too much work on its main thread.
2026-09-07 03:40:35.459 21645-21645 InsetsController        com.sloosh.tv                        D  hide(ime(), fromIme=false)
2026-09-07 03:40:35.477 21645-21729 HWUI                    com.sloosh.tv                        I  Davey! duration=3812ms; Flags=0, FrameTimelineVsyncId=4264664, IntendedVsync=53994520490294, Vsync=53998103823484, InputEventId=0, HandleInputStart=53998118804860, AnimationStart=53998119214360, PerformTraversalsStart=53998320435060, DrawStart=53998320701660, FrameDeadline=53998103823484, FrameStartTime=53998117574860, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=53998326590660, SyncStart=53998326721360, IssueDrawCommandsStart=53998326853560, SwapBuffers=53998330958860, FrameCompleted=53998333317460, DequeueBufferDuration=22300, QueueBufferDuration=770700, GpuCompleted=53998332853760, SwapBuffersCompleted=53998333317460, DisplayPresentTime=0, CommandSubmissionCompleted=53998330958860,
2026-09-07 03:40:35.478 21645-21645 ImeTracker              com.sloosh.tv                        I  com.sloosh.tv:bc387126: onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2026-09-07 03:40:36.568 21645-21736 HWUI                    com.sloosh.tv                        W  Failed to choose config with EGL_SWAP_BEHAVIOR_PRESERVED, retrying without...
2026-09-07 03:40:36.569 21645-21736 HWUI                    com.sloosh.tv                        W  Failed to initialize 101010-2 format, error = EGL_SUCCESS
2026-09-07 03:40:37.412 21645-21668 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:37.585 21645-21671 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:37.600 21645-21672 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:37.677 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:37.804 21645-21672 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:37.873 21645-21668 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:38.036 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:38.172 21645-21665 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:38.185 21645-21668 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:38.390 21645-21672 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:38.400 21645-21734 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:38.474 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:54.863 21645-21651 com.sloosh.tv           com.sloosh.tv                        I  NativeAlloc concurrent mark compact GC freed 6809KB AllocSpace bytes, 33(664KB) LOS objects, 49% free, 6743KB/13MB, paused 1.576ms,5.798ms total 63.032ms
2026-09-07 03:40:54.875 21645-21650 com.sloosh.tv           com.sloosh.tv                        I  Compiler allocated 4344KB to compile void com.sloosh.tv.ui.home.HomeScreenKt.HomeScreen(kotlin.jvm.functions.Function1, com.sloosh.tv.ui.home.HomeViewModel, androidx.compose.ui.Modifier, androidx.compose.runtime.Composer, int, int)
2026-09-07 03:40:57.190 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:57.223 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:57.272 21645-21734 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:57.330 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:57.384 21645-21734 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:57.415 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:57.842 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:57.959 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:58.206 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:58.241 21645-21734 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:58.347 21645-21672 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:58.366 21645-21734 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:58.535 21645-21645 WindowOnBackDispatcher  com.sloosh.tv                        W  OnBackInvokedCallback is not enabled for the application.
Set 'android:enableOnBackInvokedCallback="true"' in the application manifest.
2026-09-07 03:40:58.807 21645-21715 okhttp.OkHttpClient     com.sloosh.tv                        I  --> GET https://api-sloosh.vercel.app/api/v2/movie/1137844?v=5
2026-09-07 03:40:58.880 21645-21715 okhttp.OkHttpClient     com.sloosh.tv                        I  <-- 200 https://api-sloosh.vercel.app/api/v2/movie/1137844?v=5 (72ms, unknown-length body)
2026-09-07 03:40:59.456 21645-21734 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:59.508 21645-21665 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:59.541 21645-21668 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:59.567 21645-21659 com.sloosh.tv           com.sloosh.tv                        I  Waiting for a blocking GC ProfileSaver
2026-09-07 03:40:59.607 21645-21665 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:59.647 21645-21670 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:59.703 21645-21671 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:40:59.710 21645-21645 Choreographer           com.sloosh.tv                        I  Skipped 35 frames!  The application may be doing too much work on its main thread.
2026-09-07 03:40:59.740 21645-21651 com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 5776KB AllocSpace bytes, 30(604KB) LOS objects, 49% free, 8023KB/15MB, paused 1.488ms,11.747ms total 384.818ms
2026-09-07 03:40:59.744 21645-21659 com.sloosh.tv           com.sloosh.tv                        I  WaitForGcToComplete blocked ProfileSaver on Background for 177.318ms
2026-09-07 03:40:59.784 21645-21656 HWUI                    com.sloosh.tv                        I  Davey! duration=750ms; Flags=0, FrameTimelineVsyncId=4265386, IntendedVsync=54021887155866, Vsync=54021987155862, InputEventId=0, HandleInputStart=54022002289660, AnimationStart=54022002307460, PerformTraversalsStart=54022225233060, DrawStart=54022225304760, FrameDeadline=54022053822526, FrameStartTime=54022001816060, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=54022533339560, SyncStart=54022533479160, IssueDrawCommandsStart=54022560960760, SwapBuffers=54022636166060, FrameCompleted=54022637859660, DequeueBufferDuration=21200, QueueBufferDuration=1039200, GpuCompleted=54022637748860, SwapBuffersCompleted=54022637859660, DisplayPresentTime=0, CommandSubmissionCompleted=54022636166060,
2026-09-07 03:41:00.186 21645-21671 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:41:00.400 21645-21672 HWUI                    com.sloosh.tv                        W  Image decoding logging dropped!
2026-09-07 03:41:00.684 21645-21650 com.sloosh.tv           com.sloosh.tv                        I  Compiler allocated 6341KB to compile void com.sloosh.tv.ui.details.DetailsScreenKt.SidePosterDetailsLayout(com.sloosh.tv.data.api.MediaDetailsDto, com.sloosh.tv.ui.details.DetailsUiState, com.sloosh.tv.ui.details.DetailsViewModel, androidx.compose.ui.focus.FocusRequester, kotlin.jvm.functions.Function0, androidx.compose.runtime.Composer, int, int)
2026-09-07 03:41:00.955 21645-21672 AllohaRepository        com.sloosh.tv                        D  Fetching Alloha catalog: https://api.alloha.tv/?token=ffbd312217e27c4245f2678afe1881&kp=6446910
2026-09-07 03:41:01.619 21645-21672 AllohaRepository        com.sloosh.tv                        D  Alloha API HTTP response: 200 for kp=6446910
2026-09-07 03:41:01.652 21645-21672 CompatChangeReporter    com.sloosh.tv                        D  Compat change id reported: 247079863; UID 10093; state: ENABLED
2026-09-07 03:41:01.695 21645-21672 AllohaRepository        com.sloosh.tv                        D  Successfully parsed Alloha data: title=Мэйдэй, isSerial=false, seasons=0, movieTranslations=3
2026-09-07 03:41:09.599 21645-21645 libc                    com.sloosh.tv                        W  Access denied finding property "vendor.display-size"
2026-09-07 03:41:09.690 21645-21645 ExoPlayerImpl           com.sloosh.tv                        I  Init ca1366b [AndroidXMedia3/1.3.1] [emulator_x86_arm, AOSP TV on x86, Google, 36]
2026-09-07 03:41:09.878 21645-21645 AudioSystem             com.sloosh.tv                        D  onNewService: media.audio_policy service obtained 0x9710dde0
2026-09-07 03:41:09.897 21645-21645 AudioSystem             com.sloosh.tv                        D  getService: checking for service media.audio_policy: 0x9710dde0
2026-09-07 03:41:10.012 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  hiddenapi: Accessing hidden method Landroid/media/AudioTrack;->getLatency()I (runtime_flags=0, domain=platform, api=unsupported) from Landroidx/media3/exoplayer/audio/AudioTrackPositionTracker; (domain=app) using reflection: allowed
2026-09-07 03:41:10.129 21645-21645 AudioSystem             com.sloosh.tv                        D  onNewServiceWithAdapter: media.audio_flinger service obtained 0xf72fbe10
2026-09-07 03:41:10.156 21645-21645 AudioSystem             com.sloosh.tv                        D  getService: checking for service media.audio_flinger: 0x97118280
2026-09-07 03:41:10.350 21645-21729 HWUI                    com.sloosh.tv                        I  Davey! duration=1280ms; Flags=0, FrameTimelineVsyncId=4266093, IntendedVsync=54031937155464, Vsync=54031953822130, InputEventId=356950488, HandleInputStart=54031964945060, AnimationStart=54031964962660, PerformTraversalsStart=54033167605060, DrawStart=54033167835060, FrameDeadline=54031953822130, FrameStartTime=54031964928360, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=54033189595260, SyncStart=54033189724060, IssueDrawCommandsStart=54033189834260, SwapBuffers=54033205946860, FrameCompleted=54033217341960, DequeueBufferDuration=20700, QueueBufferDuration=931500, GpuCompleted=54033217217060, SwapBuffersCompleted=54033217341960, DisplayPresentTime=0, CommandSubmissionCompleted=54033205946860,
2026-09-07 03:41:10.369 21645-21672 AllohaRepository        com.sloosh.tv                        D  Fetching Alloha catalog: https://api.alloha.tv/?token=ffbd312217e27c4245f2678afe1881&kp=1137844
2026-09-07 03:41:10.823 21645-21672 AllohaRepository        com.sloosh.tv                        D  Alloha API HTTP response: 200 for kp=1137844
2026-09-07 03:41:10.828 21645-21672 AllohaRepository        com.sloosh.tv                        W  Alloha response has no 'data' object
2026-09-07 03:41:11.004 21645-21672 HlsProxy                com.sloosh.tv                        D  HLS proxy started on port 8181
2026-09-07 03:41:11.228 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c17354) locale list changing from [] to [en-US]
2026-09-07 03:41:11.229 21645-21645 WebViewFactory          com.sloosh.tv                        I  Loading com.google.android.webview version 143.0.7499.24 (code 749902436)
2026-09-07 03:41:11.232 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c126d4) locale list changing from [] to [en-US]
2026-09-07 03:41:11.233 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c14ad4) locale list changing from [] to [en-US]
2026-09-07 03:41:11.233 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c102d4) locale list changing from [] to [en-US]
2026-09-07 03:41:11.234 21645-21645 ResourcesManager        com.sloosh.tv                        V  The following library key has been added: ResourcesKey{ mHash=448d0b6 mResDir=null mSplitDirs=[] mOverlayDirs=[] mLibDirs=[/data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/WebViewGoogle.apk,/data/app/~~LkxdM7Vltltdp0GslyEtDg==/com.google.android.trichromelibrary_749902436-wbjiGQptMA_6I1mBoH2Tqw==/TrichromeLibrary.apk] mDisplayId=0 mOverrideConfig=v36 mCompatInfo={320dpi always-compat} mLoaders=[]}
2026-09-07 03:41:11.239 21645-21645 com.sloosh.tv           com.sloosh.tv                        W  Failed to find entry 'classes.dex': Entry not found
2026-09-07 03:41:11.243 21645-21645 nativeloader            com.sloosh.tv                        D  Configuring clns-8 for other apk /data/app/~~LkxdM7Vltltdp0GslyEtDg==/com.google.android.trichromelibrary_749902436-wbjiGQptMA_6I1mBoH2Tqw==/TrichromeLibrary.apk. target_sdk_version=36, uses_libraries=ALL, library_path=/data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/lib/x86:/data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/WebViewGoogle.apk!/lib/x86:/data/app/~~LkxdM7Vltltdp0GslyEtDg==/com.google.android.trichromelibrary_749902436-wbjiGQptMA_6I1mBoH2Tqw==/TrichromeLibrary.apk!/lib/x86, permitted_path=/data:/mnt/expand
2026-09-07 03:41:11.259 21645-21645 nativeloader            com.sloosh.tv                        D  Configuring clns-9 for other apk /data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/WebViewGoogle.apk. target_sdk_version=36, uses_libraries=, library_path=/data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/lib/x86:/data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/WebViewGoogle.apk!/lib/x86:/data/app/~~LkxdM7Vltltdp0GslyEtDg==/com.google.android.trichromelibrary_749902436-wbjiGQptMA_6I1mBoH2Tqw==/TrichromeLibrary.apk!/lib/x86, permitted_path=/data:/mnt/expand
2026-09-07 03:41:12.080 21645-21645 cr_WVCFactoryProvider   com.sloosh.tv                        I  version=143.0.7499.24 (749902436) minSdkVersion=29 multiprocess=true packageId=2 splits=<none>
2026-09-07 03:41:12.110 21645-21645 nativeloader            com.sloosh.tv                        D  Load /data/app/~~LkxdM7Vltltdp0GslyEtDg==/com.google.android.trichromelibrary_749902436-wbjiGQptMA_6I1mBoH2Tqw==/TrichromeLibrary.apk!/lib/x86/libmonochrome.so using class loader ns clns-9 (caller=/data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/WebViewGoogle.apk): ok
2026-09-07 03:41:12.145 21645-21645 nativeloader            com.sloosh.tv                        D  Load /system/lib/libwebviewchromium_plat_support.so using class loader ns clns-9 (caller=/data/app/~~8kPQ0Vp8TybRYfhe2oUaug==/com.google.android.webview-8Nof2skfO27wuj9PsVPr6A==/WebViewGoogle.apk): ok
2026-09-07 03:41:12.194 21645-21764 chromium                com.sloosh.tv                        E  [0906/224112.176497:ERROR:android_webview/browser/variations/variations_seed_loader.cc:39] Seed missing signature.
2026-09-07 03:41:12.305 21645-21645 cr_LibraryLoader        com.sloosh.tv                        I  Successfully loaded native library
2026-09-07 03:41:12.331 21645-21645 cr_CachingUmaRecorder   com.sloosh.tv                        I  Flushed 20 samples from 20 histograms, 0 samples were dropped.
2026-09-07 03:41:12.426 21645-21645 cr_CombinedPProvider    com.sloosh.tv                        I  #registerProvider() provider:WV.jk@2539684 isPolicyCacheEnabled:false policyProvidersSize:0
2026-09-07 03:41:12.433 21645-21645 cr_PolicyProvider       com.sloosh.tv                        I  #setManagerAndSource() 0
2026-09-07 03:41:12.462 21645-21645 cr_DisplayManager       com.sloosh.tv                        I  Is Display Topology available: false
2026-09-07 03:41:12.464 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c15a94) locale list changing from [] to [en-US]
2026-09-07 03:41:12.468 21645-21645 com.sloosh.tv           com.sloosh.tv                        I  AssetManager2(0xf6c13454) locale list changing from [] to [en-US]
2026-09-07 03:41:12.711 21645-21645 cr_CombinedPProvider    com.sloosh.tv                        I  #linkNativeInternal() 1
2026-09-07 03:41:12.714 21645-21645 cr_AppResProvider       com.sloosh.tv                        I  #getApplicationRestrictionsFromUserManager() Bundle[EMPTY_PARCEL]
2026-09-07 03:41:12.714 21645-21645 cr_PolicyProvider       com.sloosh.tv                        I  #notifySettingsAvailable() 0
2026-09-07 03:41:12.715 21645-21645 cr_CombinedPProvider    com.sloosh.tv                        I  #onSettingsAvailable() 0
2026-09-07 03:41:12.715 21645-21645 cr_CombinedPProvider    com.sloosh.tv                        I  #flushPolicies()
2026-09-07 03:41:13.591 21645-21645 chromium                com.sloosh.tv                        W  [WARNING:android_webview/browser/network_service/net_helpers.cc:143] HTTP Cache size is: 23303332
2026-09-07 03:41:14.371 21645-21783 cr_media                com.sloosh.tv                        W  BLUETOOTH_CONNECT permission is missing.
2026-09-07 03:41:14.372 21645-21783 cr_media                com.sloosh.tv                        W  getBluetoothAdapter() requires BLUETOOTH permission
2026-09-07 03:41:14.372 21645-21783 cr_media                com.sloosh.tv                        W  registerBluetoothIntentsIfNeeded: Requires BLUETOOTH permission
2026-09-07 03:41:14.489 21645-21799 libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.log"
2026-09-07 03:41:14.501 21645-21799 libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.log.file"
2026-09-07 03:41:14.501 21645-21799 MESA                    com.sloosh.tv                        E  Failed to open rendernode: No such file or directory
2026-09-07 03:41:14.677 21645-21645 Choreographer           com.sloosh.tv                        I  Skipped 211 frames!  The application may be doing too much work on its main thread.
2026-09-07 03:41:14.711 21645-21807 CameraManagerGlobal     com.sloosh.tv                        I  Connecting to camera service
2026-09-07 03:41:14.753 21645-21799 libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.vk.trace.per.submit"
2026-09-07 03:41:14.755 21645-21799 libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.gallium.print.optio"
2026-09-07 03:41:14.757 21645-21799 libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.vk.trace.frame"
2026-09-07 03:41:14.875 21645-21799 libc                    com.sloosh.tv                        W  Access denied finding property "vendor.mesa.vk.wsi.headless.swa"
2026-09-07 03:41:15.236 21645-21729 HWUI                    com.sloosh.tv                        I  Davey! duration=4064ms; Flags=0, FrameTimelineVsyncId=4266202, IntendedVsync=54034037155380, Vsync=54037553821906, InputEventId=0, HandleInputStart=54037555148160, AnimationStart=54037555168560, PerformTraversalsStart=54037564233260, DrawStart=54037571882360, FrameDeadline=54034070488712, FrameStartTime=54037554525160, FrameInterval=16666666, WorkloadTarget=16666666, SyncQueued=54037973150860, SyncStart=54037973285160, IssueDrawCommandsStart=54037973371560, SwapBuffers=54038094337860, FrameCompleted=54038101994660, DequeueBufferDuration=21600, QueueBufferDuration=6836000, GpuCompleted=54038101864960, SwapBuffersCompleted=54038101994660, DisplayPresentTime=0, CommandSubmissionCompleted=54038094337860,
2026-09-07 03:41:17.675 21645-21651 com.sloosh.tv           com.sloosh.tv                        I  Background concurrent mark compact GC freed 5758KB AllocSpace bytes, 32(1000KB) LOS objects, 49% free, 9854KB/19MB, paused 1.861ms,2.397ms total 3.026s
2026-09-07 03:41:18.868 21645-21772 CompatChangeReporter    com.sloosh.tv                        D  Compat change id reported: 263076149; UID 10093; state: ENABLED
2026-09-07 03:41:19.702 21645-21796 VideoCapabilities       com.sloosh.tv                        W  Unrecognized profile/level 0/3 for video/mpeg2
2026-09-07 03:41:20.303 21645-21796 VideoCapabilities       com.sloosh.tv                        W  Unrecognized profile/level 0/3 for video/mpeg2
2026-09-07 03:41:20.334 21645-21796 VideoCapabilities       com.sloosh.tv                        W  Unrecognized profile/level 0/3 for video/mpeg2
2026-09-07 03:41:26.574 21645-21645 chromium                com.sloosh.tv                        I  [INFO:CONSOLE:1] "Uncaught ReferenceError: fileList is not defined", source: https://antipathic-as.stravers.live/build/app.039336e4.js (1)
2026-09-07 03:41:31.174 21645-21645 AllohaResolver          com.sloosh.tv                        W  Timeout task firing at 20s. Checking accumulated payloads...
2026-09-07 03:41:31.178 21645-21645 AllohaResolver          com.sloosh.tv                        E  Resolver error: Таймаут загрузки видеопотока
2026-09-07 03:41:31.257 21645-21645 PlayerViewModel         com.sloosh.tv                        E  initPlayer error: Таймаут загрузки видеопотока (Fix with AI)
java.lang.RuntimeException: Таймаут загрузки видеопотока
at com.sloosh.tv.data.repository.AllohaRuntimeResolver.resolveWithWebView$lambda$12$finishError(AllohaRuntimeResolver.kt:314)
at com.sloosh.tv.data.repository.AllohaRuntimeResolver.access$resolveWithWebView$lambda$12$finishError(AllohaRuntimeResolver.kt:44)
at com.sloosh.tv.data.repository.AllohaRuntimeResolver$resolveWithWebView$2$timeoutTask$1.run(AllohaRuntimeResolver.kt:454)
at android.os.Handler.handleCallback(Handler.java:995)
at android.os.Handler.dispatchMessage(Handler.java:103)
at android.os.Looper.loopOnce(Looper.java:248)
at android.os.Looper.loop(Looper.java:338)
at android.app.ActivityThread.main(ActivityThread.java:9067)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:593)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:932)
2026-09-07 03:41:34.278 21645-21671 AllohaRepository        com.sloosh.tv                        D  Fetching Alloha catalog: https://api.alloha.tv/?token=ffbd312217e27c4245f2678afe1881&kp=1137844
2026-09-07 03:41:34.506 21645-21671 AllohaRepository        com.sloosh.tv                        D  Alloha API HTTP response: 200 for kp=1137844
2026-09-07 03:41:34.519 21645-21671 AllohaRepository        com.sloosh.tv                        W  Alloha response has no 'data' object
2026-09-07 03:41:39.463 21645-21645 chromium                com.sloosh.tv                        I  [INFO:CONSOLE:1] "Uncaught ReferenceError: fileList is not defined", source: https://antipathic-as.stravers.live/build/app.039336e4.js (1)
2026-09-07 03:41:43.939 21645-21650 com.sloosh.tv           com.sloosh.tv                        I  JIT allocated 54KB for stack maps of void com.sloosh.tv.ui.player.PlayerScreenKt.PlayerScreen(java.lang.String, java.lang.String, kotlin.jvm.functions.Function0, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, com.sloosh.tv.ui.player.PlayerViewModel, androidx.compose.ui.Modifier, androidx.compose.runtime.Composer, int, int)
2026-09-07 03:41:43.946 21645-21650 com.sloosh.tv           com.sloosh.tv                        I  Compiler allocated 8449KB to compile void com.sloosh.tv.ui.player.PlayerScreenKt.PlayerScreen(java.lang.String, java.lang.String, kotlin.jvm.functions.Function0, java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String, com.sloosh.tv.ui.player.PlayerViewModel, androidx.compose.ui.Modifier, androidx.compose.runtime.Composer, int, int)
2026-09-07 03:41:54.671 21645-21645 AllohaResolver          com.sloosh.tv                        W  Timeout task firing at 20s. Checking accumulated payloads...
2026-09-07 03:41:54.672 21645-21645 AllohaResolver          com.sloosh.tv                        E  Resolver error: Таймаут загрузки видеопотока
2026-09-07 03:41:54.789 21645-21645 PlayerViewModel         com.sloosh.tv                        E  initPlayer error: Таймаут загрузки видеопотока (Fix with AI)
java.lang.RuntimeException: Таймаут загрузки видеопотока
at com.sloosh.tv.data.repository.AllohaRuntimeResolver.resolveWithWebView$lambda$12$finishError(AllohaRuntimeResolver.kt:314)
at com.sloosh.tv.data.repository.AllohaRuntimeResolver.access$resolveWithWebView$lambda$12$finishError(AllohaRuntimeResolver.kt:44)
at com.sloosh.tv.data.repository.AllohaRuntimeResolver$resolveWithWebView$2$timeoutTask$1.run(AllohaRuntimeResolver.kt:454)
at android.os.Handler.handleCallback(Handler.java:995)
at android.os.Handler.dispatchMessage(Handler.java:103)
at android.os.Looper.loopOnce(Looper.java:248)
at android.os.Looper.loop(Looper.java:338)
at android.app.ActivityThread.main(ActivityThread.java:9067)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:593)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:932)
2026-09-07 03:41:56.484 21645-21671 AllohaRepository        com.sloosh.tv                        D  Fetching Alloha catalog: https://api.alloha.tv/?token=ffbd312217e27c4245f2678afe1881&kp=1137844
2026-09-07 03:41:56.708 21645-21671 AllohaRepository        com.sloosh.tv                        D  Alloha API HTTP response: 200 for kp=1137844
2026-09-07 03:41:56.728 21645-21671 AllohaRepository        com.sloosh.tv                        W  Alloha response has no 'data' object
2026-09-07 03:42:03.440 21645-21645 chromium                com.sloosh.tv                        I  [INFO:CONSOLE:1] "Uncaught ReferenceError: fileList is not defined", source: https://antipathic-as.stravers.live/build/app.039336e4.js (1)
2026-09-07 03:42:16.884 21645-21645 AllohaResolver          com.sloosh.tv                        W  Timeout task firing at 20s. Checking accumulated payloads...
2026-09-07 03:42:16.884 21645-21645 AllohaResolver          com.sloosh.tv                        E  Resolver error: Таймаут загрузки видеопотока
2026-09-07 03:42:16.970 21645-21645 PlayerViewModel         com.sloosh.tv                        E  initPlayer error: Таймаут загрузки видеопотока (Fix with AI)
java.lang.RuntimeException: Таймаут загрузки видеопотока
at com.sloosh.tv.data.repository.AllohaRuntimeResolver.resolveWithWebView$lambda$12$finishError(AllohaRuntimeResolver.kt:314)
at com.sloosh.tv.data.repository.AllohaRuntimeResolver.access$resolveWithWebView$lambda$12$finishError(AllohaRuntimeResolver.kt:44)
at com.sloosh.tv.data.repository.AllohaRuntimeResolver$resolveWithWebView$2$timeoutTask$1.run(AllohaRuntimeResolver.kt:454)
at android.os.Handler.handleCallback(Handler.java:995)
at android.os.Handler.dispatchMessage(Handler.java:103)
at android.os.Looper.loopOnce(Looper.java:248)
at android.os.Looper.loop(Looper.java:338)
at android.app.ActivityThread.main(ActivityThread.java:9067)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:593)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:932)

---

## 2. "Uncaught ReferenceError: fileList is not defined" & Playback Timeout — RESOLVED ✅

### Symptoms & Log Trace
```
2026-09-07 03:41:26.574 chromium: [INFO:CONSOLE:1] "Uncaught ReferenceError: fileList is not defined", source: https://antipathic-as.stravers.live/build/app.039336e4.js (1)
2026-09-07 03:41:31.178 AllohaResolver: Resolver error: Таймаут загрузки видеопотока
2026-09-07 03:41:31.257 PlayerViewModel: initPlayer error: Таймаут загрузки видеопотока
```

### Root Cause Analysis
1. **Direct `loadUrl(cleanUrl)` bypassed `isFramed` check**:
   - The Alloha player HTML contains an anti-framing script:
     `var isFramed=false;try{isFramed=window!=window.top||document!=top.document||self.location!=top.location}catch(e){isFramed=true}if(!isFramed){document.querySelectorAll('body')[0].remove();document.querySelectorAll('html')[0].innerHTML='...Контент не найден...'}`
   - When `cleanUrl` was loaded directly with `wv.loadUrl(cleanUrl)` instead of through `wrapperHtml` (an `<iframe>`), `window == window.top`, causing `isFramed` to evaluate to `false`.
   - The script immediately deleted the entire `<body>` of the document, which contained `<script> const fileList = JSON.parse(...) </script>`.
   - The deferred player script `app.039336e4.js` crashed immediately with `Uncaught ReferenceError: fileList is not defined`.
   - Because of this fatal JS crash, `/bnsi/` was never called, leading to a 20-second timeout.
2. **Broken `/bnsi/` GET Interception**:
   - `shouldInterceptRequest` attempted to intercept `/bnsi/` using an OkHttp GET request.
   - However, `/bnsi/` is an AJAX POST request requiring the `Borth` browser fingerprint header and request body `zl`. A GET request returns `405 Method Not Allowed`.
   - Overriding the request in `shouldInterceptRequest` broke the player's legitimate network request.

### Solution Applied (`AllohaRuntimeResolver.kt`)
1. **Restored `wrapperHtml` with `loadDataWithBaseURL`**:
   - The player is loaded inside an `<iframe>` matching the iOS `AllohaRuntimeResolver.swift` architecture (`window != window.top` evaluates to `true`).
2. **Removed `/bnsi/` Intercept in `shouldInterceptRequest`**:
   - `shouldInterceptRequest` operates purely passively (returning `null`), allowing Chromium to complete legitimate POST requests naturally.
   - `HOOK_JS` catches the response on XHR `load` event and reports `hlsSource` to Kotlin.
3. **Defense-in-depth in `HOOK_JS`**:
   - Overrode `Element.prototype.remove` for `<body>` elements, ensuring that even if an anti-framing check were triggered, `<body>` and `fileList` cannot be deleted.
