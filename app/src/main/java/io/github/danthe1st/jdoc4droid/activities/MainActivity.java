package io.github.danthe1st.jdoc4droid.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.javadocs.ListJavadocsFragment;

public class MainActivity extends FragmentHolderActivity {//https://github.com/chhorz/javadoc-parser

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AbstractFragment.openFragment(getSupportFragmentManager(), ListJavadocsFragment.newInstance(), this);
        //JavaDocDownloader.downloadJavaApiDocs(this, this, 15, dir -> {
        //    AbstractFragment.openFragment(getSupportFragmentManager(), ListClassesFragment.newInstance(dir),this);
        //});
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
