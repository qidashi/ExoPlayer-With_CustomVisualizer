package com.klx.mp3demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.klx.mp3demo.player.AudioPlayer;
import com.klx.mp3demo.player.BarVisualizer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements AudioPlayer.AudioControlListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private AudioPlayer audioPlayer;
    private SeekBar seekBar;
    private final int RADIO_MAX_PROGRESS_VALUE = 100000;
    private long mTotalDuration;
    private BarVisualizer barVisualizer;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private View view;

    /**
     * 选取音乐
     */
    private static final int SELECT_MUSIC = 0x00;
    /**
     * 求情读文件权限
     */
    private static final int REQUEST_READ_STORAGE = 0x01;
    /**
     * 请求读取文件
     */
    private static final int REQUEST_RECORD_AUDIO = 0X01;
    private Button btn_select_music;
    private TextView tv_select_music;
    private Button btn_start;
    private Uri uri;

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

        barVisualizer.setiColor(new BarVisualizer.IColor() {
            @Override
            public void showColor(int color) {
                view.setBackgroundColor(color);
            }
        });
    }

    private void initData() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            }
        }

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
        uri = createUri(this, R.raw.sample_music);
        audioPlayer.play(this, uri, 0);
        // 音乐律动条
        barVisualizer.show();
        audioPlayer.audioDataReceiver.setAudioDataListener(barVisualizer);
        // 单曲循环
        audioPlayer.repeat();

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
        view = findViewById(R.id.view);



        btn_select_music = (Button) findViewById(R.id.btn_select_music);
        tv_select_music = (TextView) findViewById(R.id.tv_select_music);
        btn_start = (Button) findViewById(R.id.btn_start);

        btn_select_music.setOnClickListener(this);
        btn_start.setOnClickListener(this);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                finish();
            }
        }
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                finish();
            }
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select_music:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, SELECT_MUSIC);
                break;
            case R.id.btn_start:
                if(uri!=null){
                    audioPlayer.play(this, uri, 0);
                    // 音乐律动条
                    barVisualizer.show();
                    audioPlayer.audioDataReceiver.setAudioDataListener(barVisualizer);
                    // 单曲循环
                    audioPlayer.repeat();
                }else {
                    Toast.makeText(this, "哥们，文件好像有问题.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (resultCode == Activity.RESULT_OK) {
            uri = data.getData();
            tv_select_music.setText(uri.getPath());
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

}
