package io.github.danthe1st.jdoc4droid.ui.list;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.ui.AbstractActivity;

public abstract class AbstractListActivity<T, A extends AbstractListViewAdapter<T, ?>> extends AbstractActivity {
	protected A adapter;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		RecyclerView recyclerView = getRecyclerView();
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = createAdapter();
		recyclerView.setAdapter(adapter);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@UiThread
	protected abstract A createAdapter();

	protected RecyclerView getRecyclerView() {
		return findViewById(R.id.list);
	}
}
