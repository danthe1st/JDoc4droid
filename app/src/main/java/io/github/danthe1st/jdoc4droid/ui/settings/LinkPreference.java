package io.github.danthe1st.jdoc4droid.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

public class LinkPreference extends MaterialPreference {
	private String link;

	public LinkPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public LinkPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public LinkPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void initAttribute(String name, AttributeSet attrs, int i) {
		if("link".equals(name)) {
			link = attrs.getAttributeValue(i);
		}
	}

	@Override
	protected void init(AttributeSet attrs) {
		super.init(attrs);
		if(link == null) {
			throw new IllegalArgumentException("link missing");
		}
		setOnPreferenceClickListener(this::onClick);
	}

	private boolean onClick(Preference preference) {
		Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
		Intent shareIntent = Intent.createChooser(sendIntent, null);
		super.getContext().startActivity(shareIntent);
		return true;
	}
}
