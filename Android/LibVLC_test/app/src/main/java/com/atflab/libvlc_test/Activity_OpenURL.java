package com.atflab.libvlc_test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.widget.EditText;
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

public class Activity_OpenURL extends AppCompatActivity {
    private static final String TAG = "Activity_OpenURL";

    private App m_main_app = null;
    private ListView m_listview = null;
    private ListArrayAdapter m_listviewAdapter = null;
    private ArrayList<HashMap<String, Object>> m_list_items = null;
    private final String LIST_ITEM_TAG__TITLE = "title";
    private final String LIST_ITEM_TAG__URL = "url";
    private final String LIST_ITEM_TAG__CHECKED = "checked";
    private Boolean m_selected_mode = false;
    private HashMap<String, Object> m_map_selected_url = new HashMap<String, Object>();

    // pass updates if select one on listview when CheckBox_SelectAll is checked.
    private boolean m_checkbox_select_all_pass = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_open_url_listview);
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
            m_main_app.load_contents( m_list_items );
        }

        //Button button_open_url = (Button) findViewById(R.id.Button_open_url);
        //Button button_add_url_test = (Button) findViewById(R.id.Button_add_url_test);
        Button button_add_url = (Button) findViewById(R.id.Button_add_url);
        //Button button_edit_url = (Button) findViewById(R.id.Button_edit_url);
        Button button_delete_url = (Button) findViewById(R.id.Button_delete_url);
        Button button_cancel = (Button) findViewById(R.id.Button_cancel);
        CheckBox checkbox_select_all = (CheckBox) findViewById( R.id.CheckBox_select_all );

//        if ( button_open_url != null ) {
//            button_open_url.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    final String url = (String) m_map_selected_url.get( LIST_ITEM_TAG__URL );
//
//                    Intent intent = new Intent( getApplicationContext(), MainActivity.class );
//                    intent.putExtra( m_main_app.ACTIVITY_OPEN_URL__INTENT_KEY__URL, url );
//                    setResult( m_main_app.ACTIVITY_RESULT_CODE__ACTIVITY_OPEN_URL, intent );
//                    finish();
//                }
//            });
//        }

//        if ( button_add_url_test != null ) {
//            button_add_url_test.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    add_url_test();
//                }
//            });
//        }

        if ( button_add_url != null ) {
            button_add_url.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dlg_message_edit_open_url( false, -1, null, null );
                }
            });
        }

