package com.dogcamera.transcode.format;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.util.Log;

public class ExportPreset640x360Strategy implements MediaFormatStrategy {
    private static final String TAG = "ExportPreset640x360Strategy";

    ExportPreset640x360Strategy() {
    }

    @SuppressLint("LongLogTag")
    @TargetApi(16)
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger("width");
        int height = inputFormat.getInteger("height");
        MediaFormat outputFormat = MediaFormatPresets.getExportPreset640x360(width, height);
        int outWidth = outputFormat.getInteger("width");
        int outHeight = outputFormat.getInteger("height");
        Log.d(TAG, String.format("input: %dx%d => output: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }
}

