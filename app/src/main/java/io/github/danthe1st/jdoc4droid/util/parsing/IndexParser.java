package io.github.danthe1st.jdoc4droid.util.parsing;

import androidx.annotation.WorkerThread;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.danthe1st.jdoc4droid.model.SimpleClassDescription;

@WorkerThread
class IndexParser {

	private final Element summaryTable;

	private final Element summaryTableHeader;

	private IndexParser(Element summaryTable, Element summaryTableHeader) {
		this.summaryTable = summaryTable;
		this.summaryTableHeader = summaryTableHeader;
	}

	static List<SimpleClassDescription> parseClasses(File javaDocDir) throws IOException {
		File index = new File(javaDocDir, "allclasses-index.html");
		File noFrameFile = new File(javaDocDir, "allclasses-noframe.html");
		if(index.exists()) {
			return parseClassesFromAllclassesIndexFile(index);
		} else if(noFrameFile.exists()) {
			return parseClassesFromAllclassesNoframeFile(noFrameFile);
		} else {
			throw new IOException("neither allclasses-index.html nor allclasses-noframe.html found");
		}
	}

	private static List<SimpleClassDescription> parseClassesFromAllclassesIndexFile(File allclassesIndex) throws IOException {
		IndexParser parser = getParser(JavaDocParser.parseFile(allclassesIndex));
		if("table".equals(parser.summaryTable.tagName())) {
			return parser.parseClassesFromHTMLTable();
		} else {
			return parser.parseClassesFromVirtualClassListTable();
		}
	}

	private static IndexParser getParser(Document doc) throws IOException {
		return new IndexParser(getSummaryTable(doc), findTableHeader(doc));
	}

	private static Element getSummaryTable(Document doc) throws IOException {
		Elements table = doc.getElementsByClass("summary-table");
		if(table.isEmpty()) {
			table = doc.getElementsByTag("table");
		}
		if(table.isEmpty()) {
			throw new IOException("summary table not found");
		}
		return table.first();
	}

	private static Element findTableHeader(Document doc) {
		return doc.getElementsByClass("table-tabs").first();
	}

	private static List<SimpleClassDescription> parseClassesFromAllclassesNoframeFile(File allclassesNoframe) throws IOException {
		return JavaDocParser.parseFile(allclassesNoframe)
				.select(".indexContainer ul li")
				.stream()
				.map(li -> loadSimpleClassDescription(li, "", new HashMap<>()))
				.collect(Collectors.toList());
	}

	private List<SimpleClassDescription> parseClassesFromHTMLTable() {
		//TODO do this partially and add it to the list
		return summaryTable.children().parallelStream()
				.filter(elem -> "tbody".equals(elem.tagName()))
				.filter(elem -> !elem.select("td").isEmpty())
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("table does not have tbody"))
				.children()
				.parallelStream()
				.filter(elem -> elem.children().stream().anyMatch(c -> "td".equals(c.tagName())))
				.map(elem -> loadSimpleClassDescription(elem.child(0), elem.child(1).text(), new HashMap<>()))
				.collect(Collectors.toList());
	}

	private List<SimpleClassDescription> parseClassesFromVirtualClassListTable() {
		Map<String, String> tabMappings = summaryTableHeader == null ? new HashMap<>() : summaryTableHeader.children().stream()
				.filter(element -> !element.id().isEmpty())
				.collect(Collectors.toMap(Element::id, IndexParser::extractTabNameFromTabElement));
		SimpleClassDescription temp = null;
		List<SimpleClassDescription> descList = new ArrayList<>();
		for(Element child : summaryTable.children()) {
			if(child.hasClass("col-first") && child.childrenSize() > 0) {
				temp = loadSimpleClassDescription(child, "", tabMappings);
				descList.add(temp);
			} else if(child.hasClass("col-last") && temp != null) {
				temp.setDescription(child.text());
			}
		}
		return descList;
	}

	private static String extractTabNameFromTabElement(Element tabElement) {
		final String summaryText = " Summary";
		String text = tabElement.text();
		if(text.endsWith(summaryText)) {
			text = text.substring(0, text.length() - summaryText.length());
		}
		return text;
	}

	private static SimpleClassDescription loadSimpleClassDescription(Element link, String description, Map<String, String> tabMappings) {
		Optional<String> classType = findTabName(link)
				.filter(tabMappings::containsKey)
				.map(tabMappings::get);
		link = link
				.children()
				.stream()
				.filter(c -> "a".equals(c.tagName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Cannot load class description as no class description was found"));
		String[] inSplit = link.attr("title").split(" in ");
		String[] spaceSplit = link.attr("title").split(" ");
		String alternativeClassType;
		String packageName;
		if(inSplit.length > 1) {
			alternativeClassType = inSplit[0];
			packageName = inSplit[1];
		} else if(spaceSplit.length > 2) {
			alternativeClassType = spaceSplit[0];
			packageName = spaceSplit[2];
		} else {
			alternativeClassType = spaceSplit[0];
			packageName = "";
		}
		return new SimpleClassDescription(
				link.text(),
				description,
				classType.orElse(capitalize(alternativeClassType)),
				packageName,
				link.attr("href"));
	}

	private static Optional<String> findTabName(Element elem) {
		return elem
				.classNames()
				.stream()
				.filter(className ->
						className.matches("all-classes-table-tab\\d+")
				).findAny();
	}

	private static String capitalize(String toCapitalize) {
		char[] chars = toCapitalize.toCharArray();
		for(int i = 0; i < chars.length; i++) {
			if(i == 0 || Character.isWhitespace(chars[i - 1])) {
				chars[i] = Character.toUpperCase(chars[i]);
			}
		}
		return String.valueOf(chars);
	}
}