//        if ( button_edit_url != null ) {
//            button_edit_url.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if ( m_map_selected_url == null ) return;
//
//                    final String title = (String) m_map_selected_url.get( LIST_ITEM_TAG__TITLE );
//                    final String url = (String) m_map_selected_url.get( LIST_ITEM_TAG__URL );
//                    dlg_message_edit_open_url( true, title, url );
//                }
//            });
//        }

        if ( button_delete_url != null ) {
            button_delete_url.setOnClickListener(new View.OnClickListener() {
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

                    Log.d( TAG, "checked urls: " + count );

                    if ( count <= 0 ) {
                        Toast.makeText( getApplicationContext(), "삭제할 목록을 선택해 주세요.", Toast.LENGTH_SHORT ).show();
                        return;
                    }

                    dlg_message_delete_urls( count );
                }
            });
        }

        if ( button_cancel != null ) {
            button_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Activity_OpenURL.this.finish();
                }
            });
        }

        if ( checkbox_select_all != null ) {
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
        }

        {
            m_listviewAdapter = new ListArrayAdapter( this,
                    //R.layout.activity_open_url_listview_item, new String[] {"dummy1", "dummy2"} );
                    R.layout.activity_open_url_listview_item, new Object[] {m_list_items} );
            m_listview = (ListView)findViewById( R.id.ListView_stream_url_list );

            if ( (m_listview != null) && (m_listviewAdapter != null) ) {
                m_listview.setAdapter(m_listviewAdapter);
            }

            m_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    //final long sel_item = adapterView.getItemIdAtPosition( i );

                    //Log.d( TAG, "onItemClick(): idx = " + i );

                    if ( !m_selected_mode ) {
                        if ( (m_list_items != null) && !m_list_items.isEmpty() ) {
                            HashMap<String, Object> map = m_list_items.get(i);
                            if (map != null) {
                                String title = (String) map.get( LIST_ITEM_TAG__TITLE );
                                String url = (String) map.get( LIST_ITEM_TAG__URL );

                                m_map_selected_url.put( LIST_ITEM_TAG__TITLE, title );
                                m_map_selected_url.put( LIST_ITEM_TAG__URL, url );

                                dlg_message_edit_open_url( true, i, title, url );
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
                    //m_selected_mode = true;
                    //m_listviewAdapter.setShowCheckBoxAll( true );

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

            m_listviewAdapter.updates();
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

    private void add_url_test() {
        ArrayList<String[]> url_list = new ArrayList<String[]>();

        url_list.add( new String[]{"EBS1", "https://ebsonair.ebs.co.kr/ebs1familypc/familypc1m/playlist.m3u8"} );

        // public test videos
        // Source: https://gist.github.com/jsturgis/3b19447b304616f18657
        url_list.add( new String[]{"test: Big Buck Bunny", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"} );
        url_list.add( new String[]{"test: Elephant Dream", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"} );
        url_list.add( new String[]{"test: For Bigger Blazes", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"} );
        url_list.add( new String[]{"test: For Bigger Escape", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"} );
        url_list.add( new String[]{"test: For Bigger Fun", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"} );
        url_list.add( new String[]{"test: For Bigger Joyrides", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"} );
        url_list.add( new String[]{"test: Sintel", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"} );
        url_list.add( new String[]{"test: Subaru Outback On Street And Dirt", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"} );
        url_list.add( new String[]{"test: Tears of Steel", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"} );
        url_list.add( new String[]{"test: Volkswagen GTI Review", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4"} );
        url_list.add( new String[]{"test: We Are Going On Bullrun", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"} );
        url_list.add( new String[]{"test: What care can you get for a grand?", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"} );

        for ( var v: url_list ) {
            add_url( -1, v[0], v[1] );
        }
    }

    // final int pos: (-1) add a new one, (>=0) edit pos
    private void add_url(final int pos, final String title, final String url) {
        //Log.d( TAG, "add_url: pos = " + pos );
        //Log.d( TAG, "add_url: title = " + title );
        //Log.d( TAG, "add_url: url = " + url );

        if ( m_list_items == null ) {
            Log.d( TAG, "add_url: list == NULL" );
            return;
        }

        HashMap<String, Object> map = null;

        if ( pos < 0 ) {
            Log.d( TAG, "add_url: add a new one" );
            // add a new one
            map = new HashMap<String, Object>();
        }
        else {
            Log.d( TAG, "add_url: edit position" );
            // edit pos
            map = m_list_items.get(pos);
        }

        if (map != null) {
            map.put(LIST_ITEM_TAG__CHECKED, false);
            map.put(LIST_ITEM_TAG__TITLE, title);
            map.put(LIST_ITEM_TAG__URL, url);

            if ( pos < 0 ) {
                m_list_items.add(map);
            }
        }

        // save a file
        m_main_app.save_stream_urls( m_list_items );

        m_listviewAdapter.updates();
    }

    private void delete_url() {
        if ( m_list_items == null || m_list_items.isEmpty() ) return;

        ArrayList<String> src_list = new ArrayList<String>();

        for ( int i = 0; i < m_list_items.size(); i++ ) {
            HashMap<String, Object> map = m_list_items.get( i );
            final String src = (String) map.get( LIST_ITEM_TAG__URL );
            final Boolean checked = (Boolean) map.get( LIST_ITEM_TAG__CHECKED );

            if ( !checked ) continue;

            src_list.add( src );

            Log.d( TAG, "(delete url) selected url: " + "[" + i + "] " + src );
        }

        Log.d( TAG, "delete item size = " + src_list.size() );
        for ( int i = 0; i < src_list.size(); i++ ) {
            final String val = src_list.get(i);
            setDeleteItem( val );
        }
        m_listviewAdapter.setCheckAll( false );
        //m_listviewAdapter.updates();

        // save
        m_main_app.save_stream_urls( m_list_items );
    }

    private void setCheckItem(final int i, final Boolean checked) {
        if ( (m_list_items != null) && !m_list_items.isEmpty() ) {
            HashMap<String, Object> map = m_list_items.get(i);
            if (map != null) {
                //Log.d( TAG, "setCheckItem(): title: " + (String) map.get(LIST_ITEM_TAG__TITLE) );
                //Log.d( TAG, "setCheckItem(): url: " + (String) map.get(LIST_ITEM_TAG__URL) );
                //Log.d( TAG, "setCheckItem(): checked: " + checked );
                map.put( LIST_ITEM_TAG__CHECKED, (Boolean) checked );
            }
        }
    }

    private void setDeleteItem(final String val) {
        if ( (m_list_items != null) && !m_list_items.isEmpty() ) {
            for ( var v: m_list_items ) {
                final String str = (String) v.get(LIST_ITEM_TAG__URL);
                if ( str != null && str.equals(val) ) {
                    m_list_items.remove( v );
                    break;
                }
            }
        }
    }

    // final boolean edit: (true) edit pos, (false) add a new one
    // final int pos: (-1) add a new one, (>=0) edit pos
    private void dlg_message_edit_open_url(final boolean edit,
                                           final int pos, final String title, final String url) {
        final String strTitle = "Stream URL 입력";
        final String strMessage = "";
        final AlertDialog.Builder winAlert;
        Dialog winDialog;
        LayoutInflater li = LayoutInflater.from( this );
        View viewDlgMessageBox = li.inflate( R.layout.dlg_messagebox_open_url, null );

        {
            EditText et_title = (EditText) viewDlgMessageBox.findViewById(R.id.EditText_title);
            EditText et_url = (EditText) viewDlgMessageBox.findViewById(R.id.EditText_url);

            if ( et_title != null ) {
                et_title.setText( title );
            }

            if ( et_url != null ) {
                et_url.setText( url );
            }
        }

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

                String title = "";
                String url = "";
                EditText et_title = (EditText) viewDlgMessageBox.findViewById( R.id.EditText_title );
                EditText et_url = (EditText) viewDlgMessageBox.findViewById( R.id.EditText_url );

                if ( et_title != null ) {
                    title = et_title.getText().toString();
                }

                if ( et_url != null ) {
                    url = et_url.getText().toString();
                }

                // OK
                if ( which == -1 ) {
                    // YES (OK)
                    Log.d( TAG, "dlg_message_edit_open_url(): Button: SELECTED #1 YES (OK)" );

                    // Add or Edit
                    {
                        if ( !title.isEmpty() && !url.isEmpty() ) {
                            Log.d( TAG, "title = " + title );
                            Log.d( TAG, "url = " + url );
                            add_url( pos, title, url );
                        }
                        else {
                            Toast.makeText( Activity_OpenURL.this, "내용을 입력해 주세요.", Toast.LENGTH_SHORT ).show();;
                            return;
                        }
                    }
                }
                // Cancel #1
                else if ( which == -3 ) {
                    // NO
                    Log.d( TAG, "dlg_message_edit_open_url(): Button: SELECTED #2 NO" );
                }
                // Cancel #2
                else if ( which == -2 ) {
                    // CANCEL
                    Log.d( TAG, "dlg_message_edit_open_url(): Button: SELECTED #3 CANCEL" );

                    {
                        // Open URL
                        if ( !title.isEmpty() && !url.isEmpty() ) {
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra(m_main_app.ACTIVITY_OPEN_URL__INTENT_KEY__URL, url);
                            intent.putExtra(m_main_app.ACTIVITY_OPEN_URL__INTENT_KEY__TITLE, title);
                            setResult(m_main_app.ACTIVITY_RESULT_CODE__ACTIVITY_OPEN_URL, intent);
                            finish();
                        }
                    }
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
                .setPositiveButton( (edit? "수정" : "추가"), okListener ) // -1
                .setNeutralButton( "취소", okListener ) // -2
                .setNegativeButton( "열기", okListener ) // -3
                .setView( viewDlgMessageBox );
        if ( winAlert != null ) {
            winDialog = winAlert.create();
            if ( winDialog != null ) {
                winDialog.show();
            }
        }
    }

    private void dlg_message_delete_urls(final int count) {
        final String strTitle = "Stream URL 삭제";
        final String strMessage = "선택한 목록을 삭제합니다.\n(" + count + ") 개 삭제";
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
                    Log.d( TAG, "dlg_message_delete_urls(): Button: SELECTED #1 YES (OK)" );

                    delete_url();
                }
                // Cancel #1
                else if ( which == -3 ) {
                    // NO
                    Log.d( TAG, "dlg_message_delete_urls(): Button: SELECTED #2 NO" );
                }
                // Cancel #2
                else if ( which == -2 ) {
                    // CANCEL
                    Log.d( TAG, "dlg_message_delete_urls(): Button: SELECTED #3 CANCEL" );
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
                .setPositiveButton( "삭제", okListener )
                .setNeutralButton( "취소", okListener )
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
        private Boolean m_show_checkbox_all = false;
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

        public void setShowCheckBoxAll(Boolean val) {
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
                v = m_inflater.inflate( R.layout.activity_open_url_listview_item, null );
                if ( v == null ) return null;

                v.setTag( R.id.CheckBox_select, v.findViewById(R.id.CheckBox_select) );
                v.setTag( R.id.TextView_title, v.findViewById(R.id.TextView_title) );
                v.setTag( R.id.TextView_url, v.findViewById(R.id.TextView_url) );
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
                TextView tv_title = (TextView) v.getTag(R.id.TextView_title);
                TextView tv_url = (TextView) v.getTag(R.id.TextView_url);

                String title = (String) map.get( LIST_ITEM_TAG__TITLE );
                String url = (String) map.get( LIST_ITEM_TAG__URL );

                if ( (title != null) && (tv_title != null) ) {
                    tv_title.setText( title );
                }

                if ( (url != null) && (tv_url != null) ) {
                    tv_url.setText( url );
                }
            }

            return v;
        }
    }
}