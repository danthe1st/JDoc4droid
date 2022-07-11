package io.github.danthe1st.jdoc4droid.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.AnyThread;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import net.steamcrafted.materialiconlib.MaterialMenuInflater;

import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.ui.settings.SettingsActivity;

public class AbstractActivity extends AppCompatActivity {

	private final Map<Integer, Runnable> keyListeners = new HashMap<>();

	private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

	private SearchView searchView;

	private MenuItem shareButton;
	private MenuItem settingsButton;
	private MenuItem openExternalButton;
	private MenuItem copyToClipboardButton;

	private Toolbar actionBarToolbar;

	protected static final String ARG_SHARE_URL = "shareUrl";
	private String shareUrl;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Runnable listener = keyListeners.get(keyCode);
		if(listener != null) {
			listener.run();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		shareUrl = getIntent().getStringExtra(ARG_SHARE_URL);
		if(getSearchView() != null) {
			reloadTopMenuButtons();
			getSearchView().setQuery("", false);
		}
	}

	@Override
	public void setContentView(@LayoutRes int layoutResID) {
		super.setContentView(layoutResID);
		getActionBarToolbar();
	}

	@UiThread
	protected Toolbar getActionBarToolbar() {
		if(actionBarToolbar == null) {
			actionBarToolbar = findViewById(R.id.include);
			if(actionBarToolbar != null) {
				setSupportActionBar(actionBarToolbar);
			}
		}
		return actionBarToolbar;
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	public SearchView getSearchView() {
		return searchView;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MaterialMenuInflater
				.with(this)
				.setDefaultColor(R.color.contrastColor)
				.setDefaultColorResource(R.color.contrastColor)
				.inflate(R.menu.top_menu, menu);
		loadOptionsMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}

	@UiThread
	protected void loadOptionsMenu(Menu menu) {
		searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
		shareButton = menu.findItem(R.id.app_bar_share);
		openExternalButton = menu.findItem(R.id.app_bar_open_external);
		copyToClipboardButton = menu.findItem(R.id.app_bar_copy_to_clipboard);
		settingsButton = menu.findItem(R.id.app_bar_settings);

		EditText searchEditText = searchView.findViewById(searchView.getContext()
				.getResources()
				.getIdentifier("android:id/search_src_text", null, null));
		searchEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				onSearch(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				onSearchType(newText);
				return true;
			}
		});

		reloadTopMenuButtons();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == R.id.app_bar_share) {
			onShareButton();
		} else if(item.getItemId() == R.id.app_bar_settings) {
			onSettingsButton();
		} else if(item.getItemId() == R.id.app_bar_open_external) {
			onOpenExternalButton();
		} else if(item.getItemId() == R.id.app_bar_copy_to_clipboard) {
			onCopyToClipboardButton();
		}
		return super.onOptionsItemSelected(item);
	}

	@UiThread
	private void onCopyToClipboardButton() {
		String shareLink = getShareLink();
		if(shareLink != null) {
			ClipboardManager clipboardManager = getSystemService(ClipboardManager.class);
			clipboardManager.setPrimaryClip(ClipData.newRawUri("Javadoc provided by JDOc4Droid", Uri.parse(shareLink)));
		}
	}

	@UiThread
	private void onOpenExternalButton() {
		String shareLink = getShareLink();
		if(shareLink != null) {
			Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(shareLink));
			Intent shareIntent = Intent.createChooser(sendIntent, null);
			startActivity(shareIntent);
		}
	}

	@UiThread
	private void onShareButton() {
		String shareLink = getShareLink();
		if(shareLink != null) {
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.setType("text/plain");
			sendIntent.putExtra(Intent.EXTRA_TEXT, shareLink);
			Intent shareIntent = Intent.createChooser(sendIntent, null);
			startActivity(shareIntent);
		}
	}

	@UiThread
	private void onSettingsButton() {
		SettingsActivity.open(this);
	}


	@UiThread
	public void reloadTopMenuButtons() {
		if(supportsSearch()) {
			searchView.setVisibility(View.VISIBLE);
		} else {
			searchView.setVisibility(View.GONE);
		}
		boolean openVisible = getShareLink() != null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Set<String> openJavadocTypesValues = prefs.getStringSet("openJavadocsTypesPreference", Collections.emptySet());
		shareButton.setVisible(openVisible && openJavadocTypesValues.contains("share"));
		openExternalButton.setVisible(openVisible && openJavadocTypesValues.contains("open_external"));
		copyToClipboardButton.setVisible(openVisible && openJavadocTypesValues.contains("clipboard"));
	}

	public void runInUIThread(Runnable toRun) {
		new Handler(Looper.getMainLooper()).post(toRun);
	}

	@UiThread
	public void onSearch(String search) {
		//default implementation
		Log.w(getClass().getName(), "default implementation of onSearch() called");
	}

	@UiThread
	public void onSearchType(String search) {
		//default implementation
	}

	public boolean supportsSearch() {
		return false;
	}

	public String getShareLink() {
		return shareUrl;
	}

	@AnyThread
	protected Void showError(@StringRes int errorMessage, Throwable e) {
		if(e instanceof UncheckedIOException && e.getCause() != null) {
			e = e.getCause();
		}
		Log.e(this.getClass().getName(), getResources().getString(errorMessage), e);
		runInUIThread(() -> Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show());
		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		threadPool.shutdown();
	}
}
