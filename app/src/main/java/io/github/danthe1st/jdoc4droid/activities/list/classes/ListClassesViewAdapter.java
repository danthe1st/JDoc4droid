package io.github.danthe1st.jdoc4droid.activities.list.classes;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListViewAdapter;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;

import java.util.List;
import java.util.function.Consumer;

public class ListClassesViewAdapter extends AbstractListViewAdapter<SimpleClassDescription, ListClassesViewAdapter.ViewHolder> {

    public ListClassesViewAdapter(List<SimpleClassDescription> classes, Consumer<SimpleClassDescription> onShow) {
        super(classes, onShow);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_list_classes, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        super.onBindViewHolder(holder,position);
        holder.classNameView.setText(Html.fromHtml(holder.item.getPackageName()+".<b>"+holder.item.getName()+"</b>",Html.FROM_HTML_MODE_LEGACY));
        holder.classDescriptionView.setText(holder.item.getDescription());
    }


    public class ViewHolder extends AbstractListViewAdapter<SimpleClassDescription,ViewHolder>.AbstractViewHolder {
        public final TextView classNameView;
        public final TextView classDescriptionView;

        public ViewHolder(View binding) {
            super(binding);
            view.setOnClickListener(this::onClick);
            classNameView=binding.findViewById(R.id.listClassesClassName);
            this.classDescriptionView = binding.findViewById(R.id.listClassesClassDesc);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + item.getName() + "'";
        }
    }
}