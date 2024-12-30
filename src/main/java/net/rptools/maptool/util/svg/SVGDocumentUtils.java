package net.rptools.maptool.util.svg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapted from com.twelvemonkeys.imageio.plugins.svg.SVGImageReader
 */
public class SVGDocumentUtils {
    private static final Logger log = LogManager.getLogger(SVGDocumentUtils.class);
    public static final Map<String, String> SVG_RESOURCE_CACHE = new HashMap<>();
    SVGDocumentUtils() {}
    public com.github.weisj.jsvg.SVGDocument getSVGAsIs(String resourcePath) {
        return SVGLoaders.getRenderableSVG(resourcePath);
    }
    public com.github.weisj.jsvg.SVGDocument getSVGWithFilter(String resourcePath, String filterName) {
        return SVGLoaders.getRenderableFromString(
                SVGModificationFactory.create(SVGLoaders.getSVGFileAsString(resourcePath))
                        .addFilter(filterName)
                        .getSvgDocAsString()
        );
    }
    public com.github.weisj.jsvg.SVGDocument getSVGofSize(String resourcePath, double width, double height) {
        return SVGLoaders.getRenderableFromString(
                SVGModificationFactory.create(SVGLoaders.getSVGFileAsString(resourcePath))
                        .setSize(width, height)
                        .getSvgDocAsString()
        );
    }
    public String getResourceAsString(String resourcePath) {
            if (SVG_RESOURCE_CACHE.containsKey(resourcePath)) {
                return SVG_RESOURCE_CACHE.get(resourcePath);
            } else {
                String docString = SVGLoaders.getSVGFileAsString(resourcePath);
                SVG_RESOURCE_CACHE.put(resourcePath, docString);
                return docString;
            }
    }
}
