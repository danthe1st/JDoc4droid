package io.github.danthe1st.jdoc4droid.activities.list.javadocs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.OracleDownloaderActivity;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListActivity;
import io.github.danthe1st.jdoc4droid.activities.list.classes.ListClassesActivity;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.model.JavaDocType;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ListJavadocsActivity extends AbstractListActivity<JavaDocInformation, ListJavaDocsViewAdapter> {

    private List<JavaDocInformation> javaDocInfos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_list_javadocs_list);
        super.onCreate(savedInstanceState);
        findViewById(R.id.downloadBtn).setOnClickListener(this::downloadBtnClicked);
        adapter.setOnSelect(javaDocInformation -> {
            findViewById(R.id.deleteBtn).setVisibility(javaDocInformation == null ? View.INVISIBLE : View.VISIBLE);
            findViewById(R.id.updateBtn).setVisibility(javaDocInformation == null || javaDocInformation.getBaseDownloadUrl().isEmpty() ? View.INVISIBLE : View.VISIBLE);
            updateMoveButtons(javaDocInformation);
        });
        findViewById(R.id.deleteBtn).setOnClickListener(this::deleteSelectedJavadoc);
        findViewById(R.id.updateBtn).setOnClickListener(this::updateSelectedJavadoc);
        findViewById(R.id.moveUpBtn).setOnClickListener(this::moveJavadocUp);
        findViewById(R.id.moveDownBtn).setOnClickListener(this::moveJavadocDown);
        getThreadPool().execute(()->JavaDocDownloader.clearCacheIfNoDownloadInProgress(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        getThreadPool().execute(() -> {
            try {
                javaDocInfos = JavaDocDownloader.getAllSavedJavaDocInfos(this);
                runInUIThread(() -> adapter.setItems(javaDocInfos));
            } catch (IOException e) {
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
        if (!canMoveJavadoc(info, indexChange)) {
            Toast.makeText(this, R.string.moveJavadocError, Toast.LENGTH_LONG).show();
            return;
        }
        int index = adapter.getItems().indexOf(info);
        int newIndex = index + indexChange;
        adapter.getItems().add(newIndex, adapter.getItems().remove(index));
        adapter.notifyItemMoved(index, newIndex);
        for (int i = Math.min(index, newIndex); i <= Math.max(index, newIndex); i++) {
            JavaDocInformation effectedJavadoc = javaDocInfos.get(i);
            effectedJavadoc.setOrder(i);
            JavaDocDownloader.saveMetadata(effectedJavadoc)
                    .exceptionally(e -> showError(R.string.saveMetadataError, e));
        }
        updateMoveButtons(info);
    }

    private boolean canMoveJavadoc(JavaDocInformation info, int indexChange) {
        if (info == null) {
            return false;
        }
        int index = adapter.getItems().indexOf(info);
        int newIndex = index + indexChange;
        return newIndex >= 0 && newIndex < adapter.getItems().size();
    }

    private void updateSelectedJavadoc(View view) {
        JavaDocInformation selected = adapter.getSelectedElement();
        if (selected != null && selected.getType() == JavaDocType.MAVEN) {
            JavaDocDownloader.updateMavenJavadoc(selected)
                    .thenAccept(docInfo -> runInUIThread(() -> {
                        if (docInfo == null) {
                            Toast.makeText(this, R.string.javadocUpdateErrorAlreadyLatestVersion, Toast.LENGTH_LONG).show();
                        } else {
                            ListClassesActivity.open(this, docInfo);
                        }
                    }))
                    .exceptionally(e -> showError(R.string.javadocUpdateError, e));
        }
    }

    @UiThread
    private void deleteSelectedJavadoc(View view) {
        JavaDocInformation selected = adapter.getSelectedElement();
        if (selected != null) {
            getThreadPool().execute(() -> {
                try {
                    deleteRecursive(selected.getDirectory());
                    runInUIThread(() -> {
                        int indexToRemove = adapter.getItems().indexOf(selected);
                        adapter.getItems().remove(indexToRemove);
                        adapter.notifyItemRemoved(indexToRemove);
                        javaDocInfos.remove(selected);
                    });
                } catch (IOException e) {
                    showError(R.string.deleteJavadocError,e);
                }
            });
        }
    }

    @WorkerThread
    public static void deleteRecursive(File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteRecursive(file);
            }
        }
        Files.delete(directory.toPath());
    }

    @UiThread
    private void downloadBtnClicked(View view) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.setOnMenuItemClickListener(this::downloadMenuItemClicked);
        menu.inflate(R.menu.download_menu);
        menu.show();
    }

    @UiThread
    private boolean downloadMenuItemClicked(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.downloadFromCentral) {
            showDownloadPopup("https://repo1.maven.org/maven2");
        } else if (itemId == R.id.downloadFromMaven) {
            showDownloadPopup("");
        } else if (itemId == R.id.downloadFromOracle) {
            OracleDownloaderActivity.open(this, javaDocInfos.size());
        } else if (itemId == R.id.downloadFromZip) {
            loadZipJavadoc();
        } else {
            return false;
        }
        return true;
    }

    @UiThread
    private void loadZipJavadoc() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/java-archive"});
        startActivityForResult(intent, 1337);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1337 && data != null) {
            JavaDocDownloader.downloadFromUri(this, data.getData(), javaDocInfos.size())
                    .thenAccept(docInfo -> ListClassesActivity.open(this, docInfo))
                    .exceptionally(e -> showError(R.string.importJavadocError, e));
        }
    }

    @UiThread
    private void showDownloadPopup(String repo) {
        View layout = getLayoutInflater().inflate(R.layout.popup_artifact_selector, null, false);
        PopupWindow popUp = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popUp.setContentView(layout);
        EditText repoSelector = layout.findViewById(R.id.artifactSelectorRepoSelector);
        repoSelector.setText(repo);
        layout.findViewById(R.id.artifactSelectorDownloadBtn).setOnClickListener(v ->
                JavaDocDownloader.downloadFromMavenRepo(this,
                        repoSelector.getText().toString(),
                        layout.<EditText>findViewById(R.id.artifactSelectorGroupSelector).getText().toString(),
                        layout.<EditText>findViewById(R.id.artifactSelectorArtifactSelector).getText().toString(),
                        layout.<EditText>findViewById(R.id.artifactSelectorVersionSelector).getText().toString(), javaDocInfos.size()
                ).thenAccept(info -> {
                    ListClassesActivity.open(this, info);
                    runInUIThread(popUp::dismiss);
                })
                        .exceptionally(e -> showError(R.string.javadocDownloadError, e))
        );
        layout.findViewById(R.id.artifactSelectorDismissBtn).setOnClickListener(v -> popUp.dismiss());
        popUp.showAtLocation(layout, Gravity.CENTER, 0, 0);
    }

    @Override
    protected ListJavaDocsViewAdapter createAdapter() {
        javaDocInfos = new ArrayList<>();
        ListJavaDocsViewAdapter listJavaDocsViewAdapter = new ListJavaDocsViewAdapter(javaDocInfos, this::onShow);
        return listJavaDocsViewAdapter;
    }

    @UiThread
    private void onShow(JavaDocInformation javaDocInformation) {
        ListClassesActivity.open(this, javaDocInformation);
    }

    @Override
    public void onSearch(String search) {
        if (javaDocInfos != null) {
            List<JavaDocInformation> items = new ArrayList<>(javaDocInfos);
            items.removeIf(item -> !item.getName().toLowerCase().contains(search.toLowerCase()));
            adapter.setItems(items);
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


}