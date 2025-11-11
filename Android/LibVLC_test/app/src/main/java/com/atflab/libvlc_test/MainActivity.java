/*
* Project:  vlc-record-live-stream
* Purpose:
* Author:   Ho-Jung Kim (godmode2k@hotmail.com)
* Date:     Since November 3, 2025
*
* modified:    November 11, 2025
* License:
*
*
* Copyright (C) 2025 Ho-Jung Kim (godmode2k@hotmail.com)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*
* Source:
* Note: {
* Reference:
* - LibVLC: https://code.videolan.org/videolan/libvlc-android-samples/
}
* -----------------------------------------------------------------
* TODO:
*
* URGENT!!!
* TODO:
*
*/

package com.atflab.libvlc_test;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.Manifest;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.VLCVideoLayout;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private App m_main_app = null;

    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    private static final String ASSET_FILENAME = "bbb.m4v";
    private static final String m_filename = "/sdcard/Download/bbb.mp4";
    private Uri m_uri_stream = null;
    private String m_stream_url = null;
    private String m_stream_title = null;
    private String m_stream_title_old = null;
    private String m_stream_record_filename = null;

    private VLCVideoLayout mVideoLayout = null;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null; // for display
    private MediaPlayer mMediaPlayer_record_stdout = null; // for record stream (stdout)
    private boolean m_is_pause = false;
    private boolean m_is_recording = false;
    private boolean m_is_play_recorded_file = false;

    //! NOTE:
    // - Do not use 'mMediaPlayer.getVolume()' it always returns 0.
    // - mute: setVolume(0), unmute: setVolume(100) (100: normal, not max volume)
    private int m_current_mediaplayer_volume = 0;
    private boolean m_is_mute = false;

    private Handler m_handler = null;
    private Runnable m_stats_runnable = null;
    private long m_record_total_bytes_read = 0;
    private long m_record_start_millisecond = 0;
    private long m_stream_start_millisecond = 0;
    private long m_seekbar_seektime = 0;
    //private long m_bitrate_last_time_millisecond = 0;

    private ActivityResultLauncher<Intent> m_activity_result_launcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d( TAG, "onCreate()" );

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d( TAG, "onDestroy()" );

        release( false );
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        // LibVLC
//        //{
//        //    if ( mMediaPlayer != null ) {
//        //        mMediaPlayer.attachViews( mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW );
//        //    }
//        //}
//    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d( TAG, "onPause()" );

        release( true );
        setChangePlaybackState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d( TAG, "onResume()" );

        resume();
        setChangePlaybackState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d( TAG, "onRequestPermissionsResult(): permission granted..." );
                }
            }
        }
    }

    private void init() {
        Log.d( TAG, "init()" );

        // ----------------------------------------------
        // TODO:
        // ----------------------------------------------
        // 1. stream address list:
        // // - add/delete/save
        //
        // 2. recorded list:
        // // - add/delete/save
        // // - copy to external-storage
        // // - file size info
        //
        // 3. control view:
        // // - play, pause, stop, record, fullscreen, mute
        // // - recording status(read, total read, duration)
        //
        // 4. recording status (fetching size, ...)
        // // - bytes read, total bytes read
        //
        // 5. Play recorded file
        // // - Playback
        //
        // 6. Is stream seekable? (Live stream, File-based HTTP streaming such as YouTube, mp4/mkv(avc1, ...), )
        // // - seek bar, progress bar
        //
        //
        // ----------------------------------------------
        // FIXME:
        // ----------------------------------------------
        // 1. File-based HTTP stream playback error
        //
        //


        /*
            new Thread(
            () -> {
                func();
            })
            .start();
        */

        m_main_app = (App)getApplicationContext();

        {
            // Permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.INTERNET,
                                Manifest.permission.ACCESS_NETWORK_STATE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        100 );   // 100: my request code
            }
            else {
                Log.d( TAG, "init(): granted already..." );
            }
        }

        {
            // Activity Results
            m_activity_result_launcher = registerForActivityResult( new ActivityResultContracts.StartActivityForResult(), result -> {
                if ( result.getResultCode() == m_main_app.ACTIVITY_RESULT_CODE__ACTIVITY_OPEN_URL ) {
                    release( false );

                    // Open URL
                    Intent intent = result.getData();
                    final String url = intent.getStringExtra( m_main_app.ACTIVITY_OPEN_URL__INTENT_KEY__URL );
                    final String title = intent.getStringExtra( m_main_app.ACTIVITY_OPEN_URL__INTENT_KEY__TITLE );

                    m_stream_url = url;
                    m_stream_title = title;
                    m_stream_title_old = title;

                    Log.d( TAG, "registerForActivityResult(): result: OpenURL: title = " + title + ", url = " + url );

                    // LibVLC
                    {
                        stop_recording();

                        m_seekbar_seektime = 0;
                        m_stream_start_millisecond = 0;

                        // SEE: m_uri_stream (Uri.parse(url))
                        playback_new( url );
                    }
                }
                else if ( result.getResultCode() == m_main_app.ACTIVITY_RESULT_CODE__ACTIVITY_RECORDED_LIST ) {
                    release( false );

                    // RecordedList
                    Intent intent = result.getData();
                    final String filename = intent.getStringExtra( m_main_app.ACTIVITY_RECORDED_LIST__INTENT_KEY__FILENAME );

                    m_stream_url = filename;
                    m_stream_title = filename;
                    m_stream_title_old = filename;
                    m_is_play_recorded_file = true;

                    Log.d( TAG, "registerForActivityResult(): RecordedList: result: filename = " + filename );

                    // LibVLC
                    {
                        stop_recording();

                        m_seekbar_seektime = 0;
                        m_stream_start_millisecond = 0;

                        // SEE: m_uri_stream (Uri.parse(url))
                        playback_new( filename );
                    }
                }
            });
        }

        // LibVLC
        {
            // test
            //m_stream_url = "https://ebsonair.ebs.co.kr/ebs1familypc/familypc1m/playlist.m3u8";
            //m_uri_stream = Uri.parse("https://ebsonair.ebs.co.kr/ebs1familypc/familypc1m/playlist.m3u8");

            // public test videos
            // Source: https://gist.github.com/jsturgis/3b19447b304616f18657

            try {
                // init
                final ArrayList<String> args = new ArrayList<>();
                //args.add("-vvv");
                mLibVLC = new LibVLC(this, args );
                mMediaPlayer = new MediaPlayer( mLibVLC );
                mVideoLayout = findViewById( R.id.video_layout );

                //mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);

                /*
                //final Media media = new Media( mLibVLC, getAssets().openFd(ASSET_FILENAME) );
                //final Media media = new Media( mLibVLC, m_filename );
                final Media media = new Media( mLibVLC, m_uri_stream );

                //media.setHWDecoderEnabled(true, false );

                mMediaPlayer.setMedia( media );
                media.release();
                */
            } catch (Exception e) {
                e.printStackTrace();
            }
            //mMediaPlayer.play();
        }

        VLCVideoLayout video_layout = (VLCVideoLayout) findViewById( R.id.video_layout );
        //LinearLayout control_layout = (LinearLayout) findViewById( R.id.control_layout );

        ImageView button_open_url = (ImageView) findViewById(R.id.Button_open_url);
        ImageView button_play_pause = (ImageView) findViewById(R.id.Button_play_pause);
        ImageView button_stop = (ImageView) findViewById(R.id.Button_stop);
        ImageView button_mute = (ImageView) findViewById(R.id.Button_mute);
        ImageView button_record = (ImageView) findViewById(R.id.Button_record);
        ImageView button_record_list = (ImageView) findViewById(R.id.Button_recorded_list);
        ImageView button_fullscreen = (ImageView) findViewById(R.id.Button_fullscreen);
        SeekBar seekbar = (SeekBar) findViewById( R.id.SeekBar );
        TextView tv_seekbar_position = (TextView) findViewById( R.id.TextView_seekbar_position );

        if ( video_layout != null ) {
            video_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    show_control_ui( false, false );
                }
            });
        }

        if ( button_open_url != null ) {
            button_open_url.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), Activity_OpenURL.class);
                    m_activity_result_launcher.launch( intent );
                }
            });
        }

        if ( button_play_pause != null ) {
            button_play_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    if ( mMediaPlayer == null ) {
                        Log.d( TAG, "button_play_pause: MediaPlayer == NULL" );
                        return;
                    }

                    if ( m_stream_url == null ) {
                        Log.d( TAG, "button_play_pause: stream url == NULL" );
                        return;
                    }

                    if ( mMediaPlayer.isReleased() ) {
                        Log.d( TAG, "button_play_pause: MediaPlayer is released... ignore... init..." );

                        //button_play_pause.setText( "Pause");
                        button_play_pause.setImageResource(R.mipmap.ic_media_pause);
                        //mMediaPlayer.play();
                        playback_new(m_stream_url);
                    }
                    else {
                        if (mMediaPlayer.isPlaying()) {
                            //button_play_pause.setText( "Play");
                            button_play_pause.setImageResource(R.mipmap.ic_media_play);
                            mMediaPlayer.pause();
                            m_seekbar_seektime = mMediaPlayer.getTime();
                        } else {
                            //button_play_pause.setText( "Pause");
                            button_play_pause.setImageResource(R.mipmap.ic_media_pause);
                            //mMediaPlayer.play();
                            playback_new(m_stream_url);
                        }
                    }
                }
            });
        }

        if ( button_stop != null ) {
            button_stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    if ( mMediaPlayer == null ) {
                        Log.d( TAG, "button_stop: MediaPlayer == NULL" );
                        return;
                    }

                    if ( m_stream_url == null ) {
                        Log.d( TAG, "button_stop: stream url == NULL" );
                        return;
                    }

                    if ( mMediaPlayer.isReleased() ) {
                        Log.d( TAG, "button_stop: MediaPlayer is released..." );
                        return;
                    }

                    set_play_info( true );
                    stop_recording();

                    mMediaPlayer.stop();
                    //mMediaPlayer.detachViews();
                    //mMediaPlayer.release();

                    m_seekbar_seektime = 0;
                    m_stream_start_millisecond = 0;

                    seekbar_update( true );

                    // clear view
                    if ( mMediaPlayer.getVLCVout() != null ) {
                        mMediaPlayer.getVLCVout().detachViews();

                        // store all layout child
                        // remove all views
                        // restore all layout child
                        ArrayList<View> view_list = new ArrayList<View>();
                        if ( view_list != null ) {
                            for (int i = 0; i < mVideoLayout.getChildCount(); i++) {
                                View v = mVideoLayout.getChildAt(i);
                                if (view_list != null) view_list.add(v);
                            }
                            if ( !view_list.isEmpty() ) {
                                mVideoLayout.removeAllViewsInLayout();
                                for (int i = 0; i < view_list.size(); i++) {
                                    View v = view_list.get(i);
                                    mVideoLayout.addView(v);
                                }
                            }
                        }
                    }

                    //button_play_pause.setText( "Play");
                    button_play_pause.setImageResource(R.mipmap.ic_media_play);
                }
            });
        }

        if ( button_mute != null ) {
            button_mute.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ( mMediaPlayer == null ) {
                        Log.d( TAG, "button_mute: MediaPlayer == NULL" );
                        return;
                    }

                    if ( m_stream_url == null ) {
                        Log.d( TAG, "button_mute: stream url == NULL" );
                        return;
                    }

                    if ( mMediaPlayer.isReleased() ) {
                        Log.d( TAG, "button_mute: MediaPlayer is released..." );
                        return;
                    }

                    // current mediaplayer volume
                    Log.d( TAG, "button_mute: current MediaPlayer volume = " + mMediaPlayer.getVolume() );

                    m_is_mute = !m_is_mute;

                    if ( m_is_mute ) {
                        // mute
                        button_mute.setImageResource(R.mipmap.ic_lock_ringer_off_alpha);

                        //! NOTE: Do not use 'mMediaPlayer.getVolume()' it always returns 0.
                        //m_current_mediaplayer_volume = mMediaPlayer.getVolume();
                        mMediaPlayer.setVolume( 0 );
                    }
                    else {
                        // unmute
                        button_mute.setImageResource(R.mipmap.ic_lock_ringer_on_alpha);
                        mMediaPlayer.setVolume( 100 ); // normal (not max volume)
                    }
                }
            });
        }

        if ( button_record != null ) {
            button_record.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    start_recording();
                }
            });
        }

        if ( button_record_list != null ) {
            button_record_list.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), Activity_RecordedList.class);
                    //startActivity( intent );
                    m_activity_result_launcher.launch( intent );
                }
            });
        }

        if ( button_fullscreen != null ) {
            button_fullscreen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int old = getRequestedOrientation();
                    if ( old == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                    else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }
                }
            });
        }

        if ( seekbar != null ) {
            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    if ( mMediaPlayer == null ) {
                        Log.d( TAG, "seekbar: MediaPlayer == NULL" );
                        return;
                    }
                    if ( mMediaPlayer.isReleased() ) {
                        Log.d( TAG, "seekbar: MediaPlayer is released..." );
                        return;
                    }

                    if ( b ) {
                        long seek_time = (long) ((seekBar.getProgress() * mMediaPlayer.getLength()) / 100);
                        mMediaPlayer.setTime( seek_time );
                    }

                    m_seekbar_seektime = mMediaPlayer.getTime();

                    if ( tv_seekbar_position != null ) {
                        tv_seekbar_position.setText( "(" + String.valueOf(seekBar.getProgress()) + "%)" );
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    //
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    //
                }
            });
        }
    }

    private void release(final boolean on_pause) {
        Log.d( TAG, "release()" );

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if ( on_pause ) {
            if ( mMediaPlayer != null && !mMediaPlayer.isReleased() ) {
                // for checking resume after onPause()
                m_is_pause = mMediaPlayer.isPlaying();
            }
        }
        else {
            m_is_pause = false;
            m_is_play_recorded_file = false;
            m_seekbar_seektime = 0;

            seekbar_update( true );
        }

        // LibVLC
        {
            if ( mMediaPlayer != null ) mMediaPlayer.release();

            if ( on_pause && m_is_recording ) { }
            else {
                if ( mMediaPlayer_record_stdout != null ) mMediaPlayer_record_stdout.release();
            }

            if ( mLibVLC != null ) mLibVLC.release();

            //mMediaPlayer = null;
            //mMediaPlayer_record_stdout = null;
            //mLibVLC = null;
        }

        if ( on_pause && m_is_recording ) { }
        else {
            m_handler = null;
            m_stats_runnable = null;
            m_stream_record_filename = null;
            //m_seekbar_seektime = 0;
        }
    }

    private void show_control_ui(final boolean system, final boolean show) {
        final LinearLayout control_layout = (LinearLayout) findViewById( R.id.control_layout );

        if ( control_layout == null ) return;

        int old = control_layout.getVisibility();

        if ( system ) {
            if ( show ) old = View.GONE;
            else old = View.VISIBLE;
        }

        set_play_info( false );
        setChangePlaybackState();

        if ( old == View.VISIBLE ) {
            //control_layout.setVisibility(View.GONE);
            control_layout.animate()
                    .alpha(.0f)
                    .translationY(-1 * control_layout.getHeight())
                    .setDuration(300)
                    .withEndAction( () -> control_layout.setVisibility(View.GONE) )
                    .start();
        }
        else {
            control_layout.setVisibility(View.VISIBLE);
            control_layout.animate()
                    .alpha(1.f)
                    .translationY(0)
                    .setDuration(300).start();
        }
    }

    private void resume() {
        // for checking resume after onPause()
        if ( m_is_pause ) {
            // resume play (restart here)
            playback_new( m_stream_url );
        }
    }

    private void playback_new(final String stream_url) {
        Log.d( TAG, "playback_new(): URL = " + stream_url );

        if ( stream_url == null || stream_url.isEmpty() ) {
            Log.d( TAG, "playback_new(): URL is NULL or EMPTY" );
            return;
        }

        m_uri_stream = Uri.parse( stream_url );


        // LibVLC
        {
            try {
                // init
                final ArrayList<String> args = new ArrayList<>();
                //args.add("-vvv");
                args.add("--network-caching=1500");
                args.add("--file-caching=1000");
                args.add("--live-caching=1500");


                //! FIXME:
                // ㅁndroid libvlc: File-based HTTP stream H.264 playback error, but sometimes playback works good.
                //
                // Test URL: Big Buck Bunny, http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
                //
                // Error log:
                // ../../src/player/timer.c:318: void vlc_player_UpdateTimerSource(vlc_player_t *, struct vlc_player_timer_source *, double, vlc_tick_t, vlc_tick_t): assertion "ts >= VLC_TICK_0" failed
                // Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE) in tid 15219 (AudioTrack)


                if ( mLibVLC != null ) mLibVLC.release();
                if ( mMediaPlayer != null ) mMediaPlayer.release();

                mLibVLC = new LibVLC(this, args);
                mMediaPlayer = new MediaPlayer(mLibVLC);
                mVideoLayout = findViewById(R.id.video_layout);
                mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);

                //final Media media = new Media( mLibVLC, getAssets().openFd(ASSET_FILENAME) );
                //final Media media = new Media( mLibVLC, m_filename );
                //final Media media = new Media(mLibVLC, m_uri_stream);
                Media media = null;

                if ( m_is_play_recorded_file ) {
                    // recorded file
                    media = new Media(mLibVLC, stream_url);
                }
                else {
                    // URL: Live stream, File-based HTTP stream
                    media = new Media(mLibVLC, m_uri_stream);
                }

                //media.setHWDecoderEnabled(true, false);

                mMediaPlayer.setMedia( media );
                media.release();


                if ( m_is_mute ) {
                    final ImageView button_mute = (ImageView) findViewById(R.id.Button_mute);

                    if ( button_mute != null ) {
                        // unmute
                        //button_mute.setImageResource(R.mipmap.ic_lock_ringer_on_alpha);
                        //mMediaPlayer.setVolume( 100 ); // normal (not max volume)

                        // mute
                        button_mute.setImageResource(R.mipmap.ic_lock_ringer_off_alpha);
                        //! NOTE: Do not use 'mMediaPlayer.getVolume()' it always returns 0.
                        //m_current_mediaplayer_volume = mMediaPlayer.getVolume();
                        mMediaPlayer.setVolume( 0 );
                    }
                }

                if ( m_stream_title != null && m_stream_title.isEmpty() ) {
                    m_stream_title = m_stream_title_old;
                }

                mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
                    final ImageView button_play_pause = (ImageView) findViewById(R.id.Button_play_pause);
                    final TextView tv_play_info_buffering = (TextView) findViewById(R.id.TextView_play_info_buffering);

                    @Override
                    public void onEvent(MediaPlayer.Event event) {
                        switch ( event.type ) {
                            case MediaPlayer.Event.Opening:
                                show_control_ui( true, true );

                                {
                                    final long playback_position = mMediaPlayer.getTime();
                                    final long total_duration = mMediaPlayer.getLength();
                                    // Checks Live stream, File-based HTTP stream
                                    // Live stream
                                    if (playback_position > total_duration) { }
                                    else { mMediaPlayer.setTime(m_seekbar_seektime); }
                                }
                                break;
                            case MediaPlayer.Event.Playing:
                                //setChangePlaybackState();

                                if ( tv_play_info_buffering != null ) {
                                    tv_play_info_buffering.setVisibility( View.GONE );
                                }

                                if ( button_play_pause != null ) {
                                    button_play_pause.setImageResource(R.mipmap.ic_media_pause);
                                }
                                show_control_ui( true, false );
                                break;
                            case MediaPlayer.Event.Paused:
                                if ( button_play_pause != null ) {
                                    button_play_pause.setImageResource(R.mipmap.ic_media_play);
                                }

                                if ( tv_play_info_buffering != null ) {
                                    tv_play_info_buffering.setVisibility( View.GONE );
                                }
                                break;
                            case MediaPlayer.Event.Stopped:
                                if ( button_play_pause != null ) {
                                    button_play_pause.setImageResource(R.mipmap.ic_media_play);
                                }

                                if ( tv_play_info_buffering != null ) {
                                    tv_play_info_buffering.setVisibility( View.GONE );
                                }
                                break;
                            case MediaPlayer.Event.Buffering:
                                if ( tv_play_info_buffering != null ) {
                                    final String v =  event.getBuffering() + "%";
                                    tv_play_info_buffering.setText( v );
                                }
                                break;
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            {
                final ImageView button_play_pause = (ImageView) findViewById(R.id.Button_play_pause);
                if (button_play_pause != null) {
                    button_play_pause.setImageResource(R.mipmap.ic_media_pause);
                }
            }

            set_play_info( false );

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            get_playback_record_status();
            mMediaPlayer.play();
        }
    }

    // record (stdout only)
    private void start_recording() {
        if ( mMediaPlayer == null ) {
            Log.d( TAG, "start_recording(): MediaPlayer == NULL" );
            return;
        }

        if ( m_stream_url == null ) {
            Log.d( TAG, "start_recording(): stream url == NULL" );
            return;
        }

        if ( mMediaPlayer.isReleased() ) {
            Log.d( TAG, "start_recording(): MediaPlayer is released..." );
            return;
        }

        if ( m_is_play_recorded_file ) {
            Log.d( TAG, "start_recording(): Media Type = Local Storage File..." );
            Toast.makeText( getApplicationContext(), "Cannot record, playing local storage file...", Toast.LENGTH_SHORT ).show();
            return;
        }

        m_is_recording = !m_is_recording;

        ImageView button_record = (ImageView) findViewById(R.id.Button_record);

        if ( m_is_recording ) {
            Toast.makeText( getApplicationContext(), "Start Recording...", Toast.LENGTH_SHORT ).show();

            //button_record.setText( "Stop Recording");
            button_record.setImageResource( R.mipmap.btn_radio_on_selected );

            // record streams (fetching to internal-storage then copy media to external-storage)
            final String base_path = getApplicationContext().getFilesDir().getAbsolutePath(); // /data/data/<package>/files/
            String full_path = base_path + File.separator + "record";
            // make an unique filename: "filename + _ + yyyy-MM-dd-HH-mm-ss".xxx
            SimpleDateFormat dateFormat = new java.text.SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
            Date dateToday = new Date();
            String filename = ""; // cannot pass to record(), path directory only, not a filename.

            File home_dir = new File(full_path);
            if (!home_dir.exists()) {
                if (home_dir.mkdir()) {
                    Log.d(TAG, "start_recording(): home directory created: " + full_path);
                } else {
                    Log.d(TAG, "start_recording(): home directory created [FAIL]: " + full_path);
                }
            }

            if ( (dateFormat != null) && (dateToday != null) ) {
                filename = dateFormat.format(dateToday) + "-" + m_stream_title.replace(" ", "_");
                filename += ".ts";
            }

            try {
                // init
                final ArrayList<String> args = new ArrayList<>();
                //args.add("-vvv");

                //if ( mLibVLC != null ) mLibVLC.release();
                //if ( mMediaPlayer != null ) mMediaPlayer.release();
                if ( mMediaPlayer_record_stdout != null ) mMediaPlayer_record_stdout.release();

                //mLibVLC = new LibVLC(this, args);
                mMediaPlayer_record_stdout = new MediaPlayer( mLibVLC );
                //mVideoLayout = findViewById(R.id.video_layout);
                //mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);

                //final Media media = new Media( mLibVLC, getAssets().openFd(ASSET_FILENAME) );
                //final Media media = new Media( mLibVLC, m_filename );
                final Media media = new Media( mLibVLC, m_uri_stream );

                media.setHWDecoderEnabled(true, false);

                {
                    // ":sout" is the stream output option
                    // "#duplicate{dst=display,dst=std{...}}" duplicates the stream, sending one to display and one to standard output
                    // "dst=std{access=file,mux=ts,dst=...}" specifies standard output to a file, with TS muxing
                    // "mux=ts" specifies Transport Stream muxing, a common container format. You can change this based on your needs.
                    //
                    // display, stdout(file)
                    //final String options =
                    //        ":sout=#duplicate{dst=display,dst=std{access=file,mux=ts,dst=" +
                    //        "\"" + full_path + File.separator + filename + "\"" +
                    //        "}}";
                    //
                    // display, stdout(file)
                    //final String options = ":sout=#duplicate{dst=file{dst=\"" +
                    //        full_path + File.separator + filename +
                    //        "\"}, dst=display}";
                    //
                    // stdout(file) only
                    final String options =
                            //":sout=#file{dst=" +
                            //"\"" + full_path + File.separator + filename + "\"" +
                            //",no-overwrite}:sout-keep";
                            ":sout=#std{access=file,mux=ts,dst=" +
                            "\"" + full_path + File.separator + filename + "\"" +
                            "}}";

                    media.addOption( options );

                    m_stream_record_filename = full_path + File.separator + filename;

                    Log.d( TAG, "start_recording(): record file path = " + full_path + File.separator + filename );
                }

                mMediaPlayer_record_stdout.setMedia( media );
                media.release();
            } catch ( Exception e ) {
                e.printStackTrace();
            }

            get_playback_record_status();
            //mMediaPlayer.record( full_path, true ); // capture display, not a raw stream data

            // record (stdout only)
            mMediaPlayer_record_stdout.play();
        }
        else {
            Toast.makeText( getApplicationContext(), "Stop Recording...", Toast.LENGTH_SHORT ).show();

            //button_record.setText( "Record");
            button_record.setImageResource( R.mipmap.btn_radio_off_selected );

            //mMediaPlayer.record( null, false ); // capture display, not a raw stream data

            mMediaPlayer_record_stdout.stop();
            mMediaPlayer_record_stdout.release();
            m_stream_record_filename = null;
        }

        /*
        {
            //! DO NOT USE THIS APPROACH...
            if ( m_is_recording ) {
                button_record.setText("Stop Recording");
            }
            else {
                button_record.setText( "Record");

                final Media media = new Media(mLibVLC, m_uri_stream);
                mMediaPlayer.pause();
                mMediaPlayer.setMedia( media );
                media.release();
                mMediaPlayer.play();

                return;
            }

            try {
                final Media media = new Media(mLibVLC, m_uri_stream);
                //IMedia media = mMediaPlayer.getMedia();

                mMediaPlayer.pause();

                // record streams (fetching to internal-storage then copy media to external-storage)
                final String base_path = getApplicationContext().getFilesDir().getAbsolutePath(); // /data/data/<package>/files/
                String full_path = base_path + File.separator + "record";
                // make an unique filename: "filename + _ + yyyy-MM-dd-HH-mm-ss".xxx
                SimpleDateFormat dateFormat = new java.text.SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss" );
                Date dateToday = new Date();
                String filename = "";

                File home_dir = new File(full_path);
                if (!home_dir.exists()) {
                    if (home_dir.mkdir()) {
                        Log.d(TAG, "home directory created: " + full_path);
                    } else {
                        Log.d(TAG, "home directory created [FAIL]: " + full_path);
                    }
                }

                if ( (dateFormat != null) && (dateToday != null) ) {
                    filename = dateFormat.format(dateToday);
                    filename += ".ts";

                    final String options = ":sout=#duplicate{dst=file{dst=\"" +
                            full_path + File.separator + filename +
                            "\"}, dst=display}";
                    media.setHWDecoderEnabled(true, false);
                    //media.addOption(":network-caching=600");
                    //media.addOption(":clock-jitter=0");
                    //media.addOption(":clock-synchro=0");

                    media.addOption(options);
                    mMediaPlayer.setMedia(media);
                    media.release();
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            mMediaPlayer.play();
        }
        */
    }

    private void stop_recording() {
        if ( mMediaPlayer_record_stdout == null ) {
            Log.d( TAG, "stop_recording(): MediaPlayer_record == NULL" );
            return;
        }

        if ( m_stream_url == null ) {
            Log.d( TAG, "stop_recording(): stream url == NULL" );
            return;
        }

        if ( mMediaPlayer_record_stdout.isReleased() ) {
            Log.d( TAG, "stop_recording(): MediaPlayer_record is released..." );
            return;
        }

        if ( m_is_play_recorded_file ) {
            Log.d( TAG, "stop_recording(): Media Type = Local Storage File..." );
            return;
        }

        if ( m_is_recording ) {
            Toast.makeText( getApplicationContext(), "Stop Recording...", Toast.LENGTH_SHORT ).show();

            // SEE: get_record_status(): Runnable::run()
            m_record_total_bytes_read = 0;
            m_record_start_millisecond = 0;
            m_stream_record_filename = null;

            {
                final ImageView button_record = (ImageView) findViewById(R.id.Button_record);
                if ( button_record != null ) {
                    //button_record.setText( "Record");
                    button_record.setImageResource( R.mipmap.btn_radio_off_selected );
                }
            }

            //mMediaPlayer.record( null, false ); // capture display, not a raw stream data

            mMediaPlayer_record_stdout.stop();
            mMediaPlayer_record_stdout.release();
        }

        m_is_recording = false;
    }

    private void setChangePlaybackState() {
        //final Activity activity
        //if ( activity == null ) {
        //    Log.d( TAG, "setChangePlaybackState(): Activity == NULL" );
        //    return;
        //}

        // Icons
        {
            final ImageView button_play_pause = (ImageView) findViewById(R.id.Button_play_pause);
            final ImageView button_record = (ImageView) findViewById(R.id.Button_record);

            if ( button_play_pause != null ) {
                if ( mMediaPlayer != null ) {
                    if ( mMediaPlayer.isReleased() ) {
                        //button_play_pause.setText( "Play");
                        button_play_pause.setImageResource(R.mipmap.ic_media_play);
                    }
                    else {
                        if ( mMediaPlayer.isPlaying() ) {
                            //button_play_pause.setText( "Pause");
                            button_play_pause.setImageResource(R.mipmap.ic_media_pause);
                        }
                        else {
                            //button_play_pause.setText( "Play");
                            button_play_pause.setImageResource(R.mipmap.ic_media_play);
                        }
                    }
                }
                else {
                    //button_play_pause.setText( "Play");
                    button_play_pause.setImageResource(R.mipmap.ic_media_play);
                }
            }

            if ( button_record != null ) {
                if (m_is_recording) {
                    //button_record.setText( "Stop Recording");
                    button_record.setImageResource(R.mipmap.btn_radio_on_selected);
                }
                else {
                    //button_record.setText( "Record");
                    button_record.setImageResource(R.mipmap.btn_radio_off_selected);
                }
            }
        }
    }

    private void set_play_info(final boolean clear) {
        Log.d( TAG, "set_play_info()" );

        final TextView tv_play_info = (TextView) findViewById( R.id.TextView_play_info );
        final TextView tv_play_info_audio_input = (TextView) findViewById( R.id.TextView_play_info_audio_input );
        final TextView tv_play_info_video_input = (TextView) findViewById( R.id.TextView_play_info_video_input );

        if ( clear ) {
            m_stream_title = "";
        }

        {
            if (tv_play_info != null) {
                tv_play_info.setText(m_stream_title);
            }

            if ( mMediaPlayer == null ) {
                Log.d( TAG, "set_play_info(): MediaPlayer == NULL" );
                return;
            }

            if ( mMediaPlayer.isReleased() ) {
                Log.d( TAG, "set_play_info(): MediaPlayer is released..." );
                return;
            }

            if ( !mMediaPlayer.isPlaying() ) {
                Log.d( TAG, "set_play_info(): MediaPlayer is not playing..." );
                return;
            }

            IMedia.Stats stats = mMediaPlayer.getMedia().getStats();
            IMedia.Track[] tracks = mMediaPlayer.getMedia().getTracks();

            if ( stats == null ) {
                Log.d( TAG, "set_play_info(): stats == NULL" );
                return;
            }

            if ( tracks == null ) {
                Log.d( TAG, "set_play_info(): tracks == NULL" );
                return;
            }

            String media_info_audio_codec = ""; // AAC, ...
            String media_info_audio_sample_rate = ""; // 48000 Hz
            String media_info_audio_channel = ""; // 2 Ch
            String media_info_audio_bitrate = ""; // xxx Kbps

            String media_info_video_codec = ""; // H264, AVC1, ...
            String media_info_video_resolution = ""; // 1920x1080
            String media_info_video_fps = ""; // 30 FPS
            String media_info_video_bitrate = ""; // xxx Kbps
            //String media_info_video_frame_info = "";

            //media_info_audio_bitrate =
            //        "input: " + String.format("%.2f Kbps", (double)((stats.inputBitrate * 8.0f) / 1000.0f)) + ", " +
            //        "demux: " + String.format("%.2f Kbps", (double)((stats.demuxBitrate * 8.0f) / 1000.0f));

            //media_info_video_frame_info = stats.decodedVideo + "->" + stats.displayedPictures + " (loss: " + stats.lostPictures + ")";
            //media_info_video_bitrate =
            //        "input: " + String.format("%.2f Kbps", (double)((stats.inputBitrate * 8.0f) / 1000.0f)) + ", " +
            //        "demux: " + String.format("%.2f Kbps", (double)((stats.demuxBitrate * 8.0f) / 1000.0f));

            {
                //if (stats.readBytes > 0 && mMediaPlayer.getTime() > 0) {
                //    media_info_video_bitrate = ((float)((stats.demuxReadBytes * 8f) / (mMediaPlayer.getTime() / 1000.f)) / 1000.f) + " Kbps";
                //    Log.d(TAG, "readBytes = " + stats.readBytes + ", getTime = " + mMediaPlayer.getTime() + ", " + media_info_video_bitrate);
                //}

                /*
                //private long m_bitrate_last_time_millisecond = 0; // class member
                long current_time = System.currentTimeMillis();
                if ( m_bitrate_last_time_millisecond > 0 ) {
                    long time = current_time - m_bitrate_last_time_millisecond;
                    if (stats.readBytes > 0 && mMediaPlayer.getTime() > 0) {
                        media_info_video_bitrate = (float) ((((float) (stats.readBytes * 8) / (time / 1000.f))) / 1000.f) + " kbps";
                        Log.d(TAG, "readBytes = " + stats.readBytes + ", getTime = " + mMediaPlayer.getTime() + ", " + media_info_video_bitrate);
                    }
                }
                m_bitrate_last_time_millisecond = current_time;
                */
            }

            //Log.d( TAG, "bitrate = " + stats.inputBitrate + ", " + stats.demuxBitrate );

            for ( var track: tracks ) {
                if ( track.type == IMedia.Track.Type.Audio ) {
                    Media.AudioTrack track_info = (Media.AudioTrack) track;
                    media_info_audio_codec = track_info.codec;
                    media_info_audio_sample_rate = String.valueOf(track_info.rate) + " Hz";
                    media_info_audio_channel = String.valueOf(track_info.channels) + " Ch";
                    //media_info_audio_bitrate = String.valueOf(track_info.bitrate) + " Kbps";
                }
                else if ( track.type == IMedia.Track.Type.Video ) {
                    Media.VideoTrack track_info = (Media.VideoTrack) track;
                    media_info_video_codec = track_info.codec;
                    media_info_video_resolution = track_info.width + "x" + track_info.height;
                    media_info_video_fps = String.valueOf((track_info.frameRateDen > 0) ? (float)(track_info.frameRateNum / track_info.frameRateDen) : 0.f ) + " FPS";
                    //media_info_video_bitrate = String.valueOf(track_info.bitrate) + " Kbps";
                }
            }

            final String audio_info =
                    media_info_audio_codec + ", " +
                    media_info_audio_sample_rate + ", " +
                    media_info_audio_channel + ", " +
                    media_info_audio_bitrate;
            final String video_info =
                    media_info_video_codec + ", " +
                    media_info_video_resolution + ", " +
                    media_info_video_fps + ", " +
                    media_info_video_bitrate;

            if ( tv_play_info_audio_input != null ) {
                tv_play_info_audio_input.setText( audio_info );
            }

            if ( tv_play_info_video_input != null ) {
                tv_play_info_video_input.setText( video_info );
            }
        }
    }

    private void seekbar_update(final boolean clear_and_hide) {
        final LinearLayout seekbar_layout = (LinearLayout) findViewById( R.id.seekbar_layout );
        final SeekBar seekbar = (SeekBar) findViewById( R.id.SeekBar );
        final TextView tv_seekbar_playback_position = (TextView) findViewById( R.id.TextView_seekbar_playback_position );
        final TextView tv_seekbar_total_duration = (TextView) findViewById( R.id.TextView_seekbar_total_duration );
        final TextView tv_seekbar_position = (TextView) findViewById( R.id.TextView_seekbar_position );

        if ( clear_and_hide ) {
            if ( tv_seekbar_playback_position != null ) {
                tv_seekbar_playback_position.setText( "00:00:00" );
            }

            if ( tv_seekbar_total_duration != null ) {
                tv_seekbar_total_duration.setText( "00:00:00" );
            }

            if ( tv_seekbar_position != null ) {
                tv_seekbar_position.setText( "(%)" );
            }

            if ( seekbar_layout != null ) {
                seekbar_layout.setVisibility( View.GONE );
            }

            return;
        }

        if ( mMediaPlayer == null ) {
            Log.d( TAG, "seekbar_update(): MediaPlayer == NULL" );
            return;
        }

        if ( mMediaPlayer.isReleased() ) {
            Log.d( TAG, "seekbar_update(): MediaPlayer is released..." );
            return;
        }

        if ( seekbar_layout != null && seekbar != null ) {
            if ( mMediaPlayer.isSeekable() ) {
                seekbar_layout.setVisibility( View.VISIBLE );

                if ( mMediaPlayer.isPlaying() ) {
                    final long playback_position = mMediaPlayer.getTime();
                    final long total_duration = mMediaPlayer.getLength();

                    String str_cur_pos = m_main_app.get_format_time( false, playback_position );
                    String str_total_dur = m_main_app.get_format_time( false, total_duration );

                    // Checks Live stream, File-based HTTP stream
                    // Live stream
                    if ( playback_position > total_duration ) {
                        seekbar.setEnabled( false );
                        str_total_dur = "00:00:00";
                        str_cur_pos = m_main_app.get_format_time( true, m_stream_start_millisecond );

                        if ( tv_seekbar_position != null ) {
                            tv_seekbar_position.setText( "(%)" );
                        }

                        //Log.d( TAG, "seekbar_update(): start pos = " + m_stream_start_millisecond );
                        //Log.d( TAG, "seekbar_update(): pos = " + str_cur_pos );
                    }
                    // File-based HTTP stream, Local Storage File
                    else {
                        seekbar.setEnabled( true );

                        if ( total_duration > 0 ) {
                            int progress_percent = (int) (((float)playback_position / total_duration) * 100);
                            seekbar.setProgress( progress_percent );
                        }
                    }

                    if ( tv_seekbar_playback_position != null ) {
                        tv_seekbar_playback_position.setText( str_cur_pos );
                    }

                    if ( tv_seekbar_total_duration != null ) {
                        tv_seekbar_total_duration.setText( str_total_dur );
                    }
                }
                else {
                    Log.d( TAG, "seekbar_update(): MediaPlayer is not playing..." );
                }
            }
            else {
                seekbar_layout.setVisibility( View.GONE );
            }
        }
    }

    private void get_playback_record_status() {
        Log.d( TAG, "get_playback_record_status()" );

        //if ( !m_is_recording ) {
        //    Log.d( TAG, "get_playback_record_status(): not recording..." );
        //    return;
        //}

        //new Thread(
        //        () -> {
        //            func();
        //        })
        //        .start();

        try {
            m_record_start_millisecond = System.currentTimeMillis();
            m_stream_start_millisecond = System.currentTimeMillis();

            if ( m_handler != null ) m_handler = null;
            if ( m_stats_runnable != null ) m_stats_runnable = null;

            m_handler = new Handler( Looper.getMainLooper() );
            m_stats_runnable = new Runnable() {
                final TextView tv_bytes_read = (TextView) findViewById( R.id.TextView_record_stream_stats__bytes_read );
                final TextView tv_total_bytes_read = (TextView) findViewById( R.id.TextView_record_stream_stats__total_bytes_read );
                final TextView tv_duration_time = (TextView) findViewById( R.id.TextView_record_stream_stats__duration_time );

                File m_file_stream_record = null;

                @Override
                public void run() {
                    if ( !m_is_recording ) {
                        //Log.d( TAG, "get_playback_record_status(): not recording..." );

                        m_record_total_bytes_read = 0;
                        m_record_start_millisecond = 0;
                        m_stream_record_filename = null;

                        if ( tv_bytes_read != null ) { tv_bytes_read.setText( "" ); }
                        if ( tv_total_bytes_read != null ) { tv_total_bytes_read.setText( "" ); }
                        if ( tv_duration_time != null ) { tv_duration_time.setText( "" ); }

                        if ( mMediaPlayer != null && !mMediaPlayer.isReleased() && mMediaPlayer.isPlaying() ) {
                            //final long pos = mMediaPlayer.getTime();
                            //final long dur = mMediaPlayer.getLength();
                            //Log.d( TAG, "get_playback_record_status():  pos = " + pos + ", dur = " + dur );

                            if ( mMediaPlayer.isSeekable() ) {
                                seekbar_update( false );
                            }

                            set_play_info( false );

                            if ( m_handler != null ) {
                                m_handler.postDelayed( this, 1000 );
                            }
                        }

                        return;
                    }
                    if ( mMediaPlayer_record_stdout == null ) {
                        Log.d( TAG, "get_playback_record_status(): MediaPlayer_record == NULL, stop recording..." );
                        return;
                    }

                    try {
                        // useless in 'per second' but leave this...
                        Media.Stats stats = mMediaPlayer_record_stdout.getMedia().getStats();
                        long bytes_read = stats.readBytes;

                        if ( m_file_stream_record == null ) {
                            m_file_stream_record = new File( m_stream_record_filename );
                        }

                        if ( m_file_stream_record != null ) {
                            m_record_total_bytes_read = m_file_stream_record.length();
                            //Log.d( TAG, "get_playback_record_status(): get file size: " + m_record_total_bytes_read );
                        }
                        else {
                            Log.d( TAG, "get_playback_record_status(): get file size: record file == NULL..., ignore..." );
                        }

                        long record_duration_seconds = (System.currentTimeMillis() - m_record_start_millisecond) / 1000;
                        long hours = record_duration_seconds / 3600;
                        long minutes = (record_duration_seconds % 3600) / 60;
                        long seconds = record_duration_seconds % 60;
                        final String record_duration_time = String.format( "%02d:%02d:%02d", hours, minutes, seconds );

                        //Log.d( TAG, "get_playback_record_status(): Bytes read = " + bytes_read );
                        //Log.d( TAG, "get_playback_record_status(): Total bytes read = " + m_record_total_bytes_read );
                        //Log.d( TAG, "get_playback_record_status(): Record duration time = " + record_duration_time );

                        // updates
                        {
                            // useless in 'per second' but leave this...
                            if ( tv_bytes_read != null ) {
                                final String val = " " + bytes_read;
                                tv_bytes_read.setText( val );
                            }

                            if ( tv_total_bytes_read != null ) {
                                //final String kib = String.format( "%.2f", (double)((double)m_record_total_bytes_read / 1024) );
                                //final String val = " " + kib + " KiB (" + String.format("%,d", m_record_total_bytes_read) + " Bytes)";
                                final String val = " " + m_main_app.humanReadableByteCountBin( m_record_total_bytes_read );
                                tv_total_bytes_read.setText( val );
                            }

                            if ( tv_duration_time != null ) {
                                tv_duration_time.setText( record_duration_time );
                            }
                        }

                        if ( mMediaPlayer != null && mMediaPlayer.isSeekable() ) {
                            seekbar_update( false );
                        }
                    } catch ( Exception e ) {
                        e.printStackTrace();

                        m_file_stream_record = null;
                    }

                    if ( m_handler != null ) {
                        m_handler.postDelayed( this, 1000 );
                    }
                    else {
                        m_file_stream_record = null;
                    }
                }
            };

            m_handler.post( m_stats_runnable );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}