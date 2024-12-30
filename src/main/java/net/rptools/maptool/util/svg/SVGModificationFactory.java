package net.rptools.maptool.util.svg;

import net.rptools.maptool.util.GraphicsUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SVGModificationFactory {
    private static final Logger log = LogManager.getLogger(SVGModificationFactory.class);
    public static final Map<String, String> SVG_RESOURCE_CACHE = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final String HALO_PROPERTIES_FILE_PATH = "net/rptools/maptool/client/halos.filters.properties";
    private static final String FILTER_UNITS = "filterUnits=\"userSpaceOnUse\" primitiveUnits=\"objectBoundingBox\"";
    private static boolean initialised = false;
    private static StringBuilder stringBuilder;
    private final Map<Integer, List<String>> changes = new HashMap<>();
    String svgDocAsString;
    private static final String SVG_START = "<svg";
    private static final String END_FIELD = ">";
    private int firstInsertionPoint = -1;
    private int secondInsertionPoint = -1;
    public static List<String> getAvailableFilters(){
        return SVG_RESOURCE_CACHE.keySet().stream().filter(string -> string.startsWith("filter.")).map(string -> string.replace("filter.", "")).collect(Collectors.toList());
    }

    SVGModificationFactory() {
        if (!initialised) {
            init();
        }
    }
    SVGModificationFactory(String svgDocAsString) {
        if (!initialised) {
            init();
        }
        this.svgDocAsString = svgDocAsString;
        stringBuilder = new StringBuilder(svgDocAsString);
    }
    protected String getSvgDocAsString(){
        if(!changes.isEmpty()){
            applyChanges();
        }
        return svgDocAsString;
    }
    public static SVGModificationFactory create(String svgDocAsString) {
        return new SVGModificationFactory(svgDocAsString).identifyInsertionPoints();
    }


    /**
     * There are two recurring points used for inserting additional parts, in the root node itself, and immediately
     * after as the first child.
     * i.e. directly after "<svg ", and the next "/>"
     * @return factory
     */
    private SVGModificationFactory identifyInsertionPoints(){
        String workingString = svgDocAsString.toLowerCase();
        firstInsertionPoint = workingString.indexOf(SVG_START) + 4;
        secondInsertionPoint = workingString.indexOf(END_FIELD, firstInsertionPoint) + 1;
        return this;
    }
    private void applyChanges(){
        // working from highest to lowest to avoid having to recalculate the insertion index every time.
        List<Integer> insertionPoints = changes.keySet().stream().toList().reversed();
        for(int i: insertionPoints){
            for(String s: changes.get(i)){
                stringBuilder.insert(i, " ");
                stringBuilder.insert(i, s);
                stringBuilder.insert(i, " ");
            }
        }
        svgDocAsString = stringBuilder.toString();
        stringBuilder = new StringBuilder(svgDocAsString);
        changes.clear();
        identifyInsertionPoints();
    }
    private void addChange(int index, String content){
        List<String> changeStrings;
        if(changes.containsKey(index)){
            changeStrings = changes.get(index);
            changeStrings.add(content);
        } else {
            changeStrings = List.of(content);
        }
        changes.put(index, changeStrings);
    }
    protected SVGModificationFactory addFilter(String filterName){
        if(SVG_RESOURCE_CACHE.containsKey(filterName)) {
            String filter = SVG_RESOURCE_CACHE.get(filterName);
            filter = filter.replace("<filter", "<filter " + FILTER_UNITS + " ");
            addChange(secondInsertionPoint, String.format("<defs>%s</defs>", filter));
            addChange(firstInsertionPoint, String.format("filter=\"url(#%s)\"", filterName.replace("filter.", "")));
        }
        return this;
    }
    private SVGModificationFactory removeSize() {
        svgDocAsString = svgDocAsString.replace("[hH][eE][iI][gG][hH][tT]=\"[\\d\\.\\w]+?\"", "")
                .replace("[wW][iI][dD][tT][hH]=\"[\\d\\.\\w]+?\"", "");
        stringBuilder = new StringBuilder(svgDocAsString);
        return this;
    }
    protected SVGModificationFactory setSize(double width, double height){
        removeSize().identifyInsertionPoints();
        addChange(firstInsertionPoint, "height=\"" + height + "\"");
        addChange(firstInsertionPoint, "width=\"" + width + "\"");
        return this;
    }
    public SVGModificationFactory replaceColours(List<Color> colourList){
        applyChanges();
        if(colourList.isEmpty()){
            return this;
        }
        String colourTarget = "<linearGradient id=\"colour%d\"><stop stop-color=\"%s\" stop-opacity=\"%f\"/></linearGradient>";
        String colourURL = "\"url(#colour%d)\"";
        Pattern pattern = Pattern.compile( "\"#0+(\\d{1,2})\"", Pattern.CASE_INSENSITIVE);
        Set<Integer> replaceable = new HashSet<>();
        Matcher matcher = pattern.matcher(svgDocAsString);
        while (matcher.find()) {
            replaceable.add(Integer.parseInt(matcher.group(1)));
        }
        int maxReplaceable = replaceable.stream().sorted().toList().getLast();
        Color c;
        String colourValue;
        StringBuilder targets = new StringBuilder();
        float alpha;
        for (int i = 0; i <= maxReplaceable; i++) {
            c = i >= colourList.size() - 1 ? colourList.getLast() : colourList.get(i);
            if(c == null) {
                alpha = 0;
            } else {
                alpha = c.getAlpha() / 255f;
            }
            colourValue = GraphicsUtil.getColourHexString(c);
            targets.append(String.format(colourTarget, i, colourValue, alpha));
            svgDocAsString = svgDocAsString.replaceAll("\"#0+?" + i + "\"", String.format(colourURL, i));
        }
        stringBuilder = new StringBuilder(svgDocAsString);
        identifyInsertionPoints();
        addChange(secondInsertionPoint, "<defs>" + targets + "</defs>");
        return this;
    }
    public void init() {
        try {
            Properties props = new Properties();
            InputStream inputStream = SVGDocumentUtils.class.getClassLoader().getResourceAsStream(HALO_PROPERTIES_FILE_PATH);
            if (inputStream == null) {
                throw new IOException();
            }
            try {
                props.load(inputStream);
                for (Enumeration<?> e = props.propertyNames();
                     e.hasMoreElements(); ) {
                    String key = e.nextElement().toString();
                    SVG_RESOURCE_CACHE.put(key, props.getProperty(key));
                }
            } catch (ClassCastException cce){
                log.info(cce.getMessage(), cce);
                throw new RuntimeException();
            }
        } catch (IOException e) {
            log.info(e.getMessage(), e);
            throw new RuntimeException();
        }
        initialised = true;
    }
}
