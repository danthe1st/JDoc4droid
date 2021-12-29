package io.github.danthe1st.jdoc4droid.activities.show.showclass;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
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
import io.github.danthe1st.jdoc4droid.activities.AbstractActivity;
import io.github.danthe1st.jdoc4droid.model.ClassInformation;
import io.github.danthe1st.jdoc4droid.model.textholder.HtmlStringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.StringHolder;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import io.github.danthe1st.jdoc4droid.util.JavaDocLinkMovementMethod;
import io.github.danthe1st.jdoc4droid.util.parsing.JavaDocParser;

public class ShowClassActivity extends AbstractActivity {

    private static final String ARG_CLASS_FILE_PATH = "classFile";
    private static final String ARG_SELECTED_ID = "selected";
    private static final String ARG_BASE_SHARE_URL = "baseShareUrl";
    private static final String ARG_BASE_JAVADOC_DIR = "baseJavadocDir";
    private static final String STATE_SELECTION_OUTER = "outerSelection";
    private static final String STATE_SELECTION_MIDDLE = "middleSelection";
    private static final String STATE_SELECTION_INNER = "innerSelection";

    private File classFile;
    private String selectedId;

    private ClassInformation information = new ClassInformation();

    private ShowSectionAdapter outerAdapter;
    private ShowSectionAdapter middleAdapter;
    private ShowSectionAdapter innerAdapter;

    private TextView textView;

    private String baseShareUrl;
    private String baseJavadocDir;


    public ShowClassActivity() {
        // Required empty public constructor
    }

    @UiThread
    public static void open(Context ctx, File baseDir, File classFile, String baseShareUrl) {
        open(ctx, baseDir, classFile, baseShareUrl, null);
    }

    @UiThread
    private static void open(Context ctx, File baseDir,File classFile, String baseShareUrl, String selectedId) {
        Intent intent=new Intent(ctx, ShowClassActivity.class);

        intent.putExtra(ARG_CLASS_FILE_PATH, classFile.getAbsolutePath());
        intent.putExtra(ARG_SELECTED_ID, selectedId);
        intent.putExtra(ARG_BASE_SHARE_URL,baseShareUrl);
        intent.putExtra(ARG_BASE_JAVADOC_DIR,baseDir.getAbsolutePath());
        intent.putExtra(ARG_SHARE_URL,loadShareUrl(baseShareUrl,baseDir,classFile));

        ctx.startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveSelection(outState,STATE_SELECTION_OUTER,information.getSelectedOuterSection());
        saveSelection(outState,STATE_SELECTION_MIDDLE,information.getSelectedMiddleSection());
        saveSelection(outState,STATE_SELECTION_INNER,information.getSelectedInnerSection());
        super.onSaveInstanceState(outState);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_class);
        classFile = new File(getIntent().getStringExtra(ARG_CLASS_FILE_PATH));
        baseShareUrl=getIntent().getStringExtra(ARG_BASE_SHARE_URL);
        baseJavadocDir=getIntent().getStringExtra(ARG_BASE_JAVADOC_DIR);
        selectedId = getIntent().getStringExtra(ARG_SELECTED_ID);

        if(savedInstanceState!=null){
            information.setSelectedOuterSection(loadSelection(savedInstanceState,STATE_SELECTION_OUTER));
            information.setSelectedMiddleSection(loadSelection(savedInstanceState,STATE_SELECTION_MIDDLE));
            information.setSelectedInnerSection(loadSelection(savedInstanceState,STATE_SELECTION_INNER));
        }

        outerAdapter = new ShowSectionAdapter(getLayoutInflater());
        middleAdapter = new ShowSectionAdapter(getLayoutInflater());
        innerAdapter = new ShowSectionAdapter(getLayoutInflater());

        textView = findViewById(R.id.contentView);
        textView.setMovementMethod(new JavaDocLinkMovementMethod(this::linkClicked));

        TextView headerView = findViewById(R.id.headerView);
        headerView.setMovementMethod(new JavaDocLinkMovementMethod(this::linkClicked));

