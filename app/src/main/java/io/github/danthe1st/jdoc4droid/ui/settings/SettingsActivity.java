package io.github.danthe1st.jdoc4droid.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import io.github.danthe1st.jdoc4droid.R;

public class SettingsActivity extends AppCompatActivity {

	@UiThread
	public static void open(Context ctx) {
		Intent intent = new Intent(ctx, SettingsActivity.class);
		ctx.startActivity(intent);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);
		if(savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

}