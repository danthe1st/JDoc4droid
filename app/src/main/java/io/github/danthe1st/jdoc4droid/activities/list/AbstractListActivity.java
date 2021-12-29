package io.github.danthe1st.jdoc4droid.activities.list;

import androidx.annotation.UiThread;
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

    @UiThread
    protected abstract A createAdapter();

    protected RecyclerView getRecyclerView() {
        return findViewById(R.id.list);
    }
}
