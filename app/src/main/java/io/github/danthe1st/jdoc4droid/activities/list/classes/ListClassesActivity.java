package io.github.danthe1st.jdoc4droid.activities.list.classes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private MenuItem filterButton;

    private List<SimpleClassDescription> descriptions = Collections.emptyList();
    private Set<String> classTypes=Collections.emptySet();
    private String selectedType=null;
    private String currentSearch="";

    @UiThread
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
        setContentView(R.layout.activity_list_classes_list);
        super.onCreate(savedInstanceState);
        javaDocDir = new File(getIntent().getStringExtra(ARG_JAVADOC_DIR));
        getThreadPool().execute(()-> {
            try {
                descriptions = JavaDocParser.loadClasses(javaDocDir);
                classTypes = descriptions.stream().map(SimpleClassDescription::getClassType).collect(Collectors.toSet());
                runInUIThread(()->{
                    adapter.setItems(descriptions);
                    findViewById(R.id.progressBar2).setVisibility(View.GONE);
                    reloadFilters();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret=super.onCreateOptionsMenu(menu);
        filterButton=menu.findItem(R.id.app_bar_filter);
        filterButton.setVisible(true);
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final String title = String.valueOf(item.getTitle());
        if (getAvailableFilters().contains(title)){
            onFilterChange(title);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected ListClassesViewAdapter createAdapter() {
        return new ListClassesViewAdapter(new ArrayList<>(),this::showClass);
    }

    @UiThread
    private void showClass(SimpleClassDescription simpleClassDescription) {
        ShowClassActivity.open(this,javaDocDir,new File(javaDocDir,simpleClassDescription.getPath()),getShareLink());
    }

    @Override
    public void onSearch(String search) {
        currentSearch=search;
        filterElements();
    }

    public void onFilterChange(String newFilter){
        selectedType=newFilter;
        filterElements();
    }

    public Set<String> getAvailableFilters(){
        return Collections.unmodifiableSet(classTypes);
    }

    private void filterElements(){
        adapter.setItems(
                descriptions.stream()
                        .filter(desc->selectedType==null||selectedType.equals(desc.getClassType()))
                        .filter(desc->(desc.getPackageName()+"."+desc.getName()).toLowerCase().contains(currentSearch.toLowerCase()))
                        .collect(Collectors.toList())
        );
    }

    @UiThread
    private void reloadFilters(){
        for (String availableFilter : getAvailableFilters()) {
            MenuItem item = filterButton.getSubMenu().add(availableFilter);
            SpannableString title = new SpannableString(availableFilter);
            title.setSpan(ContextCompat.getColor(this, R.color.teal_200), 0, title.length(), 0);
            item.setTitle(title);

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