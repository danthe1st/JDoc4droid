package io.github.danthe1st.jdoc4droid.ui.list.javadocs;

import android.view.View;
import android.widget.TextView;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.ui.list.AbstractListViewHolder;

public class ListJavaDocsViewHolder extends AbstractListViewHolder<JavaDocInformation, ListJavaDocsViewHolder> {
	private final TextView nameView;
	private final TextView sourceView;
	private final TextView typeView;

	public ListJavaDocsViewHolder(ListJavaDocsViewAdapter adapter, View view) {
		super(adapter, view);
		nameView = view.findViewById(R.id.javaDocName);
		sourceView = view.findViewById(R.id.javaDocSource);
		typeView = view.findViewById(R.id.javadocTypeField);
	}

	public TextView getNameView() {
		return this.nameView;
	}

	public TextView getSourceView() {
		return this.sourceView;
	}

	public TextView getTypeView() {
		return this.typeView;
	}
}
