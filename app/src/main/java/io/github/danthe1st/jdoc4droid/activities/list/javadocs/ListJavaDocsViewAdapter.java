package io.github.danthe1st.jdoc4droid.activities.list.javadocs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListViewAdapter;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;

public class ListJavaDocsViewAdapter extends AbstractListViewAdapter<JavaDocInformation, ListJavaDocsViewHolder> {

    public ListJavaDocsViewAdapter(List<JavaDocInformation> items, Consumer<JavaDocInformation> onShow) {
        super(items, onShow);
    }

    @NonNull
    @Override
    public ListJavaDocsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_javadocs, parent, false);
        return new ListJavaDocsViewHolder(this, view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ListJavaDocsViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        JavaDocInformation javaDocInformation = items.get(position);
        holder.getNameView().setText(javaDocInformation.getName());
        holder.getSourceView().setText(javaDocInformation.getOnlineDocUrl());
        holder.getTypeView().setText(javaDocInformation.getType().toString());
    }

    @Override
    protected void setOnSelect(Consumer<JavaDocInformation> onSelect) {
        //the purpose of this is to allow package-private access by overriding protected method
        super.setOnSelect(onSelect);
    }

    public JavaDocInformation getSelectedElement(){
        return selectedViewHolder==null?null:selectedViewHolder.item;
    }
}
