package io.github.danthe1st.jdoc4droid.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.ui.list.classes.ListClassesActivity;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;


public class OracleDownloaderActivity extends AbstractActivity {

	private static final String NUM_JAVADOCS_ARG_NAME = "numberOfJavadocs";
	private static final String URL_ARG_NAME = "url";
	private int numberOfJavadocs;
	private WebView webView;
	private TextView foreignContentInfoView;

	@UiThread
	public static void open(Context ctx, String url, int numberOfJavadocs) {
		Intent intent = new Intent(ctx, OracleDownloaderActivity.class);
		intent.putExtra(NUM_JAVADOCS_ARG_NAME, numberOfJavadocs);
		intent.putExtra(URL_ARG_NAME, url);
		ctx.startActivity(intent);
	}

	@SuppressLint("SetJavaScriptEnabled")
    @Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_downloader);
		numberOfJavadocs = getIntent().getIntExtra(NUM_JAVADOCS_ARG_NAME, 0);
		String startURL = Objects.requireNonNull(getIntent().getStringExtra(URL_ARG_NAME));
		webView = findViewById(R.id.downloaderView);
		foreignContentInfoView = findViewById(R.id.foreignContentInfo);
		ProgressBar loadingView = findViewById(R.id.downloadProgressBar);
		ProgressBar progressVisibleBar = findViewById(R.id.downloadProgressVisibleBar);

		CookieManager.getInstance().setAcceptCookie(true);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDomStorageEnabled(true);
		webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

		webView.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				String host = request.getUrl().getHost();
				Log.i(getClass().getCanonicalName(), "shouldOverrideUrlLoading: "+host);
                return host == null || !host.endsWith("oracle.com");
            }

		});
		webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
			String path = url;
			if(url.contains("?")){
				path = url.substring(0,url.indexOf("?"));
			}
			if(path.endsWith(".zip")) {

				CompletableFuture<JavaDocInformation> future = JavaDocDownloader.downloadOracleJavadoc(OracleDownloaderActivity.this, url, contentLength, numberOfJavadocs, loadingView::setProgress);
				if(future != null) {
					runInUIThread(() -> {
						loadingView.setVisibility(View.VISIBLE);
						progressVisibleBar.setVisibility(View.VISIBLE);
						webView.setVisibility(View.INVISIBLE);
					});
					foreignContentInfoView.setVisibility(View.INVISIBLE);
					future
							.thenAccept(dir ->
									runInUIThread(() -> ListClassesActivity.open(OracleDownloaderActivity.this, dir))
							)
							.exceptionally(e -> {
								showError(R.string.javadocDownloadError, e);
								return null;
							})
							.handle((a, b) -> {
								loadingView.setVisibility(View.GONE);
								progressVisibleBar.setVisibility(View.GONE);
								return null;
							});
				}
			}
		});

		webView.loadUrl(startURL);
		Toast.makeText(this, R.string.downloadOracleDocPrompt, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if(webView.canGoBack()) {
			webView.goBack();
		} else {
			super.onBackPressed();
		}
	}
}
