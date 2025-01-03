package io.github.danthe1st.jdoc4droid.ui.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;


public abstract class MaterialPreference extends Preference {
	public MaterialPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(attrs);
	}

	public MaterialPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	public MaterialPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	protected void init(AttributeSet attrs) {
		if(attrs != null) {
			for(int i = 0; i < attrs.getAttributeCount(); i++) {
				initAttribute(attrs.getAttributeName(i), attrs, i);
			}
		}
	}

	protected abstract void initAttribute(String name, AttributeSet attrs, int i);
}
