package io.github.danthe1st.jdoc4droid.activities.list;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public abstract class AbstractListViewAdapter<T,H extends AbstractListViewAdapter<T,H>.AbstractViewHolder> extends RecyclerView.Adapter<H>{

    @NonNull
    @Getter
    protected List<T> items;

    protected H selectedViewHolder;

    @NonNull
    @Setter
    protected Consumer<T> onShow;

    @Setter(AccessLevel.PROTECTED)
    private Consumer<T> onSelect;

    protected long lastClickTime=0;

    @Override
    public void onBindViewHolder(H holder, int position) {
        holder.item= items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<T> items) {
        this.items =items;
        notifyDataSetChanged();
    }

    public abstract class AbstractViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public T item;

        public AbstractViewHolder(View binding) {
            super(binding);
            this.view=binding;
            view.setOnClickListener(this::onClick);
        }

        protected void onClick(View view) {
            if (selectedViewHolder == this) {
                if(lastClickTime>System.nanoTime()-500_000_000){//2 clicks per second-->double-click
                    onShow.accept(item);
                }
            } else {
                if (selectedViewHolder != null) {
                    setCardColor(selectedViewHolder.view,R.color.background);
                }
                selectedViewHolder = (H) this;
                setCardColor(view,R.color.secondary);
                if(onSelect!=null){
                    onSelect.accept(item);
                }
            }
            lastClickTime=System.nanoTime();//monochromic, cannot be manipulated using system time
        }
        private void setCardColor(View view,@ColorRes int color){
            View card=view.findViewById(R.id.card);
            if(card==null){
                card=view;
            }
            card.setBackgroundColor(view.getResources().getColor(color,null));
        }
    }
}
