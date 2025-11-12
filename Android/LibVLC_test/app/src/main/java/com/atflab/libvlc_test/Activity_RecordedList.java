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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Activity_RecordedList extends AppCompatActivity {
    private static final String TAG = "Activity_RecordedList";

    private App m_main_app = null;
    private ListView m_listview = null;
    private ListArrayAdapter m_listviewAdapter = null;
    private ArrayList<HashMap<String, Object>> m_list_items = null;
    private final String LIST_ITEM_TAG__FILENAME = "filename";
    private final String LIST_ITEM_TAG__FILESIZE = "filesize";
    private final String LIST_ITEM_TAG__CHECKED = "checked";
    private boolean m_selected_mode = false;

    // pass updates if select one on listview when CheckBox_SelectAll is checked.
    private boolean m_checkbox_select_all_pass = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recorded_listview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        release();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void init() {
        Log.d( TAG, "init()" );

        m_main_app = (App)getApplicationContext();

        m_list_items = new ArrayList<HashMap<String, Object>>();
        // load list
        {
            //final String base_path = getApplicationContext().getFilesDir().getAbsolutePath(); // /data/data/<package>/files/
            //String internal_record_dir = base_path + File.separator + "record";

            Log.d( TAG, "init(): path = " + m_main_app.INTERNAL_RECORD_DIR );

            File home_dir = new File(m_main_app.INTERNAL_RECORD_DIR);
            if ( !home_dir.exists() ) {
                Log.d( TAG, "no such file or directory: " + m_main_app.INTERNAL_RECORD_DIR );
                return;
            }

            for ( var v: home_dir.list() ) {
                final long filesize = (new File(m_main_app.INTERNAL_RECORD_DIR + File.separator + v)).length();
                //final String kib = String.format( "%.2f", (double)((double)filesize / 1024) );
                //final String v_filesize = kib + " KiB";
                final String v_filesize = m_main_app.humanReadableByteCountBin( filesize );

                Log.d( TAG, "recorded filename: " + v + ", size: " + v_filesize );

                if ( m_list_items != null ) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    if (map != null) {
                        map.put( LIST_ITEM_TAG__FILENAME, v );
                        map.put( LIST_ITEM_TAG__FILESIZE, v_filesize );
                        map.put( LIST_ITEM_TAG__CHECKED, false );
                        m_list_items.add( map );
                    }
                }
            }
        }


        Button button_copy_internal_file_to_external = findViewById( R.id.Button_copy_internal_file_to_external );
        Button button_delete_internal_file = findViewById( R.id.Button_delete_internal_file );
        Button button_cancel = findViewById( R.id.Button_cancel );
        CheckBox checkbox_select_all = (CheckBox) findViewById( R.id.CheckBox_select_all );

        {
            TextView tv_recorded_list_title = (TextView) findViewById(R.id.TextView_recorded_list_title);
            if (tv_recorded_list_title != null) {
                //tv_recorded_list_title.setText( "Recorded List (" + m_list_items.size() + ")" );
                tv_recorded_list_title.setText(getString(R.string.activity_recorded_list__recorded_list_title) + " (" + m_list_items.size() + ")");
            }
            if (checkbox_select_all != null) {
                checkbox_select_all.setText(R.string.activity_recorded_list__layout_checkbox__select_all);
            }

            if (button_copy_internal_file_to_external != null) {
                button_copy_internal_file_to_external.setText(R.string.activity_recorded_list__layout_button__copy_internal_file_to_external);
            }
            if (button_delete_internal_file != null) {
                button_delete_internal_file.setText(R.string.activity_recorded_list__layout_button__delete_internal_file);
            }
            if (button_cancel != null) {
                button_cancel.setText(R.string.activity_recorded_list__layout_button__cancel);
            }
        }

        button_copy_internal_file_to_external.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( m_list_items == null || m_list_items.isEmpty() ) return;

                ArrayList<String> src_list = new ArrayList<String>();
                int count = 0;

                for ( int i = 0; i < m_list_items.size(); i++ ) {
                    HashMap<String, Object> map = m_list_items.get( i );
                    final String src = (String) map.get( LIST_ITEM_TAG__FILENAME );
                    final String src_filesize = (String) map.get( LIST_ITEM_TAG__FILESIZE );
                    final Boolean checked = (Boolean) map.get( LIST_ITEM_TAG__CHECKED );

                    Log.d( TAG, "src = " + src + ", size = " + src_filesize + ", checked = " + checked );

                    if ( !checked ) continue;

                    count += 1;
                    src_list.add( new String(src) );

                    //Log.d( TAG, "(copy file) selected filename: " +
                    //        "[" + i + "/" + m_list_items.size() + "] " + src );
                    Log.d( TAG, "(copy file) selected filename: " + "[" + i + "] " + src );
                }

                if ( count <= 0 ) {
                    //Toast.makeText( getApplicationContext(), "복사할 파일을 선택해 주세요.", Toast.LENGTH_SHORT ).show();
                    Toast.makeText( getApplicationContext(), getString(R.string.activity_recorded_list__layout_button__copy_internal_file_to_external__toast_select_file_to_copy), Toast.LENGTH_SHORT ).show();
                    return;
                }

                final String dst_dir = m_main_app.PUBLIC_HOME_DIR;

                //m_main_app.copy_internal_file_to_public_download( src_list, dst_dir );
                m_main_app.run_with_progress( Activity_RecordedList.this,
                        m_main_app.RUN_TYPE__COPY_INTERNAL_FILE_TO_PUBLIC_DOWNLOAD,
                        src_list, dst_dir );
            }
        });

        button_delete_internal_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int count = 0;
                if ( (m_list_items != null) && !m_list_items.isEmpty() ) {
                    for ( int i = 0; i < m_list_items.size(); i++ ) {
                        HashMap<String, Object> map = m_list_items.get(i);
                        if (map != null) {
                            final boolean checked = (Boolean) map.get( LIST_ITEM_TAG__CHECKED );
                            if ( checked ) count += 1;
                        }
                    }
                }

                Log.d( TAG, "checked files: " + count );

                if ( count <= 0 ) {
                    //Toast.makeText( getApplicationContext(), "삭제할 파일을 선택해 주세요.", Toast.LENGTH_SHORT ).show();
                    Toast.makeText( getApplicationContext(), getString(R.string.activity_recorded_list__layout_button_delete_internal_file__toast_select_file_to_delete), Toast.LENGTH_SHORT ).show();
                    return;
                }

                //delete_record_files();
                dlg_message_delete_record_files( count );
            }
        });

        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity_RecordedList.this.finish();
            }
        });

        checkbox_select_all.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if ( m_listviewAdapter != null ) {
                    // pass updates if select one on listview when CheckBox_SelectAll is checked.
                    if ( m_checkbox_select_all_pass ) return;

                    m_listviewAdapter.setCheckAll( b );

                    for ( int i = 0; i < m_list_items.size(); i++ ) {
                        setCheckItem( i, b );
                    }
                }
            }
        });


        {
            m_listviewAdapter = new ListArrayAdapter( this,
                    //R.layout.activity_recorded_listview_item, new String[] {"dummy1", "dummy2"} );
                    R.layout.activity_recorded_listview_item, new Object[] {m_list_items} );
            m_listview = (ListView)findViewById( R.id.ListView_recorded_list );

            if ( (m_listview != null) && (m_listviewAdapter != null) ) {
                m_listview.setAdapter(m_listviewAdapter);
            }

            m_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //final long sel_item = adapterView.getItemIdAtPosition( i );

                    //Log.d( TAG, "onItemClick(): idx = " + i );

                    if ( !m_selected_mode ) {
                        {
                            // play recorded file
                            if ( (m_list_items != null) && !m_list_items.isEmpty() ) {
                                HashMap<String, Object> map = m_list_items.get(i);

                                if (map != null && m_main_app != null) {
                                    final String filename = m_main_app.INTERNAL_RECORD_DIR + File.separator + (String) map.get(LIST_ITEM_TAG__FILENAME);
                                    //final String size = (String) map.get(LIST_ITEM_TAG__FILESIZE);

                                    //Log.d( TAG, "onItemClick(): filename: " + filename + ", size = " + size );

                                    // Open URL
                                    if ( !filename.isEmpty() ) {
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.putExtra(m_main_app.ACTIVITY_RECORDED_LIST__INTENT_KEY__FILENAME, filename);
                                        setResult(m_main_app.ACTIVITY_RESULT_CODE__ACTIVITY_RECORDED_LIST, intent);
                                        finish();
                                    }
                                }
                            }
                        }

                        return;
                    }

                    CheckBox checkbox_select = view.findViewById( R.id.CheckBox_select );
                    if ( checkbox_select != null ) {
                        /*
                        checkbox_select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                if ( !m_selected_mode ) {
                                    return;
                                }
                                setCheckItem( i, checkbox_select.isChecked() );
                            }
                        });
//                        checkbox_select.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                checkbox_select.setChecked( !checkbox_select.isChecked() );
//                                setCheckItem( i, checkbox_select.isChecked() );
//                            }
//                        });
                        */

                        // pass updates if select one on listview when CheckBox_SelectAll is checked.
                        m_checkbox_select_all_pass = true;
                        checkbox_select_all.setChecked( false );
                        m_checkbox_select_all_pass = false;

                        checkbox_select.setChecked( !checkbox_select.isChecked() );
                        setCheckItem( i, checkbox_select.isChecked() );
                    }
                }
            });

            m_listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    m_selected_mode = !m_selected_mode;
                    m_listviewAdapter.setShowCheckBoxAll( m_selected_mode );

                    int visibility = View.GONE;

                    if ( m_selected_mode ) visibility = View.VISIBLE;
                    else visibility = View.GONE;

                    CheckBox checkBox_select_all = (CheckBox) findViewById( R.id.CheckBox_select_all );
                    if ( checkBox_select_all != null ) {
                        checkBox_select_all.setVisibility( visibility );
                    }

                    if ( !m_selected_mode ) {
                        for (int x = 0; x < m_list_items.size(); x++) {
                            setCheckItem( x, false );
                        }
                    }


                    //Log.d( TAG, "onItemLongClick(): idx = " + i );
                    return true;
                }
            });
        }

    }

    private void release() {
        m_listviewAdapter = null;
        if ( m_listview != null ) {
            m_listview.setAdapter( null );
            m_listview = null;
        }

        if ( m_list_items != null ) {
            m_list_items.clear();
            m_list_items = null;
        }
    }

    private void delete_record_files() {
        if ( m_list_items == null || m_list_items.isEmpty() ) return;

        ArrayList<String> src_list = new ArrayList<String>();

        for ( int i = 0; i < m_list_items.size(); i++ ) {
            HashMap<String, Object> map = m_list_items.get( i );
            final String src = (String) map.get( LIST_ITEM_TAG__FILENAME );
            final String src_filesize = (String) map.get( LIST_ITEM_TAG__FILESIZE );
            final Boolean checked = (Boolean) map.get( LIST_ITEM_TAG__CHECKED );

            if ( !checked ) continue;

            src_list.add( src );

            //Log.d( TAG, "(delete file) selected filename: " +
            //        "[" + i + "/" + m_list_items.size() + "] " + src );
            Log.d( TAG, "(delete file) selected filename: " + "[" + i + "] " + src );
        }

        Log.d( TAG, "delete item size = " + src_list.size() );
        for ( int i = 0; i < src_list.size(); i++ ) {
            final String val = src_list.get(i);
            setDeleteItem( val );
        }
        m_listviewAdapter.updates();

        //m_main_app.delete_internal_file( src_list );
        //
        //! DO NOT PASS 'getApplicationContext()' here.
        // Dialog -> run this method (delete_record_files()) ->
        // m_main_app.run_with_progress() -> ProgressDialog -> return
        //
        // Cannot return View after destroy progress.
        // USE '<Activity>.this' instead of 'getApplicationContext()'.
        m_main_app.run_with_progress( Activity_RecordedList.this,
                m_main_app.RUN_TYPE__DELETE_INTERNAL_FILE,
                src_list, null );
    }

    private void setCheckItem(final int i, final Boolean checked) {
        if ( (m_list_items != null) && !m_list_items.isEmpty() ) {
            HashMap<String, Object> map = m_list_items.get(i);
            if (map != null) {
                //Log.d( TAG, "setCheckItem(): filename: " + (String) map.get(LIST_ITEM_TAG__FILENAME) );
                //Log.d( TAG, "setCheckItem(): filesize: " + (String) map.get(LIST_ITEM_TAG__FILESIZE) );
                //Log.d( TAG, "setCheckItem(): checked: " + checked );
                map.put( LIST_ITEM_TAG__CHECKED, (Boolean) checked );
            }
        }
    }

    private void setDeleteItem(final String val) {
        if ( (m_list_items != null) && !m_list_items.isEmpty() ) {
            for ( var v: m_list_items ) {
                final String str = (String) v.get(LIST_ITEM_TAG__FILENAME);
                if ( str != null && str.equals(val) ) {
                    m_list_items.remove( v );
                    break;
                }
            }
        }
    }

    private void dlg_message_delete_record_files(final int count) {
        //final String strTitle = "녹화된 파일 삭제";
        //final String strMessage = "선택한 파일을 삭제합니다.\n(" + count + ") 개 파일 삭제";
        final String strTitle = getString(R.string.activity_recorded_list__dlg_message_delete_record_files__delete_recorded_file);
        final String strMessage = getString(R.string.activity_recorded_list__dlg_message_delete_record_files__delete_selected_1)
                + count + getString(R.string.activity_recorded_list__dlg_message_delete_record_files__delete_selected_2);
        final AlertDialog.Builder winAlert;
        Dialog winDialog;
        LayoutInflater li = LayoutInflater.from( this );
        View viewDlgMessageBox = li.inflate( R.layout.dlg_messagebox, null );

        // Back Key
        DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub

                if ( keyCode == KeyEvent.KEYCODE_BACK )
                    return true;

                return false;
            }
        };

        DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

                // OK
                if ( which == -1 ) {
                    // YES (OK)
                    Log.d( TAG, "dlg_message_delete_record_files(): Button: SELECTED #1 YES (OK)" );

                    delete_record_files();
                }
                // Cancel #1
                else if ( which == -3 ) {
                    // NO
                    Log.d( TAG, "dlg_message_delete_record_files(): Button: SELECTED #2 NO" );
                }
                // Cancel #2
                else if ( which == -2 ) {
                    // CANCEL
                    Log.d( TAG, "dlg_message_delete_record_files(): Button: SELECTED #3 CANCEL" );
                }

                dialog.cancel();
                return;
            }
        };

        winAlert = new AlertDialog.Builder( this )
                .setIcon( R.mipmap.ic_menu_delete )
                .setOnKeyListener( keyListener )
                .setTitle( strTitle )
                .setMessage( strMessage )
                // getResources().getString(R.string.dlgBtn_YES)
                .setPositiveButton( getString(R.string.activity_recorded_list__dlg_message_delete_record_files__delete), okListener )
                .setNeutralButton( getString(R.string.activity_recorded_list__dlg_message_delete_record_files__cancel), okListener )
                //.setNegativeButton( "취소", okListener )
                .setView( viewDlgMessageBox );
        if ( winAlert != null ) {
            winDialog = winAlert.create();
            if ( winDialog != null ) {
                winDialog.show();
            }
        }
    }

    private class ListArrayAdapter extends ArrayAdapter {
        private Context m_context = null;
        private LayoutInflater m_inflater = null;
        private ArrayList<HashMap<String, Object>> m_list_items = null;
        private int m_list_items_count = 0;
        private boolean m_show_checkbox_all = false;
        private boolean m_check_all = false;

        public ListArrayAdapter(@NonNull Context context, int resource, @NonNull Object[] objects) {
            super(context, resource, objects);

            m_context = context;
            m_list_items = (ArrayList<HashMap<String, Object>>)objects[0];
            m_inflater = (LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        }

        @Override
        public int getCount() {
            //return super.getCount();

            m_list_items_count = 0;
            if ( m_list_items != null )
                m_list_items_count = m_list_items.size();

            return m_list_items_count;
        }
        /*
        @Nullable
        @Override
        public Object getItem(int position) {
            return super.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }
        */

        public void setShowCheckBoxAll(boolean val) {
            m_show_checkbox_all = val;
            notifyDataSetChanged();
        }

        public void setCheckAll(boolean val) {
            m_check_all = val;
            notifyDataSetChanged();
        }

        public void updates() {
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            //return super.getView(position, convertView, parent);

            View v = convertView;

            if ( m_inflater == null ) {
                return null;
            }

            if ( (m_list_items == null) || (m_list_items.isEmpty()) )
                return null;

            if ( convertView == null ) {
                v = m_inflater.inflate( R.layout.activity_recorded_listview_item, null );
                if ( v == null ) return null;

                v.setTag( R.id.CheckBox_select, v.findViewById(R.id.CheckBox_select) );
                v.setTag( R.id.TextView_filename, v.findViewById(R.id.TextView_filename) );
                v.setTag( R.id.TextView_filesize, v.findViewById(R.id.TextView_filesize) );
            }

            if ( m_show_checkbox_all ) {
                ((CheckBox) v.getTag(R.id.CheckBox_select)).setVisibility(View.VISIBLE);
            }
            else {
                ((CheckBox) v.getTag(R.id.CheckBox_select)).setVisibility(View.GONE);
            }

            ((CheckBox) v.getTag(R.id.CheckBox_select)).setChecked( m_check_all );

            //if ( position == (getCount() - 1) ) {
            //    Log.d( TAG, "pos = " + position + ", count = " + getCount() );
            //}


            HashMap<String, Object> map = m_list_items.get( position );

            if ( map != null ) {
                TextView tv_filename = (TextView) v.getTag(R.id.TextView_filename);
                TextView tv_filesize = (TextView) v.getTag(R.id.TextView_filesize);

                String filename = (String) map.get( LIST_ITEM_TAG__FILENAME );
                String filesize = (String) map.get( LIST_ITEM_TAG__FILESIZE );

                if ( (filename != null) && (tv_filename != null) ) {
                    tv_filename.setText( filename );
                }

                if ( (filesize != null) && tv_filesize != null ) {
                    tv_filesize.setText( filesize );
                }
            }

            return v;
        }

    }
}