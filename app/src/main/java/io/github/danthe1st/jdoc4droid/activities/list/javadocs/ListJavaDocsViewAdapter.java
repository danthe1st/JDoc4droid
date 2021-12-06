package io.github.danthe1st.jdoc4droid.activities.list.javadocs;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;
import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListViewAdapter;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;

public class ListJavaDocsViewAdapter extends AbstractListViewAdapter<JavaDocInformation, ListJavaDocsViewHolder> {

    public ListJavaDocsViewAdapter(List<JavaDocInformation> items, Consumer<JavaDocInformation> onShow) {
        super(items, onShow);
    }

    @Override
    public ListJavaDocsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ListJavaDocsViewHolder(this,LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_list_javadocs, parent, false));
    }

    @Override
    public void onBindViewHolder(final ListJavaDocsViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        JavaDocInformation javaDocInformation = items.get(position);
        holder.getNameView().setText(javaDocInformation.getName());
        holder.getSourceView().setText(javaDocInformation.getOnlineDocUrl());
        holder.getTypeView().setText(javaDocInformation.getType().toString());
    }

    @Override//the purpose of this is to allow package-private access
    protected void setOnSelect(Consumer<JavaDocInformation> onSelect) {
        super.setOnSelect(onSelect);
    }

    public JavaDocInformation getSelectedElement(){
        return selectedViewHolder==null?null:selectedViewHolder.item;
    }

}