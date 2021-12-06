package io.github.danthe1st.jdoc4droid.activities.list;

import android.view.View;

import androidx.annotation.ColorRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public abstract class AbstractListViewAdapter<T,H extends AbstractListViewHolder<T,H>> extends RecyclerView.Adapter<H>{

    @NonNull
    @Getter
    protected List<T> items;

    protected H selectedViewHolder;

    @NonNull
    @Setter
    protected Consumer<T> onShow;

    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PACKAGE)
    private Consumer<T> onSelect;

    protected long lastClickTime=0;

    @Override
    public void onBindViewHolder(H holder, int position) {
        holder.item = items.get(position);
        if(selectedViewHolder==holder){
            unselect();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<T> items) {
        this.items = items;
        notifyDataSetChanged();
    }
    void unselect(){
        setCardColor(selectedViewHolder.view,R.color.background);
        selectedViewHolder=null;
    }
    void setCardColor(View view,@ColorRes int color){
        View card=view.findViewById(R.id.card);
        if(card==null){
            card=view;
        }
        card.setBackgroundColor(view.getResources().getColor(color,null));
    }
}
