package io.github.danthe1st.jdoc4droid.activities.list.classes;

import android.content.Context;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListFragment;
import io.github.danthe1st.jdoc4droid.activities.show.showclass.ShowClassFragment;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import io.github.danthe1st.jdoc4droid.util.JavaDocParser;

/**
 * A fragment representing a list of Items.
 */
public class ListClassesFragment extends AbstractListFragment<ListClassesViewAdapter> {

    private static final String ARG_JAVADOC_DIR = "javaDocDir";

    private File javaDocDir;


    private List<SimpleClassDescription> descriptions= Collections.emptyList();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ListClassesFragment() {
    }

    public static ListClassesFragment newInstance(JavaDocInformation javaDocInfo) {
        ListClassesFragment fragment = new ListClassesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JAVADOC_DIR, javaDocInfo.getDirectory().getAbsolutePath());
        String shareUrl=javaDocInfo.getSource();
        if(shareUrl!=null&&!shareUrl.isEmpty()){
            args.putString(ARG_SHARE_URL, shareUrl);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            javaDocDir = new File(getArguments().getString(ARG_JAVADOC_DIR));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater,container,savedInstanceState);

        getThreadPool().execute(()-> {
            try {
                descriptions = JavaDocParser.loadClasses(javaDocDir);
                runInUIThread(()->{
                    adapter.setItems(descriptions);
                    view.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runInUIThread(()->{
                    Toast.makeText(getContext(),"Cannot load classes",Toast.LENGTH_LONG).show();
                    goBack();
                    Log.e(getClass().getName(),"Cannot load class list",e);
                });
            }
        });
        return view;
    }

    @Override
    protected ListClassesViewAdapter createAdapter(Context ctx) {
        return new ListClassesViewAdapter(new ArrayList<>(),this::showClass);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list_classes_list;
    }

    private void showClass(SimpleClassDescription simpleClassDescription) {
        openFragment(ShowClassFragment.newInstance(javaDocDir,new File(javaDocDir,simpleClassDescription.getPath()),getShareLink()));
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