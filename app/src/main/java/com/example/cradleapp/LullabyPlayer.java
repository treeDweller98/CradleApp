package com.example.cradleapp;

import android.content.ContentResolver;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LullabyPlayer {

    private final String HAPPY_LULLABY_FILENAME = "here_comes_the_sun.m4a";
    private final String NEUTRAL_LULLABY_FILENAME = "fur_elise.m4a";
    private final String DISTRESSED_LULLABY_FILENAME = "scourge_of_iron.m4a";

    private final int HAPPY_LULLABY_RESID = R.raw.here_comes_the_sun;
    private final int NEUTRAL_LULLABY_RESID = R.raw.fur_elise;
    private final int DISTRESSED_LULLABY_RESID = R.raw.scourge_of_iron;

    private final Uri happyMusicUri, neutralMusicUri, distressedMusicUri;

    private final String CHAR_PLAY = "⏵";
    private final String CHAR_PLAY_PAUSE = "⏯";
    private final String CHAR_PAUSE = "⏸";

    private final MainActivity mainActivity;
    private MediaPlayer  mediaPlayer;
    ScheduledExecutorService track_timer;
    String duration;

    TextView tv_trackTitle;
    TextView tv_trackTime;
    SeekBar  seekbar;
    Button   btn_play;
    Button   btn_playHappyMusic;
    Button   btn_playNeutralMusic;
    Button   btn_playDistressedMusic;

    public LullabyPlayer( MainActivity mainActivity ) {
        this.mainActivity = mainActivity;

        tv_trackTitle = mainActivity.findViewById(R.id.tv_track_title);
        tv_trackTime  = mainActivity.findViewById(R.id.tv_track_time);
        seekbar       = mainActivity.findViewById(R.id.seekbar);
        btn_play      = mainActivity.findViewById(R.id.btn_play);
        btn_playHappyMusic      = mainActivity.findViewById(R.id.btn_play_happy);
        btn_playNeutralMusic    = mainActivity.findViewById(R.id.btn_play_neutral);
        btn_playDistressedMusic = mainActivity.findViewById(R.id.btn_play_distress);

        btn_play.setText(CHAR_PLAY_PAUSE); btn_play.setEnabled(false);

        happyMusicUri = getRawUri(HAPPY_LULLABY_RESID);
        neutralMusicUri = getRawUri(NEUTRAL_LULLABY_RESID);
        distressedMusicUri = getRawUri(DISTRESSED_LULLABY_RESID);

        btn_play.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                        btn_play.setText(CHAR_PLAY);
                        track_timer.shutdown();
                    } else {
                        mediaPlayer.start();
                        btn_play.setText(CHAR_PAUSE);

                        track_timer = Executors.newScheduledThreadPool(1);
                        track_timer.scheduleAtFixedRate(new Runnable() {
                            @Override
                            public void run() {
                                if (mediaPlayer != null) {
                                    if (!seekbar.isPressed()) {
                                        seekbar.setProgress(mediaPlayer.getCurrentPosition());
                                    }
                                }
                            }
                        }, 10, 10, TimeUnit.MILLISECONDS);
                    }
                }
            }
        } );

        btn_playHappyMusic.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v) {
                playHappyMusic();
            }
        } );
        btn_playNeutralMusic.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v) {
                playNeutralMusic();
            }
        } );
        btn_playDistressedMusic.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v) {
                playDistressedMusic();
            }
        } );

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null){
                    int millis = mediaPlayer.getCurrentPosition();
                    long total_secs = TimeUnit.SECONDS.convert(millis, TimeUnit.MILLISECONDS);
                    long mins = TimeUnit.MINUTES.convert(total_secs, TimeUnit.SECONDS);
                    long secs = total_secs - (mins*60);
                    tv_trackTime.setText(mins + ":" + secs + " / " + duration);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekbar.getProgress());
                }
            }
        });
    }

    public void pauseMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btn_play.setText(CHAR_PLAY);
                track_timer.shutdown();
            }
        }
    }
    public void resumeMusic() {
        if (mediaPlayer != null) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                btn_play.setText(CHAR_PAUSE);

                track_timer = Executors.newScheduledThreadPool(1);
                track_timer.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer != null) {
                            if (!seekbar.isPressed()) {
                                seekbar.setProgress(mediaPlayer.getCurrentPosition());
                            }
                        }
                    }
                }, 10, 10, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void playHappyMusic() {
        releaseMusicPlayer();
        createMediaPlayer(happyMusicUri, HAPPY_LULLABY_FILENAME);
        btn_play.performClick();
    }
    public void playNeutralMusic() {
        releaseMusicPlayer();
        createMediaPlayer(neutralMusicUri, NEUTRAL_LULLABY_FILENAME);
        btn_play.performClick();
    }
    public void playDistressedMusic() {
        releaseMusicPlayer();
        createMediaPlayer(distressedMusicUri, DISTRESSED_LULLABY_FILENAME);
        btn_play.performClick();
    }


    private void createMediaPlayer(Uri uri, String filename){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        try {
            mediaPlayer.setDataSource( mainActivity.getApplicationContext(), uri );
            mediaPlayer.prepare();

            tv_trackTitle.setText(filename);
            btn_play.setEnabled(true);

            int millis = mediaPlayer.getDuration();
            long total_secs = TimeUnit.SECONDS.convert(millis, TimeUnit.MILLISECONDS);
            long mins = TimeUnit.MINUTES.convert(total_secs, TimeUnit.SECONDS);
            long secs = total_secs - (mins*60);
            duration = mins + ":" + secs;
            tv_trackTime.setText("00:00 / " + duration);
            seekbar.setMax(millis);
            seekbar.setProgress(0);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    releaseMusicPlayer();
                }
            });
        } catch (IOException e){
            tv_trackTitle.setText(e.toString());
        }
    }

    public void releaseMusicPlayer() {
        if ( mediaPlayer != null ) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if ( track_timer != null ) {
            track_timer.shutdown();
        }
        btn_play.setText(CHAR_PLAY_PAUSE);
        tv_trackTitle.setText("Lullaby Player");
        tv_trackTime.setText("00:00 / 00:00");
        seekbar.setMax(100);
        seekbar.setProgress(0);
    }

    private Uri getRawUri(int resID) {
        return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + File.pathSeparator
                        + File.separator + File.separator
                        + mainActivity.getPackageName()
                        + File.separator + resID
        );
    }
}
