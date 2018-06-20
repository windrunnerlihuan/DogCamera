package com.dogcamera.transcode.format;

import android.annotation.SuppressLint;
import android.media.MediaFormat;
import android.util.Log;

public class ExportPreset1280x720Strategy implements MediaFormatStrategy {
    private static final String TAG = "ExportPreset1280x720Strategy";

    public ExportPreset1280x720Strategy() {
    }

    @SuppressLint("LongLogTag")
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger("width");
        int height = inputFormat.getInteger("height");
        MediaFormat outputFormat = MediaFormatPresets.getExportPress1280x720(width, height);
        int outWidth = outputFormat.getInteger("width");
        int outHeight = outputFormat.getInteger("height");
        Log.d(TAG, String.format("input: %dx%d => output: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }
}
