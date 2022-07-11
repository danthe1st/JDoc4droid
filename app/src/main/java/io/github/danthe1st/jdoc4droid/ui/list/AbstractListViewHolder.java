package io.github.danthe1st.jdoc4droid.ui.list;

import android.view.View;

import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

import io.github.danthe1st.jdoc4droid.R;

public abstract class AbstractListViewHolder<T, H extends AbstractListViewHolder<T, H>> extends RecyclerView.ViewHolder {
	private final AbstractListViewAdapter<T, H> abstractListViewAdapter;
	public final View view;
	public T item;

	protected AbstractListViewHolder(AbstractListViewAdapter<T, H> abstractListViewAdapter, View binding) {
		super(binding);
		this.abstractListViewAdapter = abstractListViewAdapter;
		this.view = binding;
		view.setOnClickListener(this::onClick);
	}

	@UiThread
	protected void onClick(View view) {
		if(abstractListViewAdapter.selectedViewHolder == this) {
			if(abstractListViewAdapter.lastClickTime > System.nanoTime() - 500_000_000) {//2 clicks per second-->double-click
				abstractListViewAdapter.onShow.accept(item);
			}
		} else {
			if(abstractListViewAdapter.selectedViewHolder != null) {
				abstractListViewAdapter.unselect();
			}
			abstractListViewAdapter.selectedViewHolder = (H) this;
			abstractListViewAdapter.setCardColor(view, R.color.secondary);
			if(abstractListViewAdapter.getOnSelect() != null) {
				abstractListViewAdapter.getOnSelect().accept(item);
			}
		}
		abstractListViewAdapter.lastClickTime = System.nanoTime();//monochromic, cannot be manipulated using system time
	}
}
