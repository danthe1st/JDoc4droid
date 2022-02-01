package io.github.danthe1st.jdoc4droid.activities.list.classes;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListViewAdapter;
import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;

public class ListClassesViewAdapter extends AbstractListViewAdapter<SimpleClassDescription, ListClassesViewHolder> {

    public ListClassesViewAdapter(List<SimpleClassDescription> classes, Consumer<SimpleClassDescription> onShow) {
        super(classes, onShow);
    }

    @NonNull
    @Override
    public ListClassesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ListClassesViewHolder(this, LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_classes, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final ListClassesViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.classNameView.setText(Html.fromHtml(holder.item.getPackageName() + ".<b>" + holder.item.getName() + "</b>", Html.FROM_HTML_MODE_LEGACY));
        holder.classDescriptionView.setText(holder.item.getDescription());
    }
}