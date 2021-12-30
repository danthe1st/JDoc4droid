package io.github.danthe1st.jdoc4droid.activities.list.classes;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListViewHolder;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;

public class ListClassesViewHolder extends AbstractListViewHolder<SimpleClassDescription, ListClassesViewHolder> {
    public final TextView classNameView;
    public final TextView classDescriptionView;

    public ListClassesViewHolder(ListClassesViewAdapter listClassesViewAdapter, View binding) {
        super(listClassesViewAdapter, binding);
        view.setOnClickListener(this::onClick);
        classNameView = binding.findViewById(R.id.listClassesClassName);
        this.classDescriptionView = binding.findViewById(R.id.listClassesClassDesc);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " '" + item.getName() + "'";
    }
}
