package io.github.danthe1st.jdoc4droid.activities;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.list.classes.ListClassesFragment;
import io.github.danthe1st.jdoc4droid.util.JavaDocDownloader;


public class DownloaderFragment extends AbstractFragment {

    private WebView webView;

    public DownloaderFragment() {
        // Required empty public constructor
    }


    public static DownloaderFragment newInstance() {
        DownloaderFragment fragment = new DownloaderFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_downloader, container, false);

        webView=view.findViewById(R.id.downloaderView);
        ProgressBar loadingView=view.findViewById(R.id.loadingText);

        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if(host!=null&&host.endsWith("oracle.com")){
                    return super.shouldOverrideUrlLoading(view,request);
                }
                return false;
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if(JavaDocDownloader.downloadJavaApiDocs(getContext(),url,dir -> runInUIThread(()->{
                FragmentHolderActivity act=getBelongingActivity();
                super.goBack();
                openFragment(act.getSupportFragmentManager(), ListClassesFragment.newInstance(dir),act);
            }))){
                loadingView.setVisibility(View.VISIBLE);
                webView.setVisibility(View.INVISIBLE);
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.loadUrl("https://www.oracle.com/java/technologies/javase-downloads.html");



        return view;
    }

    @Override
    public void goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.goBack();
        }
    }
}