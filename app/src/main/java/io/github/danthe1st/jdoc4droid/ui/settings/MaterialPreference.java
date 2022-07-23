package io.github.danthe1st.jdoc4droid.ui.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

import io.github.danthe1st.jdoc4droid.R;

public class MaterialPreference extends Preference {
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

	protected void initAttribute(String name, AttributeSet attrs, int i) {
		if("materialIcon".equals(name)) {
			int attrValue = attrs.getAttributeIntValue(i, -1);
			if(attrValue != -1) {
				Drawable icon = MaterialDrawableBuilder
						.with(getContext())
						.setColorResource(R.color.contrastColor)
						.setIcon(MaterialDrawableBuilder.IconValue.values()[attrValue]).build();
				setIcon(icon);
			}
		}
	}
}
