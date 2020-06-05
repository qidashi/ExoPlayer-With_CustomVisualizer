package com.klx.mp3demo.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.TeeAudioProcessor;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;

import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;

public class AudioPlayer implements MediaSourceEventListener{

    private final Timeline.Window window;
    private final Timeline.Period period;
    private  long[] adGroupTimesMs;
    private  boolean[] playedAdGroups;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    public AudioDataReceiver audioDataReceiver;
    public static AudioDataFetch audioDataFetch;
    public static int sampleRate;
    public static int channels;
    private SimpleExoPlayer player;
    private Handler handler = new Handler();
    private AudioControlListener listener;
    private boolean isPlaying;
    private int position;// 记录当前播放位置-- 音乐列表

    public AudioPlayer(Context context){
        // init player and set a callback to receive audio data
        audioDataReceiver = new AudioDataReceiver();
        setAudioDataFetch(audioDataReceiver);
        CustomRendererFactory rendererFactory  = createRendererFactory(context);
        // init listen seek bar
        player = new SimpleExoPlayer.Builder(context,rendererFactory).build();
        window = new Timeline.Window();
        period = new Timeline.Period();
        adGroupTimesMs = new long[0];
        playedAdGroups = new boolean[0];
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        //
        initDataLoadListener();

    }

    private void initDataLoadListener() {
        player.addListener(new Player.EventListener() {

            @Override
            public void onTimelineChanged(Timeline timeline, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups,
                                        TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                if (isLoading) {
                    handler.post(loadStatusRunnable);
                }
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                // 加载网络数据
                if (playbackState == ExoPlayer.STATE_BUFFERING){
//                    progressBar.setVisibility(View.VISIBLE);
                } else {
//                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });
    }

    // create render factory
    private CustomRendererFactory createRendererFactory(Context context) {
        CustomRendererFactory rendererFactory = new CustomRendererFactory(context, new TeeAudioProcessor.AudioBufferSink() {
            int counter = 0;
            @Override
            public void flush(int sampleRateHz, int channelCount, int encoding) {
                // nothing to here
            }
            @Override
            public void handleBuffer(ByteBuffer buffer) {
                counter++;
                if(!audioDataReceiver.isLocked()){
                    audioDataReceiver.setLocked(true);
                    audioDataFetch.setAudioDataAsByteBuffer(buffer.duplicate(),sampleRate,channels);
                }
                else{
                    Log.d("xtm", "handleBuffer: skipped no"+ counter);
                }
            }
        });
        return rendererFactory;
    }

    public void play(Context context, Uri uri,int position){
        this.position = position;
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "Visualizer"));
        final ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        // register listener MediaSourceEventListener
        mediaSource.addEventListener(new Handler(),this);
        //load data source into player
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
        if (listener != null) {
            listener.onPlaying(position, true);
        }
    }

    /**
     * 停止播放
     */
    public void stop(){
        if(player!=null){
            player.seekTo(0);
            player.stop();
        }
    }

    /**
     * 暂停播放
     */
    public void pause(){
        if(player!=null && player.isPlaying()){
            player.setPlayWhenReady(false);
            if (listener != null) {
                listener.onPlaying(position, false);
            }
        }

    }

    /**
     * 暂停恢复播放
     */
    public void resumePlay(){
        if(player!=null && !player.isPlaying() && !player.getPlayWhenReady()){
            player.setPlayWhenReady(true);
            if (listener != null) {
                listener.onPlaying(position, true);
            }
        }
    }

    /**
     * 拖动进度条播放
     * @param position
     */
    public void seekTo(int position){
        if(player!=null){
            player.seekTo(position);
        }
    }

    /**
     * 调用此方法后需要重新初始化播放器
     * 通常在activity onDestroy中调用
     */
    public void destroy(){
        if(player!=null){
            player.release();
        }
    }

    @Override
    public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
        if(mediaLoadData.trackType == TRACK_TYPE_AUDIO){
            channels = mediaLoadData.trackFormat.channelCount;
            sampleRate = mediaLoadData.trackFormat.sampleRate;
        }
    }

    public void setAudioDataFetch(AudioDataFetch audioDataFetch) {
        this.audioDataFetch = audioDataFetch;
    }


    public interface AudioDataFetch{
        void setAudioDataAsByteBuffer(ByteBuffer buffer, int sampleRate, int channelCount);
    }

    private Runnable loadStatusRunnable = new Runnable() {
        @Override
        public void run() {
            long durationUs = 0;
            int adGroupCount = 0;
            long currentWindowTimeBarOffsetMs = 0;
            Timeline currentTimeline = player.getCurrentTimeline();
            if (!currentTimeline.isEmpty()) {
                int currentWindowIndex = player.getCurrentWindowIndex();
                int firstWindowIndex = currentWindowIndex;
                int lastWindowIndex = currentWindowIndex;
                for (int i = firstWindowIndex; i <= lastWindowIndex; i++) {
                    if (i == currentWindowIndex) {
                        currentWindowTimeBarOffsetMs = C.usToMs(durationUs);
                    }
                    currentTimeline.getWindow(i, window);
                    if (window.durationUs == C.TIME_UNSET) {
//                       /**/ Assertions.checkState(!multiWindowTimeBar);
                        break;
                    }
                    for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                        currentTimeline.getPeriod(j, period);
                        int periodAdGroupCount = period.getAdGroupCount();
                        for (int adGroupIndex = 0; adGroupIndex < periodAdGroupCount;
                             adGroupIndex++) {
                            long adGroupTimeInPeriodUs = period.getAdGroupTimeUs(adGroupIndex);
                            if (adGroupTimeInPeriodUs == C.TIME_END_OF_SOURCE) {
                                if (period.durationUs == C.TIME_UNSET) {
                                    // Don't show ad markers for postrolls in periods with
                                    // unknown duration.
                                    continue;
                                }
                                adGroupTimeInPeriodUs = period.durationUs;
                            }
                            long adGroupTimeInWindowUs = adGroupTimeInPeriodUs + period
                                    .getPositionInWindowUs();
                            if (adGroupTimeInWindowUs >= 0
                                    && adGroupTimeInWindowUs <= window.durationUs) {
                                if (adGroupCount == adGroupTimesMs.length) {
                                    int newLength = adGroupTimesMs.length == 0 ? 1
                                            : adGroupTimesMs.length * 2;
                                    adGroupTimesMs = Arrays.copyOf(adGroupTimesMs, newLength);
                                    playedAdGroups = Arrays.copyOf(playedAdGroups, newLength);
                                }
                                adGroupTimesMs[adGroupCount] = C
                                        .usToMs(durationUs + adGroupTimeInWindowUs);
                                playedAdGroups[adGroupCount] = period
                                        .hasPlayedAdGroup(adGroupIndex);
                                adGroupCount++;
                            }
                        }
                    }
                    durationUs += window.durationUs;
                }
            }

            durationUs = C.usToMs(window.durationUs);
            long currentTime = currentWindowTimeBarOffsetMs + player.getContentPosition();
            long bufferedPosition = currentWindowTimeBarOffsetMs + player
                    .getContentBufferedPosition();
            if (listener != null) {
                if(currentTime>durationUs){
                    currentTime = durationUs;
                }
                listener.onBufferedDuration(position, bufferedPosition);
                listener.onCurDuration(position, currentTime,"" + Util.getStringForTime(formatBuilder, formatter, currentTime));
                listener.onDuration(position, durationUs,"" + Util.getStringForTime(formatBuilder, formatter, durationUs));
            }
            handler.removeCallbacks(loadStatusRunnable);
            int playbackState = player == null ? Player.STATE_IDLE
                    : player.getPlaybackState();
            //播放器未开始播放后者播放器播放结束
            if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
                long delayMs = 0;
                //当正在播放状态时
                if (player.getPlayWhenReady() && playbackState == Player.STATE_READY) {
                    float playBackSpeed = player.getPlaybackParameters().speed;
                    if (playBackSpeed <= 0.1f) {
                        delayMs = 1000;
                    } else if (playBackSpeed <= 5f) {
                        //中间更新周期时间
                        long mediaTimeUpdatePeriodMs = 1000 / Math
                                .max(1, Math.round(1 / playBackSpeed));
                        //当前进度时间与中间更新周期之间的多出的不足一个中间更新周期时长的时间
                        long surplusTimeMs = currentTime % mediaTimeUpdatePeriodMs;
                        //播放延迟时间
                        long mediaTimeDelayMs = mediaTimeUpdatePeriodMs - surplusTimeMs;
                        if (mediaTimeDelayMs < (mediaTimeUpdatePeriodMs / 5)) {
                            mediaTimeDelayMs += mediaTimeUpdatePeriodMs;
                        }
                        delayMs = playBackSpeed == 1 ? mediaTimeDelayMs
                                : (long) (mediaTimeDelayMs / playBackSpeed);
                        Log.e("AUDIO_CONTROL", "playBackSpeed<=5:" + delayMs);
                    } else {
                        delayMs = 200;
                    }
                } else {
                    //当暂停状态时
                    delayMs = 1000;
                }
                handler.postDelayed(this, delayMs);
            } else {
                if (listener != null) {
                    //播放完结
                    listener.onPlaying(position, false);
                    listener.onPlayComplete(position);
                }
                isPlaying = false;
            }
        }
    };

    public void setOnAudioControlListener(AudioControlListener listener) {
        this.listener = listener;
    }
    public interface AudioControlListener {
        void onDuration(int position, long durationTime, String durationTimeString);// 音频总时长
        void onCurDuration(int position, long curPositionTime, String curTimeString);// 播放的当前时长
        void onBufferedDuration(int position, long bufferedPosition);// 缓冲到的当前时长
        void onPlaying(int position, boolean isPlaying);// 音频播放状态
        void onPlayComplete(int position);// 播放完毕
    }
}
