# Implementation Plan: Process Completion Percentage for Media Operations

This plan outlines the steps to implement and display process completion percentages for all media operations (FFmpeg-based) in the app.

## User Review Required

> [!NOTE]
> The progress percentage will be calculated based on the output duration vs the total duration of the source media. For operations with multiple inputs (like merging), the duration of the primary input will be used as a reference.

## Proposed Changes

### Media Utility Layer

#### [MODIFY] [MediaUtils.kt](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/src/main/java/com/example/utilityhub/features/media/MediaUtils.kt)
- Add a private helper `getMediaDuration(context: Context, uri: Uri): Long` to retrieve media duration efficiently.
- Add a private helper `executeFFmpegWithProgress(command: String, durationMs: Long, onProgress: (Float) -> Unit): Boolean` to encapsulate FFmpeg execution with progress tracking.
- Update all FFmpeg-based functions (`reduceVideoSize`, `trimVideo`, `mergeAudioFiles`, `muteVideo`, `convertToGif`, `changeVideoSpeed`, `mergeAudioVideo`, `mergeImageAudio`, `extractAudioWithFFmpeg`) to:
    - Accept an optional `onProgress: (Float) -> Unit` parameter.
    - Calculate/retrieve duration and pass it to the helper.

---

### UI Layer

#### [MODIFY] [VideoEditorScreen.kt](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/src/main/java/com/example/utilityhub/features/media/VideoEditorScreen.kt)
- Update tools (`AdCreatorTool`, `EditorTrimmerTool`, `EditorSpeedTool`, `EditorSizeReducerTool`, `EditorToGifTool`, `EditorMuterTool`, `EditorExtractAudioTool`) to:
    - Maintain a `progress` state.
    - Display the percentage text (e.g., "Processing: 45%") when `isBusy` or `isProcessing` is true.
    - For `EditorSizeReducerTool`, add the percentage text next to or below the `LinearProgressIndicator`.

#### [MODIFY] [StudioScreen.kt](file:///C:/Users/KANNAN/AndroidStudioProjects/UtilityHub/app/src/main/java/com/example/utilityhub/features/media/StudioScreen.kt)
- Update `AudioMergerTool` to:
    - Maintain a `progress` state.
    - Display the percentage text during merging.

## Verification Plan

### Automated Tests
- Build the project: `./gradlew assembleDebug`
- Run lint to ensure no new warnings: `./gradlew lint`

### Manual Verification
1. Open the Video Editor.
2. Select "Reduce Size" on a video. Verify that the percentage increases (e.g., 10%, 25%, 50%) along with the progress bar.
3. Try "Trim Video" and verify that "Processing: X%" appears.
4. Try "Merge Audio" in the Studio Screen and verify the percentage appears.
5. Verify that the process still completes successfully and saves the file to the gallery.
