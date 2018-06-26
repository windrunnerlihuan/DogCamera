/*
 * Copyright (C) 2014 Yuya Tanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dogcamera.transcode.format;

import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

// Refer for example: https://gist.github.com/wobbals/3990442
// Refer for preferred parameters: https://developer.apple.com/library/ios/documentation/networkinginternet/conceptual/streamingmediaguide/UsingHTTPLiveStreaming/UsingHTTPLiveStreaming.html#//apple_ref/doc/uid/TP40008332-CH102-SW8
// Refer for available keys: (ANDROID ROOT)/media/libstagefright/ACodec.cpp
public class MediaFormatPresets {
    private static final int LONGER_LENGTH_640x360 = 640;
    private static final int LONGER_LENGTH_960x540 = 960;
    private static final int LONGER_LENGTH_1280x720 = 1280;
    private static final int VIDEO_BIRATE = 3000 * 1024;

    private MediaFormatPresets() {
    }

    /**
     * Adapter to honor 6(H60-L01 and H60-L12)
     *
     * @param originalWidth  Input video width.
     * @param originalHeight Input video height.
     * @return MediaFormat instance, or null if pass through.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static MediaFormat getExportPreset640x360(int originalWidth, int originalHeight) {
        int longerLength = Math.max(originalWidth, originalHeight);
        int shorterLength = Math.min(originalWidth, originalHeight);

        int width, height;

        if (longerLength > LONGER_LENGTH_640x360) {

            int scaledShorter = LONGER_LENGTH_640x360 * shorterLength / longerLength;

            if (originalWidth >= originalHeight) {
                width = LONGER_LENGTH_640x360;
                height = scaledShorter;
            } else {
                width = scaledShorter;
                height = LONGER_LENGTH_640x360;
            }
        } else {
            width = originalWidth;
            height = originalHeight;
        }

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 3000 * 1024);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);
        return format;
    }


    // preset similar to iOS SDK's AVAssetExportPreset960x540
    @Deprecated
    public static MediaFormat getExportPreset960x540() {
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 960, 540);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 5500 * 1000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        return format;
    }

    /**
     * Preset similar to iOS SDK's AVAssetExportPreset960x540.
     * Note that encoding resolutions of this preset are not supported in all devices e.g. Nexus 4.
     * On unsupported device encoded video stream will be broken without any exception.
     * @param originalWidth Input video width.
     * @param originalHeight Input video height.
     * @return MediaFormat instance, or null if pass through.
     */
    public static MediaFormat getExportPreset960x540(int originalWidth, int originalHeight) {
        int longerLength = Math.max(originalWidth, originalHeight);
        int shorterLength = Math.min(originalWidth, originalHeight);

        if (longerLength <= LONGER_LENGTH_960x540) return null; // don't upscale

        int residue = LONGER_LENGTH_960x540 * shorterLength % longerLength;
        if (residue != 0) {
            double ambiguousShorter = (double) LONGER_LENGTH_960x540 * shorterLength / longerLength;
            throw new OutputFormatUnavailableException(String.format(
                    "Could not fit to integer, original: (%d, %d), scaled: (%d, %f)",
                    longerLength, shorterLength, LONGER_LENGTH_960x540, ambiguousShorter));
        }

        int scaledShorter = LONGER_LENGTH_960x540 * shorterLength / longerLength;
        int width, height;
        if (originalWidth >= originalHeight) {
            width = LONGER_LENGTH_960x540;
            height = scaledShorter;
        } else {
            width = scaledShorter;
            height = LONGER_LENGTH_960x540;
        }

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 5500 * 1000);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        return format;
    }

    public static MediaFormat getExportPress1280x720(int originalWidth, int originalHeight) {
        int longerLength = Math.max(originalWidth, originalHeight);
        int shorterLength = Math.min(originalWidth, originalHeight);

        int width, height;
        if (longerLength > LONGER_LENGTH_1280x720) {
            int scaledShorter = LONGER_LENGTH_1280x720 * shorterLength / longerLength;
            if (originalWidth >= originalHeight) {
                width = LONGER_LENGTH_1280x720;
                height = scaledShorter;
            } else {
                width = scaledShorter;
                height = LONGER_LENGTH_1280x720;
            }
        } else {
            width = originalWidth;
            height = originalHeight;
        }

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 3000 * 1024);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        return format;
    }

    public static MediaFormat getExportPresetXxY(int originalWidth, int originalHeight){
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", originalWidth, originalHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIRATE);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);
        return format;
    }

    public static MediaFormat getExportPresetCustomAudio(){
        final MediaFormat format = MediaFormat.createAudioFormat(MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC,
                44100, 1);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        return format;
    }
}
