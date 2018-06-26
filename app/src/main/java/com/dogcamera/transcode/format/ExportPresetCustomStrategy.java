package com.dogcamera.transcode.format;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

public class ExportPresetCustomStrategy implements MediaFormatStrategy {

    private static final String TAG = "ExportPresetXxYStrategy";

    public ExportPresetCustomStrategy() {
    }
    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger("width");
        int height = inputFormat.getInteger("height");
        MediaFormat outputFormat = MediaFormatPresets.getExportPress1280x720(width, height);
        int outWidth = outputFormat.getInteger("width");
        int outHeight = outputFormat.getInteger("height");
        Log.d(TAG, String.format("input: %dx%d => output: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        // Use original sample rate, as resampling is not supported yet.
        String mime = inputFormat.getString(MediaFormat.KEY_MIME);
        int sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int audioChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int audioBitrate = inputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        int aacProfile = inputFormat.getInteger(MediaFormat.KEY_AAC_PROFILE);
        final MediaFormat format = MediaFormat.createAudioFormat(mime, sampleRate, audioChannels);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, aacProfile);
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);
        return format;
    }
}
