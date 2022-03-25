package io.github.danthe1st.jdoc4droid.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import io.github.danthe1st.jdoc4droid.model.textholder.TextHolder;

public class ClassInformation implements Serializable {
    private static final long serialVersionUID = 7618988607928849793L;

    private TextHolder header;
    private Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> sections;
    private TextHolder selectedOuterSection;
    private TextHolder selectedMiddleSection;
    private TextHolder selectedInnerSection;

    //region boilerplate
    public ClassInformation() {
    }

    public ClassInformation(TextHolder header, Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> sections, TextHolder selectedOuterSection, TextHolder selectedMiddleSection, TextHolder selectedInnerSection) {
        this.header = header;
        this.sections = sections;
        this.selectedOuterSection = selectedOuterSection;
        this.selectedMiddleSection = selectedMiddleSection;
        this.selectedInnerSection = selectedInnerSection;
    }

    public TextHolder getHeader() {
        return this.header;
    }

    public Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> getSections() {
        return this.sections;
    }

    public TextHolder getSelectedOuterSection() {
        return this.selectedOuterSection;
    }

    public TextHolder getSelectedMiddleSection() {
        return this.selectedMiddleSection;
    }

    public TextHolder getSelectedInnerSection() {
        return this.selectedInnerSection;
    }

    public void setHeader(TextHolder header) {
        this.header = header;
    }

    public void setSections(Map<TextHolder, Map<TextHolder, Map<TextHolder, TextHolder>>> sections) {
        this.sections = sections;
    }

    public void setSelectedOuterSection(TextHolder selectedOuterSection) {
        this.selectedOuterSection = selectedOuterSection;
    }

    public void setSelectedMiddleSection(TextHolder selectedMiddleSection) {
        this.selectedMiddleSection = selectedMiddleSection;
    }

    public void setSelectedInnerSection(TextHolder selectedInnerSection) {
        this.selectedInnerSection = selectedInnerSection;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        ClassInformation that = (ClassInformation) o;
        return Objects.equals(header, that.header) && Objects.equals(sections, that.sections) && Objects.equals(selectedOuterSection, that.selectedOuterSection) && Objects.equals(selectedMiddleSection, that.selectedMiddleSection) && Objects.equals(selectedInnerSection, that.selectedInnerSection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, sections, selectedOuterSection, selectedMiddleSection, selectedInnerSection);
    }

    @Override
    public String toString() {
        return "ClassInformation{" +
                "header=" + header +
                ", sections=" + sections +
                ", selectedOuterSection=" + selectedOuterSection +
                ", selectedMiddleSection=" + selectedMiddleSection +
                ", selectedInnerSection=" + selectedInnerSection +
                '}';
    }
    //endregion
}
