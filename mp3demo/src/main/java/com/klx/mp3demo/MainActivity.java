package com.klx.mp3demo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.klx.mp3demo.player.AudioPlayer;
import com.klx.mp3demo.player.BarVisualizer;

public class MainActivity extends AppCompatActivity implements AudioPlayer.AudioControlListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private AudioPlayer audioPlayer;
    private SeekBar seekBar;
    private final int RADIO_MAX_PROGRESS_VALUE = 100000;
    private long mTotalDuration;
    private BarVisualizer barVisualizer;
    private TextView tvStartTime;
    private TextView tvEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initListen();
    }

    private void initListen() {
        seekBar.setMax(RADIO_MAX_PROGRESS_VALUE);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                double curProgress = 1.0 * seekBar.getProgress() / RADIO_MAX_PROGRESS_VALUE;
                int tempProgress = (int) (mTotalDuration * curProgress);
                audioPlayer.seekTo(tempProgress);

            }
        });
    }

    private void initData() {
        audioPlayer = new AudioPlayer(this);
        audioPlayer.setOnAudioControlListener(this);
    }

    private Uri createUri(Context context, int resId) {
        //create data source from resource
        DataSpec dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(resId));
        final RawResourceDataSource rawResourceDataSource = new RawResourceDataSource(context);
        try {
            rawResourceDataSource.open(dataSpec);
        } catch (RawResourceDataSource.RawResourceDataSourceException e) {
            e.printStackTrace();
        }
        Uri uri = rawResourceDataSource.getUri();
        return uri;
    }

    public void play(View view) {
        Uri uri = createUri(this, R.raw.sample_music);
        audioPlayer.play(this, uri, 0);
        // 音乐律动条
        barVisualizer.show();
        audioPlayer.audioDataReceiver.setAudioDataListener(barVisualizer);

    }

    public void stop(View view) {
        audioPlayer.stop();
    }

    public void pause(View view) {
        audioPlayer.pause();
    }

    public void resume(View view) {
        audioPlayer.resumePlay();
    }

    private void initView() {
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        barVisualizer = (BarVisualizer) findViewById(R.id.bar_visualizer);
        tvStartTime = (TextView) findViewById(R.id.tv_start_time);
        tvEndTime = (TextView) findViewById(R.id.tv_end_time);
    }

    @Override
    public void onDuration(int position, long durationTime, String durationTimeString) {
        Log.e(TAG, "durationTimeString:" + durationTimeString);
        this.mTotalDuration = durationTime;
        tvEndTime.setText(durationTimeString);
    }

    @Override
    public void onCurDuration(int position, long curPositionTime, String curTimeString) {
        Log.e(TAG, "curTimeString:" + curTimeString);
        if (mTotalDuration != 0) {
            double progress = curPositionTime * 1.0 / mTotalDuration * 1.0;
            int curProgress = (int) (RADIO_MAX_PROGRESS_VALUE * progress);
            seekBar.setProgress(curProgress);
            tvStartTime.setText(curTimeString);
        }
    }

    @Override
    public void onBufferedDuration(int position, long bufferedPosition) {
        Log.e(TAG, "onBufferedDuration: " + position + " bufferedPosition" + bufferedPosition);
    }

    @Override
    public void onPlaying(int position, boolean isPlaying) {
        Log.e(TAG, "onPlaying: " + position + " isPlaying:" + isPlaying);
        if (!isPlaying) {
            barVisualizer.hide();
        } else {
            barVisualizer.show();
        }
    }

    @Override
    public void onPlayComplete(int position) {
        Log.e(TAG, "onPlayComplete: " + position);
        barVisualizer.hide();
        seekBar.setProgress(0);
        tvStartTime.setText("00:00");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioPlayer != null) {
            audioPlayer.destroy();
        }
    }
}