        Spinner outerSelectionSpinner = findViewById(R.id.mainSectionSpinner);
        Spinner middleSelectionSpinner = findViewById(R.id.middleSectionSpinner);
        Spinner innerSelectionSpinner = findViewById(R.id.innerSelectionSpinner);
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
                Log.i(getClass().getName(), "nothing selected (o)");
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
                Log.i(getClass().getName(), "nothing selected (m)");
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
                Log.i(getClass().getName(), "nothing selected (i)");
            }
        });

        getThreadPool().execute(() -> {
            try {
                ClassInformation newInfo=JavaDocParser.loadClassInformation(classFile, selectedId);//pass information directly
                if(information.getSelectedOuterSection()!=null){
                    newInfo.setSelectedOuterSection(information.getSelectedOuterSection());
                }
                if(information.getSelectedMiddleSection()!=null){
                    newInfo.setSelectedMiddleSection(information.getSelectedMiddleSection());
                }
                if(information.getSelectedInnerSection()!=null){
                    newInfo.setSelectedInnerSection(information.getSelectedInnerSection());
                }
                information = newInfo;

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
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    headerView.setText(information.getHeader().getText());
                    outerAdapter.notifyDataSetChanged();
                });
            } catch (IOException e) {
                Log.e(ShowClassActivity.class.getName(), "cannot parse class", e);
            }
        });
    }
    private void saveSelection(Bundle outState, String selectionStateName, TextHolder selectedSection){
        if(selectedSection==null){
            return;
        }
        outState.putString(selectionStateName,selectedSection.getRawText());
        outState.putString(selectionStateName+"Type",selectedSection.getClass().getSimpleName());
        outState.putString(selectionStateName+"MainName",selectedSection.getMainName());
        if(selectedSection instanceof HtmlStringHolder){
            outState.putInt(selectionStateName+"Flags",((HtmlStringHolder)selectedSection).getFlags());
        }
    }

    private TextHolder loadSelection(Bundle inState, String selectionStateName){
        String rawText=inState.getString(selectionStateName);
        if(rawText==null){
            return null;
        }
        String typeName=inState.getString(selectionStateName+"Type");
        String mainName=inState.getString(selectionStateName+"MainName");
        if (StringHolder.class.getSimpleName().equals(typeName)) {
            return new StringHolder(rawText, mainName);
        } else if (HtmlStringHolder.class.getSimpleName().equals(typeName)) {
            return new HtmlStringHolder(rawText, inState.getInt(selectionStateName+"Flags"), mainName);
        }
        throw new IllegalStateException("trying to load invalid StringHolder: "+typeName);
    }

    private static String loadShareUrl(String baseUrl,File baseDir,File actualFile){
        String shareUrl=baseUrl;
        if(shareUrl!=null){
            shareUrl += baseDir.toPath().relativize(actualFile.toPath());
        }
        return shareUrl;
    }

    @UiThread
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
            onMiddleSelected(innerSelectionSpinner, middleAdapter.getSections().indexOf(information.getSelectedMiddleSection()));
        }
    }

    @UiThread
    private void onMiddleSelected(Spinner innerSelectionSpinner, int position) {
        if(position==-1){
            position=0;
        }
        TextHolder middleSelected = middleAdapter.getSections().get(position);
        information.setSelectedMiddleSection(middleSelected);
        Map<TextHolder, TextHolder> selected = information.getSections().get(information.getSelectedOuterSection()).get(middleSelected);
        if (selected == null) {
            //do nothing
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

    @UiThread
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
        onInnerSelected(innerAdapter.getSections().indexOf(information.getSelectedInnerSection()));
    }

    private boolean containsText(TextHolder textHolder,String textToContain){
        textToContain=textToContain.toLowerCase();
        if(textHolder.getMainName()==null){
            return textHolder.getText().toString().toLowerCase().contains(textToContain);
        }else{
            return textHolder.getMainName().toLowerCase().contains(textToContain);
        }
    }

    @UiThread
    private void onInnerSelected(int position) {
        if(position==-1){
            position=0;
        }
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

    @UiThread
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
                //TODO implement this
            } else {
                //TODO set option/Section/scroll/whatever (split[1]), also if self link (split[0] empty)
                open(this,new File(baseJavadocDir),file,baseShareUrl,split.length > 1 ? split[1] : null);
            }
            return false;

        } else {
            Log.i(getClass().getName(), "non-file link clicked");
            uri=Uri.parse(link);
        }
        //let it be handled by sth else
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(getPackageManager()) != null) {
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
        if(information.getSelectedInnerSection()!=null){
            loadInnerSections(information.getSections().get(information.getSelectedOuterSection()).get(information.getSelectedMiddleSection()),search);
        }
    }


    @Override
    public void onSearchType(String search) {
        onSearch(search);
    }

    @Override
    public String getShareLink() {
        TextHolder selected=getSelected();
        String id="";
        if(selected instanceof HtmlStringHolder){
            id = "#"+((HtmlStringHolder) selected).getId();
        }
        return super.getShareLink()+id;
    }
    private TextHolder getSelected(){
        Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> sections = information.getSections();
        if(sections==null){
            return null;
        }
        Map<TextHolder, Map<TextHolder, TextHolder>> outer = sections.get(information.getSelectedOuterSection());
        Map<TextHolder, TextHolder> middle;
        if(outer==null){
            return null;
        }
        if(outer.size()>1){
            middle = outer.get(information.getSelectedMiddleSection());
        }else{
            middle=outer.values().stream().findAny().orElseThrow(IllegalStateException::new);
        }
        if(middle.size()>1){
            return middle.get(information.getSelectedInnerSection());
        }else {
            return middle.values().stream().findAny().orElseThrow(IllegalStateException::new);
        }
    }
}
