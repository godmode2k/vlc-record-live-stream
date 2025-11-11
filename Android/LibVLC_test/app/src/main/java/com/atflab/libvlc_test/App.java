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
}
* -----------------------------------------------------------------
* TODO:
*
* URGENT!!!
* TODO:
*
*/

package com.atflab.libvlc_test;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;


public class App extends Application {
    private static final String TAG = "App";

    private Context m_context = null;

    public String INTERNAL_HOME_DIR = null;
    // Internal Directory: /data/data/<package>/files/record
    public String INTERNAL_RECORD_DIR = null;
    // Public Directory: <Download>/record_stream
    public String PUBLIC_HOME_DIR = null;
    public String INTERNAL_RECORD_DIR_NAME = "record";
    public String PUBLIC_RECORD_DIR_NAME = "record_stream";
    public String INTERNAL_STREAM_URL_FILE = "stream_urls.json";
    // file: stream_urls.json
    // {
    //     "stream_urls": [
    //         { "title": "EBS1", "url": "..." }
    //     ]
    // }

    private Boolean m_cancel_process = false;
    private ProgressDialog m_progress_dialog = null;
    public final int RUN_TYPE__COPY_INTERNAL_FILE_TO_PUBLIC_DOWNLOAD = 1;
    public final int RUN_TYPE__DELETE_INTERNAL_FILE = 2;

    public final int ACTIVITY_RESULT_CODE__ACTIVITY_OPEN_URL = 1000;
    public final int ACTIVITY_RESULT_CODE__ACTIVITY_RECORDED_LIST = 1001;
    public final String ACTIVITY_OPEN_URL__INTENT_KEY__URL = "url";
    public final String ACTIVITY_OPEN_URL__INTENT_KEY__TITLE = "title";
    public final String ACTIVITY_RECORDED_LIST__INTENT_KEY__FILENAME = "filename";


    @Override
    public void onCreate() {
        super.onCreate();

        init();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        release();
    }

    private void init() {
        m_context = this.getApplicationContext();

        INTERNAL_HOME_DIR = getApplicationContext().getFilesDir().getAbsolutePath(); // /data/data/<package>/files/
        // Internal Directory: /data/data/<package>/files/record
        INTERNAL_RECORD_DIR = INTERNAL_HOME_DIR + File.separator + INTERNAL_RECORD_DIR_NAME;
        // Public Directory: <Download>/record_stream
        //PUBLIC_HOME_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "record_stream";
        PUBLIC_HOME_DIR = Environment.DIRECTORY_DOWNLOADS + File.separator + PUBLIC_RECORD_DIR_NAME;
    }

    private void release() {
        //
    }

