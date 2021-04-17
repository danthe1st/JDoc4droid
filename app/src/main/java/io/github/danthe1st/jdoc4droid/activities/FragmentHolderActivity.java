package io.github.danthe1st.jdoc4droid.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SearchView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.danthe1st.jdoc4droid.BuildConfig;
import io.github.danthe1st.jdoc4droid.R;
import lombok.Getter;

public class FragmentHolderActivity extends AppCompatActivity {

    @Getter
    private Deque<AbstractFragment> currentFragments = new LinkedList<>();

    private Map<Integer, Runnable> keyListeners = new HashMap<>();

    @Getter
    private ExecutorService threadPool = Executors.newSingleThreadExecutor();

    @Getter
    private SearchView searchView;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Runnable listener = keyListeners.get(keyCode);
        if (listener != null) {
            listener.run();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setListener(int key, Runnable listener) {
        keyListeners.put(key, listener);
    }

    public Runnable getListener(int key) {
        return keyListeners.get(key);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BuildConfig.DEBUG) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFragments.isEmpty()) {
            super.onBackPressed();
        } else {
            currentFragments.peek().goBack();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                AbstractFragment currentFragment=currentFragments.peek();
                if (currentFragment != null&&currentFragment.getView()!=null) {
                    currentFragment.onSearch(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                AbstractFragment currentFragment=currentFragments.peek();
                if (currentFragment != null&&currentFragment.getView()!=null) {
                    currentFragment.onSearchType(newText);
                }
                return true;
            }
        });

        AbstractFragment currentFragment = currentFragments.peek();
        if(currentFragment!=null&&currentFragment.supportsSearch()){
            searchView.setVisibility(View.VISIBLE);
        }else{
            searchView.setVisibility(View.GONE);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    }

    @Override
    public boolean onSearchRequested() {
        return super.onSearchRequested();
    }
}
