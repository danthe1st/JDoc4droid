package io.github.danthe1st.jdoc4droid.activities.list.javadocs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListViewAdapter;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;

public class ListJavaDocsViewAdapter extends AbstractListViewAdapter<JavaDocInformation, ListJavaDocsViewAdapter.ViewHolder> {

    public ListJavaDocsViewAdapter(List<JavaDocInformation> items, Consumer<JavaDocInformation> onShow) {
        super(items, onShow);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_list_javadocs, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.nameView.setText(items.get(position).getName());
        holder.sourceView.setText(items.get(position).getSource());

    }

    @Override//the purpose of this is to allow package-private access
    protected void setOnSelect(Consumer<JavaDocInformation> onSelect) {
        super.setOnSelect(onSelect);
    }

    public JavaDocInformation getSelectedElement(){
        return selectedViewHolder==null?null:selectedViewHolder.item;
    }

    public class ViewHolder extends AbstractListViewAdapter<JavaDocInformation, ListJavaDocsViewAdapter.ViewHolder>.AbstractViewHolder {
        private final TextView nameView;
        private final TextView sourceView;

        public ViewHolder(View view) {
            super(view);
            nameView = view.findViewById(R.id.javaDocName);
            sourceView = view.findViewById(R.id.javaDocSource);
        }
    }
}