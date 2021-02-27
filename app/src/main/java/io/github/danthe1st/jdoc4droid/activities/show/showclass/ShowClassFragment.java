package io.github.danthe1st.jdoc4droid.activities.show.showclass;

import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.activities.AbstractFragment;
import io.github.danthe1st.jdoc4droid.model.ClassInformation;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import io.github.danthe1st.jdoc4droid.util.JavaDocLinkMovementMethod;
import io.github.danthe1st.jdoc4droid.util.JavaDocParser;

public class ShowClassFragment extends AbstractFragment {

    private static final String ARG_CLASS_FILE_PATH = "classFile";
    private static final String ARG_SELECTED_ID = "selected";

    private File classFile;
    private String selectedId;

    private ClassInformation information = new ClassInformation();

    private ShowSectionAdapter outerAdapter = new ShowSectionAdapter();
    private ShowSectionAdapter middleAdapter = new ShowSectionAdapter();
    private ShowSectionAdapter innerAdapter = new ShowSectionAdapter();


    public ShowClassFragment() {
        // Required empty public constructor
    }

    public static ShowClassFragment newInstance(File classFile, String selectedId) {
        ShowClassFragment fragment = new ShowClassFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLASS_FILE_PATH, classFile.getAbsolutePath());
        args.putString(ARG_SELECTED_ID, selectedId);
        fragment.setArguments(args);
        return fragment;
    }

    public static ShowClassFragment newInstance(File classFile) {
        return newInstance(classFile, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            classFile = new File(getArguments().getString(ARG_CLASS_FILE_PATH));
            selectedId = getArguments().getString(ARG_SELECTED_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ConstraintLayout view = (ConstraintLayout) inflater.inflate(R.layout.fragment_show_class, container, false);

        TextView textView = view.findViewById(R.id.contentView);
        textView.setMovementMethod(new JavaDocLinkMovementMethod(this::linkClicked));

        TextView headerView=view.findViewById(R.id.headerView);
        headerView.setMovementMethod(new JavaDocLinkMovementMethod(this::linkClicked));

        Spinner outerSelectionSpinner = view.findViewById(R.id.mainSectionSpinner);
        Spinner middleSelectionSpinner = view.findViewById(R.id.middleSectionSpinner);
        Spinner innerSelectionSpinner = view.findViewById(R.id.innerSelectionSpinner);
        outerSelectionSpinner.setAdapter(outerAdapter);
        middleSelectionSpinner.setAdapter(middleAdapter);
        innerSelectionSpinner.setAdapter(innerAdapter);

        outerSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                onOuterSelected(middleSelectionSpinner,innerSelectionSpinner, textView, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //ignore
                Log.e(getClass().getName(), "nothing selected (o)");
            }
        });
        middleSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                onMiddleSelected(innerSelectionSpinner,textView, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //ignore
                Log.e(getClass().getName(), "nothing selected (m)");
            }
        });
        innerSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                onInnerSelected(textView, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //ignore
                Log.e(getClass().getName(), "nothing selected (i)");
            }
        });

        getThreadPool().execute(() -> {
            try {
                information = JavaDocParser.loadClassInformation(classFile, selectedId);//pass information directly

                outerAdapter.setSections(new ArrayList<>(information.getSections().keySet()));
                runInUIThread(() -> {
                    if (information.getSelectedOuterSection() != null) {
                        int pos = outerAdapter.getPositionFromName(information.getSelectedOuterSection());
                        if (pos != -1) {
                            outerSelectionSpinner.setSelection(pos);
                            onOuterSelected(middleSelectionSpinner,innerSelectionSpinner, textView, pos);
                            if (information.getSelectedMiddleSection() != null) {
                                pos = middleAdapter.getPositionFromName(information.getSelectedMiddleSection());
                                if (pos != -1) {
                                    middleSelectionSpinner.setSelection(pos);
                                    onMiddleSelected(innerSelectionSpinner,textView, pos);
                                    if (information.getSelectedInnerSection() != null) {
                                        pos = innerAdapter.getPositionFromName(information.getSelectedInnerSection());
                                        if (pos != -1) {
                                            innerSelectionSpinner.setSelection(pos);
                                            onInnerSelected(textView, pos);
                                        }
                                    }
                                }
                            }
                        }

                    }
                    headerView.setText(information.getHeader().getText());

                    outerAdapter.notifyDataSetChanged();
                });
            } catch (IOException e) {
                Log.e(ShowClassFragment.class.getName(), "cannot parse class", e);
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void onOuterSelected(Spinner middleSelectionSpinner, Spinner innerSelectionSpinner, TextView textView, int position) {
        TextHolder outerSelected = outerAdapter.getSections().get(position);
        information.setSelectedOuterSection(outerSelected);
        Map<TextHolder,Map<TextHolder, TextHolder>> selected = information.getSections().get(outerSelected);
        if (selected.size() == 1&&selected.containsKey(TextHolder.EMPTY)) {
            middleSelectionSpinner.setVisibility(View.GONE);
            middleSelectionSpinner.setWillNotDraw(true);
            innerSelectionSpinner.setVisibility(View.GONE);
            innerSelectionSpinner.setWillNotDraw(true);
            textView.setText(selected.values().stream().flatMap(x->x.values().stream()).findAny().get().getText());
        } else {
            middleSelectionSpinner.setVisibility(View.VISIBLE);
            middleSelectionSpinner.setWillNotDraw(false);
            middleAdapter.setSections(new ArrayList<>(selected.keySet()));
            middleAdapter.notifyDataSetChanged();
            onMiddleSelected(innerSelectionSpinner,textView, 0);
        }
    }

    private void onMiddleSelected(Spinner innerSelectionSpinner,TextView textView,int position){
        TextHolder middleSelected = middleAdapter.getSections().get(position);
        information.setSelectedMiddleSection(middleSelected);
        Map<TextHolder, TextHolder> selected = information.getSections().get(information.getSelectedOuterSection()).get(middleSelected);
        if(selected==null){

        }else if (selected.size() == 1&&selected.containsKey(TextHolder.EMPTY)) {
            innerSelectionSpinner.setVisibility(View.GONE);
            innerSelectionSpinner.setWillNotDraw(true);
            textView.setText(selected.values().stream().findAny().map(TextHolder::getText).orElse(null));
        } else {
            innerSelectionSpinner.setVisibility(View.VISIBLE);
            innerSelectionSpinner.setWillNotDraw(false);
            innerAdapter.setSections(new ArrayList<>(selected.keySet()));
            innerAdapter.notifyDataSetChanged();
            //onInnerSelected(textView, 0);
        }
    }
    private void onInnerSelected(TextView textView, int position) {

        TextHolder innerSelected = innerAdapter.getSections().get(position);
        information.setSelectedInnerSection(innerSelected);
        Map<TextHolder, Map<TextHolder, TextHolder>> outerSection = information.getSections().get(information.getSelectedOuterSection());
        if(outerSection!=null){
            Map<TextHolder, TextHolder> middleSection=outerSection.get(information.getSelectedMiddleSection());
            TextHolder selected = middleSection.get(innerSelected);
            if(selected!=null){
                textView.setText(selected.getText());
            }
        }
    }

    private boolean linkClicked(String link) {
        String[] split = link.split(Pattern.quote("#"), 2);
        File file;
        if (split[0].isEmpty()) {
            file = classFile;
        } else {
            file = new File(classFile.getParent(), split[0]);
        }
        Log.i(getClass().getName(), "load link: " + file + " (" + link + ")");
        if (file.isFile()) {
            if (file.getName().endsWith("-summary.html")) {
                Log.i(getClass().getName(), "Tried to access a summary link, not implemented");
                //TODO fix this
            } else {
                //TODO set option/Section/scroll/whatever (split[1]), also if self link (split[0] empty)
                openFragment(ShowClassFragment.newInstance(file, split.length > 1 ? split[1] : null));
            }
        } else {
            Log.i(getClass().getName(), "non-file link clicked");
        }
        //let it be handled by sth else
        return false;
    }
}