    public void copy_internal_file_to_public_download(final ArrayList<String> src_list_filename,
                                                      final String dst_dir, RunTask task) {
        Log.d( TAG, "src size: " + src_list_filename.size() );

        for ( int i = 0; i < src_list_filename.size(); i++ ) {
            final String src_filename = src_list_filename.get(i);
            final String dst_filename = src_filename;

            try {
                /*
                {
                    File home_dir = new File( dst_dir );
                    if ( !home_dir.exists() ) {
                        if ( home_dir.mkdir() ) {
                            Log.d( TAG, "target directory created: " + dst_dir );
                        } else {
                            Log.d( TAG, "target directory created [FAIL]: " + dst_dir );
                        }
                    }
                }
                */

                Uri uri_src_file = Uri.fromFile(new File(INTERNAL_RECORD_DIR + File.separator + src_filename));
                //InputStream is = url.openStream();
                InputStream is = null;
                OutputStream os = null;
                byte[] buffer = new byte[4 * 1024];
                int bytes = 0;

                if (uri_src_file != null) {
                    is = getContentResolver().openInputStream(uri_src_file);

                    Uri collection;
                    ContentResolver resolver = m_context.getContentResolver();
                    ContentValues contentValues = new ContentValues();

                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, dst_filename);
                    //contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain"); // test
                    //contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "<dir>" );
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, dst_dir);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    } else {
                        collection = MediaStore.Files.getContentUri("external");
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
                    }

                    Uri fileUri = resolver.insert(collection, contentValues);

                    if (fileUri == null) {
                        Log.d(TAG, "output == NULL");
                        return;
                    }

                    //try (OutputStream outputStream = resolver.openOutputStream(fileUri)) {
                    //    outputStream.write(content.getBytes());
                    //    outputStream.flush();
                    //    Toast.makeText(m_context, "File saved to Downloads", Toast.LENGTH_SHORT).show();
                    //}
                    //catch (IOException e) {
                    //    e.printStackTrace();
                    //    Toast.makeText(m_context, "Failed to save file", Toast.LENGTH_SHORT).show();
                    //}

                    //os = new java.io.FileOutputStream(file);
                    os = resolver.openOutputStream(fileUri);
                    if (os == null) {
                        Log.d(TAG, "output == NULL");
                        return;
                    }

                    Log.d(TAG, "Copying...  (" + (i+1) + "/" + src_list_filename.size() + ")");
                    Log.d(TAG, "file (URI) = " + fileUri.toString());
                    Log.d(TAG, "src = " + src_filename);
                    Log.d(TAG, "dst = " + dst_dir + "/" + dst_filename);

                    final int src_file_size = is.available();
                    int size = 0;
                    while ((bytes = is.read(buffer)) > 0) {
                        if (m_cancel_process) {
                            Log.d(TAG, "cancelled...");
                            break;
                        }

                        size += bytes;

                        String msg = "'다운로드'로 복사 중...\n";
                        msg += (((i+1) * 100) / src_list_filename.size()) + "% " + "(" + (i+1) + "/" + src_list_filename.size() + ")\n";
                        msg += "(" + size + "/" + src_file_size + ")";
                        //Log.d( TAG, "progress: " + msg );
                        task.updates_progress(msg);

                        os.write(buffer, 0, bytes);
                    }
                    os.flush();
                    Log.d(TAG, "done...");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear();
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        resolver.update(fileUri, contentValues, null, null);
                    }
                }

                is.close();
                os.close();
                is = null;
                os = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void delete_internal_file(final ArrayList<String> src_list_filename) {
        Log.d( TAG, "src size: " + src_list_filename.size() );

        for ( var src_filename: src_list_filename ) {
            try {
                File target = new File(INTERNAL_RECORD_DIR + File.separator + src_filename);
                if (target.exists()) {
                    if (target.delete()) {
                        Log.d(TAG, "target deleted: " + target.getAbsolutePath());
                    } else {
                        Log.d(TAG, "target deleted [FAIL]: " + target.getAbsolutePath());
                    }
                }
                else {
                    Log.d( TAG, "no such file or directory: " + target.getAbsolutePath() );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void run_with_progress(Context context, final int type,
                                  final ArrayList<String> src_list, final String dst_dir) {
        if ( src_list == null || src_list.isEmpty() ) {
            Log.d( TAG, "run_with_progress(): src is NULL or EMPTY" );
            return;
        }

        RunTask task = new RunTask();

        //m_progress_dialog = new ProgressDialog( getActivity() );
        m_progress_dialog = new ProgressDialog( context );
        if ( m_progress_dialog == null ) {
            Log.d( TAG, "run_with_progress(): ProgressDialog == NULL" );
            return;
        }

        m_progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        m_progress_dialog.setMessage("");
        m_progress_dialog.setCancelable(true);
        m_progress_dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                m_cancel_process = true;
            }
        });
        m_progress_dialog.show();

        if ( task == null ) {
            Log.d( TAG, "run_with_progress(): RunTask == NULL" );
            m_progress_dialog.dismiss();
            return;
        }

        task.execute( type, src_list, dst_dir );
    }

    public class RunTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Object doInBackground(Object... objects) {
            //return null;

            final int type = (Integer) objects[0];
            final ArrayList<String> src_list = (ArrayList<String>) objects[1];
            final String dst_dir = (String) objects[2];

            m_cancel_process = false;

            if ( type == RUN_TYPE__COPY_INTERNAL_FILE_TO_PUBLIC_DOWNLOAD ) {
                copy_internal_file_to_public_download( src_list, dst_dir, this );
            }
            else if ( type == RUN_TYPE__DELETE_INTERNAL_FILE ) {
                delete_internal_file( src_list );
            }

            src_list.clear();

            return null;
        }

        public void updates_progress(final String val) {
            publishProgress( val );
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);

            String progress = (String) values[0];

            if ( m_progress_dialog != null ) {
                m_progress_dialog.setMessage(progress);
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            //super.onPostExecute(o);

            if (m_progress_dialog != null) {
                m_progress_dialog.dismiss();
            }

            //Toast.makeText( m_context, "done...", Toast.LENGTH_SHORT ).show();
        }
    }


    public void save_stream_urls(final ArrayList<HashMap<String, Object>> list) {
        Log.d( TAG, "save_stream_urls" );
        // file: stream_urls.json
        // {
        //     "stream_urls": [
        //         { "title": "EBS1", "url": "..." }
        //     ]
        // }

        if ( list == null ) {
            Log.d( TAG, "save_stream_urls(): list == NULL" );
            return;
        }

        try {
            JsonObject obj_export = new JsonObject();
            JsonArray arr_root = new JsonArray();

            for (var v : list) {
                HashMap<String, Object> map = v;
                if (map != null) {
                    final String title = (String) map.get("title");
                    final String url = (String) map.get("url");

                    JsonObject obj = new JsonObject();
                    obj.addProperty("title", title);
                    obj.addProperty("url", url);
                    arr_root.add(obj);
                }
            }

            obj_export.add("stream_urls", arr_root);

            Gson gson = new Gson();
            String json_result = gson.toJson(obj_export);
            Log.d(TAG, "JSON result = \n" + json_result);

            save_contents( json_result.getBytes() );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private void save_contents(final byte[] contents) {
        final String full_filename = INTERNAL_HOME_DIR + File.separator + INTERNAL_STREAM_URL_FILE;

        try {
            {
                // delete file exist
                //final String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                //final String full_filename = path + File.separator + HISTORY_DIR + File.separator + HISTORY_FILENAME;
                Log.d(TAG, "saveas_contents: full pathname = " + full_filename);

                File delete_file = new File(full_filename);
                if (delete_file.exists()) {
                    if (delete_file.delete()) {
                        Log.d(TAG, "saveas_contents: deleted...");
                    } else {
                        Log.d(TAG, "saveas_contents: failed to delete...");
                    }
                }
            }

            File f = new File(full_filename);
            if (!f.exists()) {
                Log.d(TAG, "saveas_contents: file not found, creating...");
                f.createNewFile();
            }
            if (f.exists()) {
                Log.d(TAG, "saveas_contents: file found, writing...");
                OutputStream os = new FileOutputStream(f);
                if (os != null) {
                    os.write(contents);
                    os.flush();
                    os.close();
                    os = null;

                    Log.d(TAG, "saveas_contents: done...");
                }
            } else {
                //Toast.makeText(m_context, "Failed to create file", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "saveas_contents: failed to save as...: file not found...");
                return;
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void load_contents(ArrayList<HashMap<String, Object>> list) {
        final String full_filename = INTERNAL_HOME_DIR + File.separator + INTERNAL_STREAM_URL_FILE;
        String json_str = "";

        try {
            Log.d( TAG, "full pathname = " + full_filename );

            if ( !new File(full_filename).exists() ) {
                Log.d( TAG, "load_contents: failed to open a history file, file not found, ignore..." );
                return;
            }

            Uri fileUri = Uri.fromFile( new File(full_filename) );
            if ( fileUri != null ) {
                String line = null;
                BufferedReader br = null;
                InputStream is = getContentResolver().openInputStream(fileUri);

                br = new BufferedReader( new InputStreamReader(is) );

                while ( true ) {
                    line = br.readLine();
                    if ( line == null ) break;
                    json_str += line;
                }

                if ( br != null ) br.close();
                if ( is != null ) is.close();
                br = null;
                is = null;
            } else {
                Log.d( TAG, "load_contents: failed to open a history file: URI error..." );
                return;
            }

            {
                // file: stream_urls.json
                // {
                //     "stream_urls": [
                //         { "title": "EBS1", "url": "..." }
                //     ]
                // }

                Log.d(TAG, "JSON = \n" + json_str );

                JSONObject obj_export = new JSONObject(json_str);
                JSONArray arr_root = new JSONArray();
                String title = "";
                String url = "";

                arr_root = (JSONArray) obj_export.get("stream_urls");

                for ( int i = 0; i < arr_root.length(); i++ ) {
                    JSONObject obj = new JSONObject();
                    obj = (JSONObject) arr_root.get(i);

                    title = (String) obj.get("title");
                    url = (String) obj.get("url");

                    if (list != null) {
                        HashMap<String, Object> map = new HashMap<String, Object>();
                        if (map != null) {
                            map.put( "checked", false );
                            map.put("title", title);
                            map.put("url", url);
                            list.add(map);
                        }
                    }
                }
            }
        } catch (Exception e ) {
            Log.d( TAG, "load_contents: failed" );
            e.printStackTrace();
            return;
        }

        Log.d( TAG, "load_contents: done..." );
    }

    public String get_format_time(final boolean use_elapsed, final long time_ms) {
        //long duration_seconds = time_ms / 1000;
        long duration_seconds = 0;

        if ( use_elapsed ) {
            duration_seconds = (System.currentTimeMillis() - time_ms) / 1000;
        }
        else {
            duration_seconds = time_ms / 1000;
        }

        long hours = duration_seconds / 3600;
        long minutes = (duration_seconds % 3600) / 60;
        long seconds = duration_seconds % 60;
        String duration_time = String.format( "%02d:%02d:%02d", hours, minutes, seconds );

        return duration_time;
    }

    // Source: https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    //
    // SI (1 k = 1,000)
    public String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
    // Binary (1 Ki = 1,024)
    public String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

}
