package io.github.danthe1st.jdoc4droid.activities.show.showclass;

import android.content.Intent;
import android.net.Uri;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
    private static final String ARG_BASE_SHARE_URL = "baseShareUrl";
    private static final String ARG_BASE_JAVADOC_DIR = "baseJavadocDir";

    private File classFile;
    private String selectedId;

    private ClassInformation information = new ClassInformation();

    private ShowSectionAdapter outerAdapter;
    private ShowSectionAdapter middleAdapter;
    private ShowSectionAdapter innerAdapter;

    private TextView textView;

    private String baseShareUrl;
    private String baseJavadocDir;


    public ShowClassFragment() {
        // Required empty public constructor
    }

    private static ShowClassFragment newInstance(File baseDir,File classFile, String baseShareUrl, String selectedId) {
        ShowClassFragment fragment = new ShowClassFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLASS_FILE_PATH, classFile.getAbsolutePath());
        args.putString(ARG_SELECTED_ID, selectedId);
        args.putString(ARG_BASE_SHARE_URL,baseShareUrl);
        args.putString(ARG_BASE_JAVADOC_DIR,baseDir.getAbsolutePath());
        args.putString(ARG_SHARE_URL,loadShareUrl(baseShareUrl,baseDir,classFile));

        fragment.setArguments(args);
        return fragment;
    }

    public static ShowClassFragment newInstance(File baseDir,File classFile, String baseShareUrl) {
        return newInstance(baseDir, classFile, baseShareUrl, null);
    }

    private static String loadShareUrl(String baseUrl,File baseDir,File actualFile){
        String shareUrl=baseUrl;
        if(shareUrl!=null){
            URI baseUri = baseDir.toURI();
            URI actualUri = actualFile.toURI();
            URI relativePath = baseUri.relativize(actualUri);
            shareUrl += relativePath.getPath();
        }
        return shareUrl;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            classFile = new File(getArguments().getString(ARG_CLASS_FILE_PATH));
            baseShareUrl=getArguments().getString(ARG_BASE_SHARE_URL);
            baseJavadocDir=getArguments().getString(ARG_BASE_JAVADOC_DIR);
            selectedId = getArguments().getString(ARG_SELECTED_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ConstraintLayout view = (ConstraintLayout) inflater.inflate(R.layout.fragment_show_class, container, false);

        outerAdapter = new ShowSectionAdapter(inflater);
        middleAdapter = new ShowSectionAdapter(inflater);
        innerAdapter = new ShowSectionAdapter(inflater);

        textView = view.findViewById(R.id.contentView);
        textView.setMovementMethod(new JavaDocLinkMovementMethod(this::linkClicked));

        TextView headerView = view.findViewById(R.id.headerView);
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
                onOuterSelected(middleSelectionSpinner, innerSelectionSpinner, position);
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
                onMiddleSelected(innerSelectionSpinner, position);
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
                onInnerSelected(position);
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
                            onOuterSelected(middleSelectionSpinner, innerSelectionSpinner, pos);
                            if (information.getSelectedMiddleSection() != null) {
                                pos = middleAdapter.getPositionFromName(information.getSelectedMiddleSection());
                                if (pos != -1) {
                                    middleSelectionSpinner.setSelection(pos);
                                    onMiddleSelected(innerSelectionSpinner, pos);
                                    if (information.getSelectedInnerSection() != null) {
                                        pos = innerAdapter.getPositionFromName(information.getSelectedInnerSection());
                                        if (pos != -1) {
                                            innerSelectionSpinner.setSelection(pos);
                                            onInnerSelected(pos);
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

    private void onOuterSelected(Spinner middleSelectionSpinner, Spinner innerSelectionSpinner, int position) {
        TextHolder outerSelected = outerAdapter.getSections().get(position);
        information.setSelectedOuterSection(outerSelected);
        Map<TextHolder, Map<TextHolder, TextHolder>> selected = information.getSections().get(outerSelected);
        if (selected.size() == 1 && selected.containsKey(TextHolder.EMPTY)) {
            middleSelectionSpinner.setVisibility(View.GONE);
            middleSelectionSpinner.setWillNotDraw(true);
            innerSelectionSpinner.setVisibility(View.GONE);
            innerSelectionSpinner.setWillNotDraw(true);
            textView.setText(selected.values().stream().flatMap(x -> x.values().stream()).findAny().get().getText());
        } else {
            middleSelectionSpinner.setVisibility(View.VISIBLE);
            middleSelectionSpinner.setWillNotDraw(false);
            middleAdapter.setSections(new ArrayList<>(selected.keySet()));
            middleAdapter.notifyDataSetChanged();
            onMiddleSelected(innerSelectionSpinner, 0);
        }
    }

    private void onMiddleSelected(Spinner innerSelectionSpinner, int position) {
        TextHolder middleSelected = middleAdapter.getSections().get(position);
        information.setSelectedMiddleSection(middleSelected);
        Map<TextHolder, TextHolder> selected = information.getSections().get(information.getSelectedOuterSection()).get(middleSelected);
        if (selected == null) {

        } else if (selected.size() == 1 && selected.containsKey(TextHolder.EMPTY)) {
            innerSelectionSpinner.setVisibility(View.GONE);
            innerSelectionSpinner.setWillNotDraw(true);
            textView.setText(selected.values().stream().findAny().map(TextHolder::getText).orElse(null));
        } else {
            innerSelectionSpinner.setVisibility(View.VISIBLE);
            innerSelectionSpinner.setWillNotDraw(false);
            loadInnerSections(selected,null);
        }
    }

    private void loadInnerSections(Map<TextHolder, TextHolder> selected, String search){
        List<TextHolder> sections=new ArrayList<>(selected.keySet());
        if(search!=null){
            sections.removeIf(section->!containsText(section,search));
            if(sections.isEmpty()){
                sections=new ArrayList<>(selected.keySet());
            }
        }
        innerAdapter.setSections(sections);
        innerAdapter.notifyDataSetChanged();
        onInnerSelected(0);
    }

    private boolean containsText(TextHolder textHolder,String textToContain){
        textToContain=textToContain.toLowerCase();
        if(textHolder.getMainName()==null){
            return textHolder.getText().toString().toLowerCase().contains(textToContain);
        }else{
            return textHolder.getMainName().toLowerCase().contains(textToContain);
        }
    }

    private void onInnerSelected(int position) {
        TextHolder innerSelected = innerAdapter.getSections().get(position);
        information.setSelectedInnerSection(innerSelected);
        Map<TextHolder, Map<TextHolder, TextHolder>> outerSection = information.getSections().get(information.getSelectedOuterSection());
        if (outerSection != null) {
            Map<TextHolder, TextHolder> middleSection = outerSection.get(information.getSelectedMiddleSection());
            TextHolder selected = middleSection.get(innerSelected);
            if (selected != null) {
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
        Uri uri;
        if (file.isFile()) {
            if (file.getName().endsWith("-summary.html")) {
                Log.i(getClass().getName(), "Tried to access a summary link, not implemented");
                //TODO fix this
            } else {
                //TODO set option/Section/scroll/whatever (split[1]), also if self link (split[0] empty)
                openFragment(ShowClassFragment.newInstance(new File(baseJavadocDir),file,baseShareUrl,split.length > 1 ? split[1] : null));
            }
            return false;

        } else {
            Log.i(getClass().getName(), "non-file link clicked");
            uri=Uri.parse(link);
        }
        //let it be handled by sth else
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(getBelongingActivity().getPackageManager()) != null) {
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public void onSearch(String search) {
        if(information.getSelectedInnerSection()==null){
            //TODO
        }else{
            loadInnerSections(information.getSections().get(information.getSelectedOuterSection()).get(information.getSelectedMiddleSection()),search);
        }
    }


    @Override
    public void onSearchType(String search) {
        onSearch(search);
    }
}
