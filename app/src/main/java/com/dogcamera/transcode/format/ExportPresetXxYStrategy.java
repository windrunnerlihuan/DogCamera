package com.dogcamera.transcode.format;

import android.media.MediaFormat;
import android.util.Log;

public class ExportPresetXxYStrategy implements MediaFormatStrategy {

    private static final String TAG = "ExportPresetXxYStrategy";

    private int mWidth;
    private int mHeight;

    public ExportPresetXxYStrategy(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }
    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        // TODO: detect non-baseline profile and throw exception
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        MediaFormat outputFormat = MediaFormatPresets.getExportPresetXxY(mWidth, mHeight);
        int outWidth = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int outHeight = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        Log.d(TAG, String.format("inputFormat: %dx%d => outputFormat: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }
}
