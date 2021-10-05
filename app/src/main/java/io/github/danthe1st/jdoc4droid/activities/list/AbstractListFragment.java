package io.github.danthe1st.jdoc4droid.activities.list;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.AbstractFragment;
import io.github.danthe1st.jdoc4droid.activities.list.classes.ListClassesViewAdapter;
import io.github.danthe1st.jdoc4droid.util.JavaDocParser;

public abstract class AbstractListFragment<A extends AbstractListViewAdapter> extends AbstractFragment {
    protected A adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);

        Context context = view.getContext();
        RecyclerView recyclerView = getRecyclerView(view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        adapter = createAdapter(context);
        recyclerView.setAdapter(adapter);
        return view;
    }

    protected abstract A createAdapter(Context ctx);

    @LayoutRes
    protected abstract int getLayoutId();

    protected RecyclerView getRecyclerView(View root) {
        return root.findViewById(R.id.list);
    }
}
