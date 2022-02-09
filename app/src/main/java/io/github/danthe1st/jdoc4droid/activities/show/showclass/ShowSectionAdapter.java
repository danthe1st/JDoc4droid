package io.github.danthe1st.jdoc4droid.activities.show.showclass;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.UiThread;

import java.util.ArrayList;
import java.util.List;

import io.github.danthe1st.jdoc4droid.R;
import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ShowSectionAdapter extends BaseAdapter {

    private final LayoutInflater inflater;

    @Getter
    @Setter
    private List<TextHolder> sections = new ArrayList<>();

    @Override
    public int getCount() {
        return sections.size();
    }

    @Override
    public Object getItem(int position) {
        return sections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int selectedItemPosition = ((Spinner) parent).getSelectedItemPosition();
        return createView(selectedItemPosition == -1 ? position : selectedItemPosition, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, parent);
    }

    @UiThread
    private View createView(int position, ViewGroup parent) {
        View view = inflater.inflate(R.layout.context_menu_item, parent, false);
        bindView(view, position);
        return view;
    }

    @UiThread
    private void bindView(View view, int position) {
        view.<TextView>findViewById(R.id.contextMenuField).setText(sections.get(position).getText());
    }

    public int getPositionFromName(TextHolder name) {
        for (int i = 0; i < sections.size(); i++) {
            if (name.equals(sections.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
