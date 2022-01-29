package io.github.danthe1st.jdoc4droid.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebResponse;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.classes.ListClassesActivity;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;


public class OracleDownloaderActivity extends AbstractActivity {

    private static final String NUM_JAVADOCS_ARG_NAME = "numberOfJavadocs";
    private static final String URL_ARG_NAME = "url";
    private int numberOfJavadocs;
    private GeckoView geckoView;
    private GeckoSession session;
    private boolean canGoBack = false;

    @UiThread
    public static void open(Context ctx, String url, int numberOfJavadocs) {
        Intent intent = new Intent(ctx, OracleDownloaderActivity.class);
        intent.putExtra(NUM_JAVADOCS_ARG_NAME, numberOfJavadocs);
        intent.putExtra(URL_ARG_NAME, url);
        ctx.startActivity(intent);
    }

    private void loadSession() {
        GeckoRuntime runtime = GeckoRuntime.getDefault(this);
        session = geckoView.getSession();
        if (session == null) {
            session = new GeckoSession();
            geckoView.setSession(session);
            session.open(runtime);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);
        numberOfJavadocs = getIntent().getIntExtra(NUM_JAVADOCS_ARG_NAME, 0);
        String startURL = getIntent().getStringExtra(URL_ARG_NAME);
        geckoView = findViewById(R.id.downloaderView);
        ProgressBar loadingView = findViewById(R.id.downloadProgressBar);
        ProgressBar progressVisibleBar = findViewById(R.id.downloadProgressVisibleBar);

        loadSession();

        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Nullable
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
                if (!request.uri.contains("oracle.com")) {
                    GeckoResult<AllowOrDeny> res = new GeckoResult<>();
                    res.complete(AllowOrDeny.DENY);
                    return res;
                }
                return null;
            }

            @Nullable
            @Override
            public GeckoResult<GeckoSession> onNewSession(@NonNull GeckoSession session, @NonNull String uri) {
                session.loadUri(uri);
                return null;
            }

            @Override
            public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBack) {
                OracleDownloaderActivity.this.canGoBack = canGoBack;
            }
        });

        session.loadUri(startURL);
        session.setContentDelegate(new GeckoSession.ContentDelegate() {

            @Override
            public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {
                String fileUri = response.uri;
                if (fileUri.contains("?")) {
                    fileUri = fileUri.substring(0, fileUri.indexOf('?'));
                }
                if (fileUri.endsWith(".zip")) {
                    CompletableFuture<JavaDocInformation> future = JavaDocDownloader.downloadOracleJavadoc(OracleDownloaderActivity.this, response.uri, response.body, Long.parseLong(Objects.requireNonNull(response.headers.getOrDefault("Content-Length", "-1"))), numberOfJavadocs, loadingView::setProgress);
                    if (future != null) {
                        loadingView.setVisibility(View.VISIBLE);
                        progressVisibleBar.setVisibility(View.VISIBLE);
                        geckoView.setVisibility(View.INVISIBLE);
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
            }

        });
        Toast.makeText(this, R.string.downloadOracleDocPrompt, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }
}