package net.rptools.maptool.util.svg;

import com.github.weisj.jsvg.SVGDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.util.List;


public class SVGUtil {
    private static final Logger log = LogManager.getLogger(SVGUtil.class);

    static SVGDocumentUtils svgDocumentUtils = new SVGDocumentUtils();
    public static @Nullable String getSVGString(String resourcePath) {
        try {
            return SVGLoaders.getSVGFileAsString(resourcePath);
        } catch (NullPointerException e) {
            log.info("Invalid path for getSVGDocument: " + resourcePath);
            return null;
        }
    }

    public static String addDimensions(String svgDocAsString) {
        String workingString = svgDocAsString.toLowerCase();
        int startIdx, limitIdx, hIdx, wIdx;
        startIdx = workingString.indexOf("<svg") + 4; // what we want should be in the opening block
        limitIdx = workingString.indexOf(">", startIdx);
        hIdx = workingString.indexOf("height=\"", 0, limitIdx) + 8;
        wIdx = workingString.indexOf("width=\"", 0, limitIdx) + 7;
        if (hIdx != 7 || wIdx != 6) {
            // has dimensions, return unchanged
            return svgDocAsString;
        }
        // no dimensions explicitly set, try to get them from the viewBox
        float h, w;
        int vbStartIdx, vbEndIdx;
        vbStartIdx = workingString.indexOf("viewbox=\"") + 9;
        if (vbStartIdx == 8) {
            // no viewBox, give-up
            return svgDocAsString;
        }
        vbEndIdx = workingString.indexOf("\"", vbStartIdx);
        String vb = workingString.substring(vbStartIdx, vbEndIdx);
        String[] vals = vb.split("[\\s|,]+");
        h = Float.parseFloat(vals[3]);
        w = Float.parseFloat(vals[2]);
        String addThis = String.format(" width=\"%.4f\" height=\"%.4f\" ", w, h);
        String res = new StringBuilder(svgDocAsString).insert(startIdx, addThis).toString();
        return res;
    }
    static String filter = "<defs> <!-- Filter declaration --> <filter id=\"MyFilter\" filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"200\" height=\"120\"> <!-- offsetBlur --> <feGaussianBlur in=\"SourceAlpha\" stdDeviation=\"4\" result=\"blur\" /> <feOffset in=\"blur\" dx=\"4\" dy=\"4\" result=\"offsetBlur\" />  <!-- litPaint --> <feSpecularLighting in=\"blur\" surfaceScale=\"5\" specularConstant=\".75\" specularExponent=\"20\" lighting-color=\"#bbbbbb\" result=\"specOut\"> <fePointLight x=\"-5000\" y=\"-10000\" z=\"20000\" /> </feSpecularLighting> <feComposite in=\"specOut\" in2=\"SourceAlpha\" operator=\"in\" result=\"specOut\" /> <feComposite in=\"SourceGraphic\" in2=\"specOut\" operator=\"arithmetic\" k1=\"0\" k2=\"1\" k3=\"1\" k4=\"0\" result=\"litPaint\" /><!-- merge offsetBlur + litPaint --> <feMerge> <feMergeNode in=\"offsetBlur\" /> <feMergeNode in=\"litPaint\" /> </feMerge> </filter></defs><!-- Graphic elements --><g filter=\"url(#MyFilter)\">";
    public static String addFilter(String svgDocAsString){
        String workingString = svgDocAsString.toLowerCase();
        boolean hasDefs = workingString.contains("<defs>");
        if(hasDefs){
            return svgDocAsString;
        }
        int filterInsertPoint = workingString.indexOf("\">") + 2;
        int endInsertPoint = workingString.indexOf("</svg>");
        if(filterInsertPoint == -1 || endInsertPoint == -1){
            return svgDocAsString;
        }
        return new StringBuilder(svgDocAsString).insert(endInsertPoint, "</g>").insert(filterInsertPoint, filter).toString();
    }

    private static SVGDocument stringToSVG(String string) {
//        Object o = SVGLoaders.getBatikSVGFromString(string);
        return SVGLoaders.getRenderableFromString(string);
    }
    public static Dimension2D getSVGDimensions(String svgDocAsString) {
        SVGParser parser = new SVGParser(svgDocAsString);
        return parser.getDimensions(svgDocAsString).doubleSize();
//        svgDocAsString = svgDocAsString.toLowerCase();
//        float h, w;
//        int startIdx = svgDocAsString.indexOf("<svg"); // what we want should be in the opening block
//        int limit = svgDocAsString.indexOf(">", startIdx);
//        int hIdx = svgDocAsString.indexOf("height=\"", 0, limit) + 9;
//        int wIdx = svgDocAsString.indexOf("width=\"", 0, limit) + 8;
//        if(hIdx == 8 || wIdx == 7){
//            // use viewBox
//            int vbIdx = svgDocAsString.indexOf("viewbox=\"", startIdx, limit) + 9;
//            if(vbIdx == 9){
//                return null;
//            }
//            String[] values = Strings.split(svgDocAsString.substring(vbIdx, svgDocAsString.indexOf("\"", vbIdx)).replaceAll(",", " "), ' ');
//            return new FloatSize(Float.parseFloat(values[2]), Float.parseFloat(values[3]));
//        }
//        h = Float.parseFloat(svgDocAsString.substring(hIdx, svgDocAsString.indexOf("\"", hIdx)).replaceAll("[a-zA-Z]+", ""));
//        w = Float.parseFloat(svgDocAsString.substring(wIdx, svgDocAsString.indexOf("\"", wIdx)).replaceAll("[a-zA-Z]+", ""));
//        return new DoubleSize(w, h);
    }

    public static String setSVGSize(String svgDocAsString, double width, double height) {
        String workString = svgDocAsString.toLowerCase();
        int startIdx = workString.indexOf("<svg"); // what we want should be in the opening block
        int limit = workString.indexOf(">", startIdx);
        int hIdx = workString.indexOf("height=\"", startIdx, limit) + 8;
        if(hIdx == 7){
            // attribute field(s) not present
            return svgDocAsString;
        }
        svgDocAsString = svgDocAsString.replace(
                svgDocAsString.substring(
                        hIdx,
                        svgDocAsString.indexOf("\"", hIdx)
                ),
                String.valueOf(height));
        int wIdx = workString.indexOf("width=\"", startIdx, limit) + 7;
        svgDocAsString = svgDocAsString.replace(
                svgDocAsString.substring(
                        wIdx,
                        svgDocAsString.indexOf("\"", wIdx)
                ),
                String.valueOf(width));
        return svgDocAsString;
    }

    public static SVGDocument colourSubstitute(String svgDocAsString, List<Color> colorList) {
        return stringToSVG(
        SVGModificationFactory.create(svgDocAsString).replaceColours(colorList).getSvgDocAsString()
        );
//        if (!colorList.isEmpty()) {
//            for (int i = 0; i < Math.max(colorList.size(), 12); i++) {
//                svgDocAsString = svgDocAsString.replaceAll("\"#0+" + i + "\"", String.format("\"#%06x\"", colorList.get(Math.min(colorList.size() - 1, i)).getRGB() & 0x00FFFFFF));
//            }
//        }
//        return stringToSVG(svgDocAsString);
    }
}
