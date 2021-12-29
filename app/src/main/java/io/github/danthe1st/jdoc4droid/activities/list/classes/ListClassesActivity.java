package io.github.danthe1st.jdoc4droid.activities.list.classes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListActivity;
import io.github.danthe1st.jdoc4droid.activities.show.showclass.ShowClassActivity;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import io.github.danthe1st.jdoc4droid.util.parsing.JavaDocParser;

/**
 * A fragment representing a list of Items.
 */
public class ListClassesActivity extends AbstractListActivity<SimpleClassDescription,ListClassesViewAdapter> {

    private static final String ARG_JAVADOC_DIR = "javaDocDir";

    private File javaDocDir;

    private List<SimpleClassDescription> descriptions= Collections.emptyList();

    public static void open(Context ctx, JavaDocInformation javaDocInfo){
        Intent intent=new Intent(ctx, ListClassesActivity.class);
        intent.putExtra(ARG_JAVADOC_DIR,javaDocInfo.getDirectory().getAbsolutePath());
        String shareUrl=javaDocInfo.getOnlineDocUrl();
        if(shareUrl!=null&&!shareUrl.isEmpty()){
            intent.putExtra(ARG_SHARE_URL, shareUrl);
        }
        ctx.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_classes_list);
        javaDocDir = new File(getIntent().getStringExtra(ARG_JAVADOC_DIR));
    }

    @Override
    protected void onStart() {
        super.onStart();
        getThreadPool().execute(()-> {
            try {
                descriptions = JavaDocParser.loadClasses(javaDocDir);
                runInUIThread(()->{
                    adapter.setItems(descriptions);
                    findViewById(R.id.progressBar2).setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runInUIThread(()->{
                    Toast.makeText(this,"Cannot load classes",Toast.LENGTH_LONG).show();
                    onBackPressed();
                    Log.e(getClass().getName(),"Cannot load class list",e);
                });
            }
        });
    }

    @Override
    protected ListClassesViewAdapter createAdapter() {
        return new ListClassesViewAdapter(new ArrayList<>(),this::showClass);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_list_classes_list;
    }

    private void showClass(SimpleClassDescription simpleClassDescription) {
        ShowClassActivity.open(getApplicationContext(),javaDocDir,new File(javaDocDir,simpleClassDescription.getPath()),getShareLink());
    }

    @Override
    public void onSearch(String search) {
        adapter.setItems(descriptions.stream().filter(desc->(desc.getPackageName()+"."+desc.getName()).toLowerCase().contains(search.toLowerCase())).collect(Collectors.toList()));
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