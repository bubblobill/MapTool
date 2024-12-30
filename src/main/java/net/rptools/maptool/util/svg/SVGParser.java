package net.rptools.maptool.util.svg;

import net.rptools.maptool.model.DoubleSize;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calling this a parser is an insult to parsers everywhere but I didn't know what else to call it.
 * It performs the most rudimentary actions on the root element
 */
public class SVGParser {
    private static final Map<String, Map<String, String>> PARSER_HISTORY = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, Dimensions> PARSER_HISTORY_DIMENSIONS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    SVGParser(String svgDocAsString){
        if(!PARSER_HISTORY.containsKey(svgDocAsString)){
            parse(svgDocAsString);
        }
    }
    public void parse(String svgDocAsString){
        Parser p = new Parser(svgDocAsString);
        PARSER_HISTORY.put(svgDocAsString, p.getKeyValuePairs());
        Dimensions d = p.getDimensions();
        if(d.isValid()) {
            PARSER_HISTORY_DIMENSIONS.put(svgDocAsString, d);
        }
    }
    public record Dimensions(DoubleSize doubleSize, Rectangle2D viewBox){
        boolean isValid(){
            return doubleSize.getHeight() != -1 && doubleSize.getWidth() != -1 && !viewBox.isEmpty();
        }
    }
//    public record Attributes(ImmutablePair<L,R> pair...)
    public Dimensions getDimensions(String svgDocAsString){
        if(PARSER_HISTORY_DIMENSIONS.containsKey(svgDocAsString)){
            return PARSER_HISTORY_DIMENSIONS.get(svgDocAsString);
        } else if(PARSER_HISTORY.containsKey(svgDocAsString)) {
            // tried it before and it didn't work
            return null;
        } else {
            parse(svgDocAsString);
            return getDimensions(svgDocAsString);
        }
    }
    private static class Parser{
        static final Map<String, String> keyValuePairs = new HashMap<>();
        static String workString;
        static DoubleSize doubleSize = new DoubleSize(-1,-1);
        static Rectangle2D viewBox = new Rectangle2D.Double();

        Dimensions getDimensions(){
            return new Dimensions(doubleSize, viewBox);
        }
        private Parser(String originalString){
            workString = originalString.toLowerCase();
            isolateRootNode();
            findKeyValuePairs();
        }
        public Map<String, String> getKeyValuePairs(){ return keyValuePairs; }
        private void isolateRootNode(){
            int startIdx = workString.indexOf("<svg"); // what we want should be in the opening block
            int limit = workString.indexOf(">", startIdx + 4) + 1;
            workString = workString.substring(startIdx, limit);
        }
        private void findKeyValuePairs(){
            int i = 0;
            do {
                i = workString.indexOf("=", i + 1);
                if(i != -1) {
                    String key = getKeyString(i);
                    String value = getValueString(i);
                    if (key.equals("width") || key.equals("height") || key.equals("viewbox")) {
                        setDimensions(key, value);
                    }
                    keyValuePairs.put(key, value);
                }
            } while(i < workString.length() - 1 && i > -1);

            if((doubleSize.getHeight() == -1 || doubleSize.getWidth() == -1) && !viewBox.isEmpty()) {
                doubleSize.setSize(viewBox.getWidth(), viewBox.getHeight());
            } else if(doubleSize.getHeight() != -1 && doubleSize.getWidth() != -1 && viewBox.isEmpty()){
                viewBox = new Rectangle2D.Double(0,0,doubleSize.getWidth(), doubleSize.getHeight());
            }
        }
        private String getKeyString(int i){
            int end = i;
            i--;
            char prevChar = workString.charAt(i);
            while(prevChar != ' ' && prevChar != ',' && prevChar != '=' && i > 0) {
                i--;
                prevChar = workString.charAt(i);
            }
            return workString.substring(i + 1, end).trim();
        }
        private String getValueString(int i){
            i+=2;
            int start = i;
            char nextChar = workString.charAt(start);
            while(nextChar != '"' && nextChar != '=' && i < workString.length()-1) {
                i++;
                nextChar = workString.charAt(i);
            }
            return workString.substring(start, i).trim();
        }
        private double parseMeasurementFieldValue(String s) {
            return Double.parseDouble(
                s.replaceAll("[,\"a-z]", "")
            );
        }
        private Rectangle2D parseViewBoxValue(String s) {
            s = s.replaceAll(",", " ").replaceAll(" {2,}", " ");
            String[] values = s.split(" ");
            return new Rectangle2D.Double(
                    Double.parseDouble(values[0]),
                    Double.parseDouble(values[1]),
                    Double.parseDouble(values[2]),
                    Double.parseDouble(values[3])
            );
        }
        private void setDimensions(String key, String value) {
                switch (key){
                    case "width" -> doubleSize.setWidth(parseMeasurementFieldValue(value));
                    case "height" ->doubleSize.setHeight(parseMeasurementFieldValue(value));
                    case "viewbox" -> viewBox.setRect(parseViewBoxValue(value));
                }
            }
        }
    }

