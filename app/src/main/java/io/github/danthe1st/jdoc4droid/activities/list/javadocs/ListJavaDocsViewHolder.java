package io.github.danthe1st.jdoc4droid.activities.list.javadocs;

import android.graphics.Rect;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.ScrollCaptureCallback;
import android.view.ScrollCaptureSession;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.function.Consumer;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.AbstractListViewHolder;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ListJavaDocsViewHolder extends AbstractListViewHolder<JavaDocInformation,ListJavaDocsViewHolder> {
    private final TextView nameView;
    private final TextView sourceView;
    private final TextView typeView;


    public ListJavaDocsViewHolder(ListJavaDocsViewAdapter adapter, View view) {

        super(adapter,view);
        nameView = view.findViewById(R.id.javaDocName);
        sourceView = view.findViewById(R.id.javaDocSource);
        typeView = view.findViewById(R.id.javadocTypeField);
    }
}
