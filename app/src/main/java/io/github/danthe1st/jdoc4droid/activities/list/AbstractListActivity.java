package io.github.danthe1st.jdoc4droid.activities.list;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.AbstractActivity;

public abstract class AbstractListActivity<T,A extends AbstractListViewAdapter<T,?>> extends AbstractActivity {
    protected A adapter;

    @Override
    protected void onStart() {
        super.onStart();
        RecyclerView recyclerView = getRecyclerView();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = createAdapter();
        recyclerView.setAdapter(adapter);
    }


    protected abstract A createAdapter();

    @LayoutRes
    protected abstract int getLayoutId();

    protected RecyclerView getRecyclerView() {
        return findViewById(R.id.list);
    }
}
