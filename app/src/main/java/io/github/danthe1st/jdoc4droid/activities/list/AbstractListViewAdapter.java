package io.github.danthe1st.jdoc4droid.activities.list;

import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;

public abstract class AbstractListViewAdapter<T, H extends AbstractListViewHolder<T, H>> extends RecyclerView.Adapter<H> {

    protected List<T> items;

    protected H selectedViewHolder;

    protected Consumer<T> onShow;

    private Consumer<T> onSelect;

    protected long lastClickTime = 0;

    public AbstractListViewAdapter(List<T> items, Consumer<T> onShow) {
        this.items = items;
        this.onShow = onShow;
    }

    @Override
    @UiThread
    public void onBindViewHolder(H holder, int position) {
        holder.item = items.get(position);
        if (selectedViewHolder == holder) {
            unselect();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @UiThread
    public void setItems(List<T> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @UiThread
    void unselect() {
        setCardColor(selectedViewHolder.view, R.color.background);
        selectedViewHolder = null;
        if (onSelect != null) {
            onSelect.accept(null);
        }
    }

    @UiThread
    void setCardColor(View view, @ColorRes int color) {
        View card = view.findViewById(R.id.card);
        if (card == null) {
            card = view;
        }
        card.setBackgroundColor(view.getResources().getColor(color, null));
    }

    public List<T> getItems() {
        return this.items;
    }

    Consumer<T> getOnSelect() {
        return this.onSelect;
    }

    public void setOnShow(Consumer<T> onShow) {
        this.onShow = onShow;
    }

    protected void setOnSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect;
    }
}
