package com.dogcamera.transcode.format;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

public class ExportPresetCompatStrategy implements MediaFormatStrategy {

    private static final String TAG = "ExportPresetXxYStrategy";

    public ExportPresetCompatStrategy() {

    }
    
    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int width = inputFormat.getInteger("width");
        int height = inputFormat.getInteger("height");
        MediaFormat outputFormat = MediaFormatPresets.getExportPreset960x540(width, height);
        int outWidth = outputFormat.getInteger("width");
        int outHeight = outputFormat.getInteger("height");
        Log.d(TAG, String.format("input: %dx%d => output: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        // Use original sample rate, as resampling is not supported yet.
        int audioChannels = 1;
        try{
            audioChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        }catch (Exception e){
            //do nothing
        }
        int audioBitrate = 128000;
        try{
            audioBitrate = inputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        }catch (Exception e){
            //do nothing
        }
        final MediaFormat format = MediaFormat.createAudioFormat(MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC,
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), audioChannels);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);
        return format;
    }
}
