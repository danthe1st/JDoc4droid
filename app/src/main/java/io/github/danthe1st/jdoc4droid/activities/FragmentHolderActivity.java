package io.github.danthe1st.jdoc4droid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.danthe1st.jdoc4droid.BuildConfig;
import io.github.danthe1st.jdoc4droid.R;
import lombok.Getter;

public class FragmentHolderActivity extends AppCompatActivity {

    @Getter
    private final Deque<AbstractFragment> currentFragments = new LinkedList<>();

    private final Map<Integer, Runnable> keyListeners = new HashMap<>();

    @Getter
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    @Getter
    private SearchView searchView;

    private MenuItem shareButton;

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
        if (BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
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
        if(savedInstanceState!=null){
            String[] currentFragmentIds = savedInstanceState.getStringArray("currentFragmentTags");
            if(currentFragmentIds!=null){
                for (String fragTag : currentFragmentIds) {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(fragTag);
                    if(currentFragment instanceof AbstractFragment){
                        currentFragments.addLast((AbstractFragment) currentFragment);
                    }
                }
                Log.i(FragmentHolderActivity.class.getCanonicalName(), "onCreate: loaded "+currentFragments.size()+" fragments: "+currentFragments);
                AbstractFragment last = currentFragments.peekFirst();
                Log.i(FragmentHolderActivity.class.getCanonicalName(), "onCreate: use fragment: "+last);
                if(last!=null){
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragHolder, last).attach(last).commit();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String[] tags=currentFragments.stream()
                .map(Fragment::getTag)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        outState.putStringArray("currentFragmentTags",tags);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);

        searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();

        shareButton = menu.findItem(R.id.app_bar_share);

        EditText searchEditText = searchView.findViewById( searchView.getContext()
                .getResources()
                .getIdentifier("android:id/search_src_text", null, null));
        searchEditText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
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
        if (currentFragment == null) {
            searchView.setVisibility(View.GONE);
            shareButton.setVisible(false);
        } else {
            reloadTopMenuButtons(currentFragment);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.app_bar_share) {
            onShareButton();
        }
        return super.onOptionsItemSelected(item);
    }

    private void onShareButton(){
        AbstractFragment currentFragment=currentFragments.peek();
        String shareLink=currentFragment.getShareLink();
        if(shareLink!=null){
            Intent sendIntent=new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT,shareLink);
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        }
    }

    public void reloadTopMenuButtons(AbstractFragment currentFragment){
        if (currentFragment.supportsSearch()) {
            searchView.setVisibility(View.VISIBLE);
        } else {
            searchView.setVisibility(View.GONE);
        }
        shareButton.setVisible(currentFragment.getShareLink() != null);
    }

}
