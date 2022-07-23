package io.github.danthe1st.jdoc4droid.ui.settings;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;

import io.github.danthe1st.jdoc4droid.R;

public class SettingsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey);
		findPreference("aboutLicensesPreference").setOnPreferenceClickListener(pref -> {
			startActivity(new Intent(getActivity(), OssLicensesMenuActivity.class));
			return true;
		});
		Drawable prefIcon = MaterialDrawableBuilder
				.with(getContext())
				.setColorResource(R.color.contrastColor)
				.setIcon(MaterialDrawableBuilder.IconValue.SETTINGS).build();
		findPreference("openJavadocsTypesPreference").setIcon(prefIcon);
	}

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
}
