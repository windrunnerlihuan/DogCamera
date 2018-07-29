package com.dogcamera.utils;


import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.dogcamera.transcode.MediaTranscoder;
import com.dogcamera.transcode.engine.RenderConfig;
import com.dogcamera.transcode.format.MediaFormatStrategy;
import com.dogcamera.transcode.format.MediaFormatStrategyPresets;
import com.dogcamera.transcode.utils.VideoDimensionCompat;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by huanli on 2018/2/28.
 */

public class VideoUtils {

    private static final String TAG = "VideoUtils";

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Deprecated
    public static boolean joinVideoForSameCodec(List<String> fileList, String outPath) {
        Iterator<String> fileIterator = fileList.iterator();

        //--------step 1 MediaExtractor拿到多媒体信息，用于MediaMuxer创建文件
        MediaFormat outVideoFormat = null;
        MediaFormat outAudioFormat = null;

        while (fileIterator.hasNext()) {
            String filePath = fileIterator.next();
            MediaExtractor extractor = new MediaExtractor();
            try {
                extractor.setDataSource(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (outVideoFormat == null) {
                int videoTrackIndex = findTrackIndex(extractor, "video/");
                if (videoTrackIndex >= 0) {
                    outVideoFormat = extractor.getTrackFormat(videoTrackIndex);
                }
            }
            if (outAudioFormat == null) {
                int audioTrackIndex = findTrackIndex(extractor, "audio/");
                if (audioTrackIndex >= 0) {
                    outAudioFormat = extractor.getTrackFormat(audioTrackIndex);
                }
            }
            extractor.release();
            if (outVideoFormat != null && outAudioFormat != null) {
                break;
            }
        }
        if (outVideoFormat == null && outAudioFormat == null) {
            Log.e(TAG, "can not found video&audio format");
            return false;
        }
        MediaMuxer mediaMuxer;
        try {
            mediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (outVideoFormat != null) {
            mediaMuxer.addTrack(outVideoFormat);

        }
        if (outAudioFormat != null) {
            mediaMuxer.addTrack(outAudioFormat);
        }
        mediaMuxer.start();

        //--------step 2 遍历文件，MediaExtractor读取帧数据，MediaMuxer写入帧数据，并记录帧信息
        long ptsOffset = 0L;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer buffer = ByteBuffer.allocateDirect(500 * 1024).order(ByteOrder.nativeOrder());

        Iterator<String> mediaIterator = fileList.iterator();

        while (mediaIterator.hasNext()) {
            String mediaPath = mediaIterator.next();
            MediaExtractor mediaExtractor = new MediaExtractor();
            try {
                mediaExtractor.setDataSource(mediaPath);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            boolean hasVideo = outVideoFormat != null;
            int videoTrackIndex = findTrackIndex(mediaExtractor, "video/");
            if (videoTrackIndex < 0) {
                hasVideo = false;
            }
            long videoPtsOffsetTmp = 0L;
            if (hasVideo) {
                mediaExtractor.selectTrack(videoTrackIndex);
                while (true) {
                    int trackIndex = mediaExtractor.getSampleTrackIndex();
                    if (trackIndex < 0) {
                        buffer.clear();
                        bufferInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mediaMuxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                        break;
                    }
                    buffer.clear();
                    int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                    boolean isKeyFrame = (mediaExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
                    int flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
                    bufferInfo.set(0, sampleSize, mediaExtractor.getSampleTime() + ptsOffset, flags);
                    mediaMuxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                    videoPtsOffsetTmp = bufferInfo.presentationTimeUs;
                    mediaExtractor.advance();
                }
                mediaExtractor.unselectTrack(videoTrackIndex);
            }

            boolean hasAudio = outAudioFormat != null;
            int audioTrackIndex = findTrackIndex(mediaExtractor, "audio/");
            if (audioTrackIndex < 0) {
                hasAudio = false;
            }
            long audioPtsOffsetTmp = 0L;
            if (hasAudio) {
                mediaExtractor.selectTrack(audioTrackIndex);
                while (true) {
                    int trackIndex = mediaExtractor.getSampleTrackIndex();
                    if (trackIndex < 0) {
                        buffer.clear();
                        bufferInfo.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mediaMuxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                        break;
                    }
                    buffer.clear();
                    int sampleSize = mediaExtractor.readSampleData(buffer, 0);
                    boolean isKeyFrame = (mediaExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
                    int flags = isKeyFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;
                    bufferInfo.set(0, sampleSize, mediaExtractor.getSampleTime() + ptsOffset, flags);
                    mediaMuxer.writeSampleData(audioTrackIndex, buffer, bufferInfo);
                    audioPtsOffsetTmp = bufferInfo.presentationTimeUs;
                    mediaExtractor.advance();
                }
                mediaExtractor.unselectTrack(audioTrackIndex);
            }
            //记录当前文件的最后一个pts，作为下一个文件的pts offset
            //前一个文件的最后一帧与后一个文件的第一帧，差1ms，只是估计值，不准确，但能用
            if (mediaIterator.hasNext()) {
                ptsOffset = Math.max(videoPtsOffsetTmp, audioPtsOffsetTmp) + 1000L;
            }

            mediaExtractor.release();

        }
        mediaMuxer.stop();
        mediaMuxer.release();
        return true;
    }

    public static int findTrackIndex(MediaExtractor extractor, String mimePrefix) {
        int trackNums = extractor.getTrackCount();
        for (int i = 0; i < trackNums; i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 转码并添加滤镜，一些机型转换成1280x720的视频可能会有花屏、绿屏，需要做适配
     */
    @SuppressLint("NewApi")
    public static void transcodeVideo(String srcPath, String dstPath, RenderConfig config, MediaTranscoder.Listener listener) {
        //FIXME 一些机型需要适配
        //TODO
        MediaFormatStrategy strategy = getAdaptableStrategy();
        try {
            MediaTranscoder.getInstance().transcodeVideoSync(srcPath, dstPath, strategy, config, listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MediaFormatStrategy getAdaptableStrategy() {
        MediaFormatStrategy mediaFormatStrategy = null;
        if (isGreenScreenPhone()) {
            mediaFormatStrategy = MediaFormatStrategyPresets.createExportPresetCompatStategy();
        }
        if (mediaFormatStrategy == null) {
            mediaFormatStrategy = MediaFormatStrategyPresets.createExportPresetCustomStategy();
        }
        return mediaFormatStrategy;
    }

    public static boolean isGreenScreenPhone() {
        String bm = Build.MODEL;
        if (TextUtils.isEmpty(bm))
            return false;
        for (String gsp : VideoDimensionCompat.HUAWEI_GREENSCREEN_PHONE) {
            if (gsp.contains(bm)) {
                return true;
            }
        }
        return false;
    }

    public static boolean joinVideoWithMp4parser(List<String> mp4PathList, String outPutPath){
        boolean ret = false;
        for(int i = 0; i < 3; i++){
            ret = appendMp4List(mp4PathList, outPutPath);
            if(ret)
                break;
            Log.e(TAG, "joinVideoWithMp4parser failed ! go to retry !");
        }
        return ret;
    }

    /**
     * 对Mp4文件集合进行追加合并(按照顺序一个一个拼接起来)
     *
     * @param mp4PathList [输入]Mp4文件路径的集合(支持m4a)(不支持wav)
     * @param outPutPath  [输出]结果文件全部名称包含后缀(比如.mp4)
     * @throws IOException 格式不支持等情况抛出异常
     */
    public static boolean appendMp4List(List<String> mp4PathList, String outPutPath) {


        try {
            List<Movie> mp4MovieList = new ArrayList<>();// Movie对象集合[输入]
            for (String mp4Path : mp4PathList) {// 将每个文件路径都构建成一个Movie对象
                mp4MovieList.add(MovieCreator.build( mp4Path));
            }

            List<Track> audioTracks = new LinkedList<>();// 音频通道集合
            List<Track> videoTracks = new LinkedList<>();// 视频通道集合

            for (Movie mp4Movie : mp4MovieList) {// 对Movie对象集合进行循环
                for (Track inMovieTrack : mp4Movie.getTracks()) {
                    if ("soun".equals(inMovieTrack.getHandler())) {// 从Movie对象中取出音频通道
                        audioTracks.add(inMovieTrack);
                    }
                    if ("vide".equals(inMovieTrack.getHandler())) {// 从Movie对象中取出视频通道
                        videoTracks.add(inMovieTrack);
                    }
                }
            }
            Movie resultMovie = new Movie();// 结果Movie对象[输出]
            if (!audioTracks.isEmpty()) {// 将所有音频通道追加合并
                resultMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }

            if (!videoTracks.isEmpty()) {// 将所有视频通道追加合并
                resultMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }

            Container outContainer = new DefaultMp4Builder().build(resultMovie);// 将结果Movie对象封装进容器
            FileChannel fileChannel = new RandomAccessFile(String.format(outPutPath), "rw").getChannel();
            outContainer.writeContainer(fileChannel);// 将容器内容写入磁盘
            fileChannel.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
