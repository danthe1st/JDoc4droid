package io.github.danthe1st.jdoc4droid.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import io.github.danthe1st.jdoc4droid.R;

public class SettingsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.root_preferences, rootKey);
		findPreference("aboutLicensesPreference").setOnPreferenceClickListener(pref -> {
			startActivity(new Intent(getActivity(), OssLicensesMenuActivity.class));
			return true;
		});
		findPreference("aboutGitHubPreference").setOnPreferenceClickListener(pref -> {
			Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/danthe1st/JDoc4droid"));
			Intent shareIntent = Intent.createChooser(sendIntent, null);
			startActivity(shareIntent);
			return true;
		});
	}

}
