package io.github.danthe1st.jdoc4droid.ui.list.javadocs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.github.danthe1st.jdoc4droid.BuildConfig;
import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.model.JavaDocType;
import io.github.danthe1st.jdoc4droid.ui.OracleDownloaderActivity;
import io.github.danthe1st.jdoc4droid.ui.list.AbstractListActivity;
import io.github.danthe1st.jdoc4droid.ui.list.classes.ListClassesActivity;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;

public class ListJavadocsActivity extends AbstractListActivity<JavaDocInformation, ListJavaDocsViewAdapter> {

	private List<JavaDocInformation> javaDocInfos;
	private ActivityResultLauncher<Object> zipLauncher;

	private ProgressBar progressBar;

	public ListJavadocsActivity() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_list_javadocs_list);
		super.onCreate(savedInstanceState);
		findViewById(R.id.downloadBtn).setOnClickListener(this::downloadBtnClicked);
		progressBar = findViewById(R.id.downloadProgressBar);
		adapter.setOnSelect(javaDocInformation -> {
			findViewById(R.id.deleteBtn).setVisibility(javaDocInformation == null ? View.INVISIBLE : View.VISIBLE);
			findViewById(R.id.updateBtn).setVisibility(javaDocInformation == null || javaDocInformation.getBaseDownloadUrl().isEmpty() ? View.INVISIBLE : View.VISIBLE);
			updateMoveButtons(javaDocInformation);
		});
		findViewById(R.id.deleteBtn).setOnClickListener(this::deleteSelectedJavadoc);
		findViewById(R.id.updateBtn).setOnClickListener(this::updateSelectedJavadoc);
		findViewById(R.id.moveUpBtn).setOnClickListener(this::moveJavadocUp);
		findViewById(R.id.moveDownBtn).setOnClickListener(this::moveJavadocDown);
		getThreadPool().execute(() -> JavaDocDownloader.clearCacheIfNoDownloadInProgress(this));
		if(BuildConfig.DEBUG && Debug.isDebuggerConnected()) {
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitDiskReads().detectAll().penaltyLog().build());
		}

		zipLauncher = registerForActivityResult(new ActivityResultContract<Object, Uri>() {
			@NonNull
			@Override
			public Intent createIntent(@NonNull Context context, Object input) {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("*/*");
				intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/java-archive"});
				return intent;
			}

			@Override
			public Uri parseResult(int resultCode, @Nullable Intent intent) {
				if(intent == null) {
					return null;
				}
				return intent.getData();
			}
		}, result -> {
			if(result != null) {
				progressBar.setVisibility(View.VISIBLE);
				JavaDocDownloader.downloadFromUri(this, result, javaDocInfos.size(), progressBar::setProgress)
						.thenAccept(docInfo -> ListClassesActivity.open(this, docInfo))
						.exceptionally(e -> showError(R.string.importJavadocError, e))
						.handle(this::removeProgressBar);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		getThreadPool().execute(() -> {
			try {
				javaDocInfos = JavaDocDownloader.getAllSavedJavaDocInfos(this);
				runInUIThread(() -> adapter.setItems(new ArrayList<>(javaDocInfos)));
			} catch(IOException e) {
				Toast.makeText(this, R.string.loadJavadocError, Toast.LENGTH_LONG).show();
				Log.e(getClass().getCanonicalName(), "Cannot load Javadocs", e);
			}
		});
	}

	@UiThread
	private void updateMoveButtons(JavaDocInformation javaDocInformation) {
		findViewById(R.id.moveUpBtn).setVisibility(canMoveJavadoc(javaDocInformation, -1) ? View.VISIBLE : View.INVISIBLE);
		findViewById(R.id.moveDownBtn).setVisibility(canMoveJavadoc(javaDocInformation, 1) ? View.VISIBLE : View.INVISIBLE);
	}

	@UiThread
	private void moveJavadocDown(View view) {
		moveSelectedJavadoc(1);
	}

	@UiThread
	private void moveJavadocUp(View view) {
		moveSelectedJavadoc(-1);
	}

	@UiThread
	private void moveSelectedJavadoc(int indexChange) {
		JavaDocInformation info = adapter.getSelectedElement();
		if(!canMoveJavadoc(info, indexChange)) {
			Toast.makeText(this, R.string.moveJavadocError, Toast.LENGTH_LONG).show();
			return;
		}
		int index = adapter.getItems().indexOf(info);
		int actualIndex = javaDocInfos.indexOf(info);
		int newIndex = index + indexChange;
		adapter.getItems().add(newIndex, adapter.getItems().remove(index));
		int actualNewIndex = javaDocInfos.indexOf(adapter.getItems().get(newIndex - indexChange));
		javaDocInfos.add(actualNewIndex, javaDocInfos.remove(actualIndex));
		adapter.notifyItemMoved(index, newIndex);
		for(int i = Math.min(actualIndex, actualNewIndex); i <= Math.max(actualIndex, actualNewIndex); i++) {
			JavaDocInformation effectedJavadoc = javaDocInfos.get(i);
			effectedJavadoc.setOrder(i);
			JavaDocDownloader.saveMetadata(effectedJavadoc)
					.exceptionally(e -> showError(R.string.saveMetadataError, e));
		}
		updateMoveButtons(info);
	}

	private boolean canMoveJavadoc(JavaDocInformation info, int indexChange) {
		if(info == null) {
			return false;
		}
		int index = adapter.getItems().indexOf(info);
		int newIndex = index + indexChange;
		return index >= 0 && index < adapter.getItemCount() && newIndex >= 0 && newIndex < adapter.getItemCount();
	}

	private void updateSelectedJavadoc(View view) {
		JavaDocInformation selected = adapter.getSelectedElement();
		if(selected != null && selected.getType() == JavaDocType.MAVEN) {
			progressBar.setVisibility(View.VISIBLE);
			JavaDocDownloader.updateMavenJavadoc(selected, progressBar::setProgress)
					.thenAccept(docInfo -> runInUIThread(() -> {
						if(docInfo == null) {
							Toast.makeText(this, R.string.javadocUpdateErrorAlreadyLatestVersion, Toast.LENGTH_LONG).show();
						} else {
							ListClassesActivity.open(this, docInfo);
						}
						progressBar.setVisibility(View.GONE);
					}))
					.exceptionally(e -> showError(R.string.javadocUpdateError, e))
					.handle(this::removeProgressBar);
		}
	}

	@UiThread
	private void deleteSelectedJavadoc(View view) {
		JavaDocInformation selected = adapter.getSelectedElement();
		if(selected != null) {
			getThreadPool().execute(() -> {
				try {
					deleteRecursive(selected.getDirectory());
					runInUIThread(() -> {
						int indexToRemove = adapter.getItems().indexOf(selected);
						adapter.getItems().remove(indexToRemove);
						adapter.notifyItemRemoved(indexToRemove);
						javaDocInfos.remove(selected);
						updateMoveButtons(null);
					});
				} catch(IOException e) {
					showError(R.string.deleteJavadocError, e);
				}
			});
		}
	}

	@WorkerThread
	public static void deleteRecursive(File directory) throws IOException {
		try {
			deleteRecursive(directory.toPath());
		} catch(UncheckedIOException e) {
			IOException cause = e.getCause();
			throw cause == null ? new IOException(e) : cause;
		}
	}

	@WorkerThread
	public static void deleteRecursive(Path directory) {
		try {
			if(Files.isDirectory(directory)) {
				try(Stream<Path> list = Files.list(directory)) {
					list.forEach(ListJavadocsActivity::deleteRecursive);
				}
			}
			Files.delete(directory);
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	@UiThread
	private void downloadBtnClicked(View view) {
		PopupMenu menu = new PopupMenu(this, view);
		menu.setOnMenuItemClickListener(this::downloadMenuItemClicked);
		menu.inflate(R.menu.download_menu);
		menu.show();
	}

	@UiThread
	private boolean downloadMenuItemClicked(MenuItem menuItem) {
		int itemId = menuItem.getItemId();
		String url = null;
		if(itemId == R.id.downloadFromCentral) {
			showDownloadPopup("https://repo1.maven.org/maven2");
		} else if(itemId == R.id.downloadFromMaven) {
			showDownloadPopup("");
		} else if(itemId == R.id.downloadFromZip) {
			loadZipJavadoc();
		} else if(itemId == R.id.oracleDownloadSelector8) {
			url = "https://www.oracle.com/java/technologies/javase-jdk8-doc-downloads.html";
		} else if(itemId == R.id.oracleDownloadSelector11) {
			url = "https://www.oracle.com/java/technologies/javase-jdk11-doc-downloads.html";
		} else if(itemId == R.id.oracleDownloadSelector17) {
			url = "https://www.oracle.com/java/technologies/javase-jdk17-doc-downloads.html";
		} else if(itemId == R.id.oracleDownloadSelector21) {
			url = "https://www.oracle.com/java/technologies/javase-jdk21-doc-downloads.html";
		} else if(itemId == R.id.oracleDownloadSelectorCustom) {
			url = "https://www.oracle.com/java/technologies/javase-downloads.html";
		} else {
			return false;
		}
		if(url != null) {
			OracleDownloaderActivity.open(this, url, javaDocInfos.size());
		}
		return true;
	}

	@UiThread
	private void loadZipJavadoc() {
		zipLauncher.launch(null);
	}

	@UiThread
	private void showDownloadPopup(String repo) {
		View layout = getLayoutInflater().inflate(R.layout.popup_artifact_selector, findViewById(android.R.id.content), false);
		PopupWindow popUp = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
		popUp.setContentView(layout);
		EditText repoSelector = layout.findViewById(R.id.artifactSelectorRepoSelector);
		repoSelector.setText(repo);

		layout.findViewById(R.id.artifactSelectorDownloadBtn).setOnClickListener(v -> {
					progressBar.setVisibility(View.VISIBLE);
					JavaDocDownloader.downloadFromMavenRepo(this,
									repoSelector.getText().toString(),
									layout.<EditText>findViewById(R.id.artifactSelectorGroupSelector).getText().toString(),
									layout.<EditText>findViewById(R.id.artifactSelectorArtifactSelector).getText().toString(),
									layout.<EditText>findViewById(R.id.artifactSelectorVersionSelector).getText().toString(), javaDocInfos.size(),
									progressBar::setProgress
							).thenAccept(info -> {
								ListClassesActivity.open(this, info);
								runInUIThread(popUp::dismiss);
							}).exceptionally(e -> showError(R.string.javadocDownloadError, e))
							.handle(this::removeProgressBar);
				}
		);
		layout.findViewById(R.id.artifactSelectorDismissBtn).setOnClickListener(v -> popUp.dismiss());
		popUp.showAtLocation(layout, Gravity.CENTER, 0, 0);
	}

	@Override
	protected ListJavaDocsViewAdapter createAdapter() {
		javaDocInfos = new ArrayList<>();
		return new ListJavaDocsViewAdapter(javaDocInfos, this::onShow);
	}

	@UiThread
	private void onShow(JavaDocInformation javaDocInformation) {
		ListClassesActivity.open(this, javaDocInformation);
	}

	@Override
	public void onSearch(String search) {
		if(javaDocInfos != null) {
			List<JavaDocInformation> items = new ArrayList<>(javaDocInfos);
			items.removeIf(item -> !item.getName().toLowerCase().contains(search.toLowerCase()));
			adapter.setItems(new ArrayList<>(items));
		}
	}

	@Override
	public void onSearchType(String search) {
		onSearch(search);
	}

	@Override
	public boolean supportsSearch() {
		return true;
	}

	private Object removeProgressBar(Void a, Throwable b) {
		progressBar.setVisibility(View.GONE);
		return null;
	}
}
