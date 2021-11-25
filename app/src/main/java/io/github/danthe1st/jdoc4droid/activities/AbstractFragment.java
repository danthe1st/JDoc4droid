package io.github.danthe1st.jdoc4droid.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import io.github.danthe1st.jdoc4droid.R;

public abstract class AbstractFragment extends Fragment {

    protected static final String ARG_SHARE_URL = "shareUrl";
    private String shareUrl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            shareUrl = getArguments().getString(ARG_SHARE_URL);
        }
        onFragmentLoad(getBelongingActivity());
    }

    public FragmentHolderActivity getBelongingActivity() {
        return (FragmentHolderActivity) super.getActivity();
    }

    protected AbstractFragment(){
        Log.i(AbstractFragment.class.getCanonicalName(), "AbstractFragment: create fragment "+this.getClass().getSimpleName()+" with ref hashCode "+System.identityHashCode(this));
    }

    public ExecutorService getThreadPool() {
        return getBelongingActivity().getThreadPool();
    }

    protected void openFragment(AbstractFragment fragment) {
        openFragment(getBelongingActivity().getSupportFragmentManager(), fragment, getBelongingActivity());
    }

    public void goBack() {
        FragmentHolderActivity activity = getBelongingActivity();
        AbstractFragment oldFragment = activity.getCurrentFragments().pop();
        if (activity.getCurrentFragments().isEmpty()) {
            activity.moveTaskToBack(true);
        } else {
            AbstractFragment fragment = activity.getCurrentFragments().peek();
            getFragmentManager().beginTransaction().remove(oldFragment).attach(fragment).replace(R.id.fragHolder,fragment,fragment.getTag()).commit();
            fragment.onFragmentLoad(activity);

        }
    }

    public static void openFragment(FragmentManager manager, AbstractFragment fragment, FragmentHolderActivity activity) {
        AbstractFragment oldFrag = activity.getCurrentFragments().peek();
        Log.i(AbstractFragment.class.getCanonicalName(), "fragments: "+manager.getFragments());
        FragmentTransaction transaction = manager.beginTransaction();
        if(oldFrag!=null){
            transaction.detach(oldFrag);
            Log.i(AbstractFragment.class.getCanonicalName(), "detach frag: "+oldFrag);
        }
        transaction.add(R.id.fragHolder, fragment, UUID.randomUUID().toString());
        transaction.commit();
        activity.getCurrentFragments().push(fragment);
        Log.i(AbstractFragment.class.getCanonicalName(), "open frag: "+fragment.getClass().getSimpleName()+" with tag "+fragment.getTag()+" and ref hashCode "+System.identityHashCode(fragment));

    }

    public void onFragmentLoad(FragmentHolderActivity activity) {
        if (activity.getSearchView() != null) {
            activity.reloadTopMenuButtons(this);
            activity.getSearchView().setQuery("", false);
        }
    }

    public void runInUIThread(Runnable toRun) {
        new Handler(Looper.getMainLooper()).post(toRun);
    }

    public void onSearch(String search) {
        //default implementation
        Log.w(getClass().getName(), "default implementation of onSearch() called");
    }

    public void onSearchType(String search) {
        //default implementation
    }

    public boolean supportsSearch() {
        return false;
    }

    public String getShareLink(){
        return shareUrl;
    }
}
