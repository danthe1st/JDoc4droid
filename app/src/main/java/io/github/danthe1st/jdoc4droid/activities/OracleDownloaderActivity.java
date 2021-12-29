package io.github.danthe1st.jdoc4droid.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebResponse;

import java.util.concurrent.CompletableFuture;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.classes.ListClassesActivity;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;


public class OracleDownloaderActivity extends AbstractActivity {

    private static final String NUM_JAVADOCS_ARG_NAME = "numberOfJavadocs";
    private int numberOfJavadocs;
    private GeckoRuntime runtime;
    private GeckoView webView;
    private GeckoSession session;
    private boolean canGoBack = false;

    public static void open(Context ctx, int numberOfJavadocs) {
        Intent intent = new Intent(ctx, OracleDownloaderActivity.class);
        intent.putExtra(NUM_JAVADOCS_ARG_NAME, numberOfJavadocs);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloader);
        numberOfJavadocs = getIntent().getIntExtra(NUM_JAVADOCS_ARG_NAME, 0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        webView = findViewById(R.id.downloaderView);
        ProgressBar loadingView = findViewById(R.id.loadingText);

        CookieManager.getInstance().setAcceptCookie(true);

        session = new GeckoSession();


        runtime = GeckoRuntime.create(this);

        session.open(runtime);
        webView.setSession(session);

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

        session.loadUri("https://www.oracle.com/java/technologies/javase-downloads.html");
        session.setContentDelegate(new GeckoSession.ContentDelegate() {

            @Override
            public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {
                String fileUri = response.uri;
                if (fileUri.contains("?")) {
                    fileUri = fileUri.substring(0, fileUri.indexOf('?'));
                }
                if (fileUri.endsWith(".zip")) {
                    CompletableFuture<JavaDocInformation> future = JavaDocDownloader.downloadJavaApiDocs(OracleDownloaderActivity.this, response.uri, response.body, numberOfJavadocs);
                    if (future != null) {
                        loadingView.setVisibility(View.VISIBLE);
                        webView.setVisibility(View.INVISIBLE);
                        future
                                .thenAccept(dir ->
                                        runInUIThread(() -> ListClassesActivity.open(OracleDownloaderActivity.this, dir))
                                )
                                .exceptionally(e -> {
                                    showError(R.string.javadocDownloadError, e);
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
        runtime.shutdown();
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