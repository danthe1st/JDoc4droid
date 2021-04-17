package io.github.danthe1st.jdoc4droid.activities;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.concurrent.ExecutorService;

import io.github.danthe1st.jdoc4droid.R;

public abstract class AbstractFragment extends Fragment {
    public FragmentHolderActivity getBelongingActivity(){
        return (FragmentHolderActivity) super.getActivity();
    }
    public ExecutorService getThreadPool(){
        return getBelongingActivity().getThreadPool();
    }

    protected void openFragment(AbstractFragment fragment) {
        openFragment(getBelongingActivity().getSupportFragmentManager(), fragment,getBelongingActivity());
    }

    public void goBack(){
        FragmentHolderActivity activity = getBelongingActivity();
        activity.getCurrentFragments().pop();
        if(activity.getCurrentFragments().isEmpty()){
            activity.moveTaskToBack(true);
        }else{
            AbstractFragment fragment=activity.getCurrentFragments().peek();
            getFragmentManager().beginTransaction().replace(R.id.fragHolder, fragment).commit();
            fragment.onFragmentLoad(activity);

        }
    }

    public static void openFragment(FragmentManager manager, AbstractFragment fragment, FragmentHolderActivity activity) {
        manager.beginTransaction().replace(R.id.fragHolder, fragment).commit();
        activity.getCurrentFragments().push(fragment);
        fragment.onFragmentLoad(activity);
    }
    public void onFragmentLoad(FragmentHolderActivity activity){
        if(activity.getSearchView()!=null){
            if(supportsSearch()){
                activity.getSearchView().setVisibility(View.VISIBLE);
            }else{
                activity.getSearchView().setVisibility(View.GONE);
            }
            activity.getSearchView().setQuery("",false);
        }
    }
    public void runInUIThread(Runnable toRun){
        new Handler(Looper.getMainLooper()).post(toRun);
    }
    public void onSearch(String search){
        //default implementation
        Log.w(getClass().getName(),"default implementaton of onSearch() called");
    }
    public void onSearchType(String search){
        //default implementation
    }
    public boolean supportsSearch(){
        return false;
    }
}
