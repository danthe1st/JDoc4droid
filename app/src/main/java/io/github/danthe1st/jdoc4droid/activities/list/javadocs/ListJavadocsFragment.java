package io.github.danthe1st.jdoc4droid.activities.list.javadocs;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.DownloaderFragment;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListFragment;
import io.github.danthe1st.jdoc4droid.activities.list.classes.ListClassesFragment;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ListJavadocsFragment extends AbstractListFragment<ListJavaDocsViewAdapter> {

    private List<JavaDocInformation> javaDocInfos;

    public static ListJavadocsFragment newInstance() {
        ListJavadocsFragment fragment = new ListJavadocsFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.findViewById(R.id.downloadBtn).setOnClickListener(this::downloadBtnClicked);
        adapter.setOnSelect(javaDocInformation -> view.findViewById(R.id.deleteBtn).setVisibility(javaDocInformation==null?View.INVISIBLE:View.VISIBLE));
        view.findViewById(R.id.deleteBtn).setOnClickListener(this::deleteSelectedJavadoc);
        return view;
    }

    private void deleteSelectedJavadoc(View view) {
        JavaDocInformation selected = adapter.getSelectedElement();
        if(selected!=null){
            try {
                deleteRecursive(selected.getDirectory());
                int indexToRemove = adapter.getItems().indexOf(selected);
                adapter.getItems().remove(indexToRemove);
                adapter.notifyItemRemoved(indexToRemove);
            } catch (IOException e) {
                Toast.makeText(getContext(),"Cannot delete javadoc",Toast.LENGTH_SHORT).show();
                Log.e(getClass().getName(),"Cannot delete javadoc",e);
            }
        }
    }

    private void deleteRecursive(File directory) throws IOException {
        if(directory.isDirectory()){
            for (File file : directory.listFiles()) {
                deleteRecursive(file);
            }
        }
        Files.delete(directory.toPath());
    }

    private void downloadBtnClicked(View view) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.setOnMenuItemClickListener(this::downloadMenuItemClicked);
        menu.inflate(R.menu.download_menu);
        menu.show();
    }

    private boolean downloadMenuItemClicked(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.downloadFromCentral:
                Log.i(getClass().getName(),"Central");
                showDownloadPopup("https://repo1.maven.org/maven2");
                break;
            case R.id.downloadFromMaven:
                Log.i(getClass().getName(),"Custom Maven repo");
                showDownloadPopup("");
                break;
            case R.id.downloadFromOracle:
                Log.i(getClass().getName(),"Oracle");
                openFragment(DownloaderFragment.newInstance());
                break;
            case R.id.downloadFromZip:
                Log.i(getClass().getName(),"zip");
                loadZipJavadoc();
                break;
            default:
                return false;

        }
        return true;
    }

    private void loadZipJavadoc(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip","application/java-archive"});
        startActivityForResult(intent, 1337);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1337:
                if(data!=null){
                    JavaDocDownloader.downloadFromUri(getContext(),data.getData(), dir->openFragment(ListClassesFragment.newInstance(dir)));//TODO test
                }
                break;
        }
    }

    private void showDownloadPopup(String repo){
        PopupWindow popUp=new PopupWindow(getView(), ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,true);
        View layout=getLayoutInflater().inflate(R.layout.popup_artifact_selector,null,false);
        popUp.setContentView(layout);
        EditText repoSelector = layout.findViewById(R.id.artifactSelectorRepoSelector);
        repoSelector.setText(repo);
        layout.findViewById(R.id.artifactSelectorDownloadBtn).setOnClickListener(v->{
            JavaDocDownloader.downloadFromMavenRepo(requireContext(),
                    repoSelector.getText().toString(),
                    layout.<EditText>findViewById(R.id.artifactSelectorGroupSelector).getText().toString(),
                    layout.<EditText>findViewById(R.id.artifactSelectorArtifactSelector).getText().toString(),
                    layout.<EditText>findViewById(R.id.artifactSelectorVersionSelector).getText().toString(),
                    dir->openFragment(ListClassesFragment.newInstance(dir))
            );
        });
        layout.findViewById(R.id.artifactSelectorDismissBtn).setOnClickListener(v->popUp.dismiss());
        popUp.showAtLocation(getView(), Gravity.CENTER,0,0);
    }

    @Override
    protected ListJavaDocsViewAdapter createAdapter(Context ctx) {
        javaDocInfos = JavaDocDownloader.getAllSavedJavaDocInfos(ctx);
        return new ListJavaDocsViewAdapter(javaDocInfos, this::onShow);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list_javadocs_list;
    }

    private void onShow(JavaDocInformation javaDocInformation) {
        openFragment(ListClassesFragment.newInstance(javaDocInformation.getDirectory()));
    }

    @Override
    public void onSearch(String search) {
        if(javaDocInfos!=null){
            List<JavaDocInformation> items=new ArrayList<>(javaDocInfos);
            items.removeIf(item->!item.getName().toLowerCase().contains(search.toLowerCase()));
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