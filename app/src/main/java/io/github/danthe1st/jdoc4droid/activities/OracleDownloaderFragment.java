package io.github.danthe1st.jdoc4droid.activities;

import android.content.Context;
import android.os.Bundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import io.github.danthe1st.jdoc4droid.activities.list.classes.ListClassesFragment;
import io.github.danthe1st.jdoc4droid.model.JavaDocInformation;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;


public class OracleDownloaderFragment extends AbstractFragment {

    private static final String NUM_JAVADOCS_ARG_NAME = "numberOfJavadocs";
    private int numberOfJavadocs;
    private GeckoRuntime runtime;
    private GeckoView webView;
    private GeckoSession session;
    private boolean canGoBack = false;
    private Context context;
    private boolean instanceValid = true;

    public OracleDownloaderFragment() {
        // Required empty public constructor
    }


    public static OracleDownloaderFragment newInstance(int numberOfJavadocs) {
        OracleDownloaderFragment fragment = new OracleDownloaderFragment();
        Bundle args = new Bundle();
        args.putInt(NUM_JAVADOCS_ARG_NAME, numberOfJavadocs);
        fragment.setArguments(args);
        // fragment.numberOfJavadocs=numberOfJavadocs;
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        instanceValid = false;
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            numberOfJavadocs = getArguments().getInt(NUM_JAVADOCS_ARG_NAME);//TODO test
        }
        View view = inflater.inflate(R.layout.fragment_downloader, container, false);

        webView = view.findViewById(R.id.downloaderView);
        ProgressBar loadingView = view.findViewById(R.id.loadingText);

        CookieManager.getInstance().setAcceptCookie(true);

        session = new GeckoSession();


        runtime = GeckoRuntime.create(requireContext());

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

                //if (request.uri.endsWith(".zip") &&
//
                //        JavaDocDownloader.downloadJavaApiDocs(context, request.uri, dir -> runInUIThread(() -> {
                //            FragmentHolderActivity act = getBelongingActivity();
                //            if (instanceValid) {
                //                OracleDownloaderFragment.super.goBack();
                //            }
                //            openFragment(act.getSupportFragmentManager(), ListClassesFragment.newInstance(dir), act);
                //        }), numberOfJavadocs)) {
                //    loadingView.setVisibility(View.VISIBLE);
                //    webView.setVisibility(View.INVISIBLE);
                //}
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
                OracleDownloaderFragment.this.canGoBack = canGoBack;
            }
        });

        session.loadUri("https://www.oracle.com/java/technologies/javase-downloads.html");
        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onKill(@NonNull GeckoSession session) {
                OracleDownloaderFragment.super.goBack();
            }

            @Override
            public void onCrash(@NonNull GeckoSession session) {
                onKill(session);
            }

            @Override
            public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {
                String fileUri=response.uri;
                if(fileUri.contains("?")){
                    fileUri=fileUri.substring(0,fileUri.indexOf('?'));
                }
                if (fileUri.endsWith(".zip")) {
                    CompletableFuture<JavaDocInformation> future = JavaDocDownloader.downloadJavaApiDocs(context, response.uri, response.body, numberOfJavadocs);
                    if(future!=null) {
                        loadingView.setVisibility(View.VISIBLE);
                        webView.setVisibility(View.INVISIBLE);
                        future.thenAccept(dir -> runInUIThread(() -> {
                            FragmentHolderActivity act = getBelongingActivity();
                            exitBrowserIfPossible();
                            openFragment(act.getSupportFragmentManager(), ListClassesFragment.newInstance(dir), act);
                        }))
                        .exceptionally(e->{
                            showError(R.string.javadocDownloadError,e);
                            exitBrowserIfPossible();
                            return null;
                        });
                    }
                }
            }

        });
        Toast.makeText(getContext(), R.string.downloadOracleDocPrompt, Toast.LENGTH_LONG).show();
        return view;
    }

    private void exitBrowserIfPossible(){
        if (instanceValid) {
            super.goBack();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        runtime.shutdown();
    }

    @Override
    public void goBack() {
        if (canGoBack) {
            session.goBack();
        } else {
            super.goBack();
        }
    }
}