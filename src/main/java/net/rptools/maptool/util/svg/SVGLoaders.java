package net.rptools.maptool.util.svg;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import net.rptools.maptool.client.AppConstants;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.DOMImplementation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * We have 3 ways of getting SVG images; Batik, twelveMonkeys, and JSVG.<br>
 * <p>twelveMonkeys uses Batik to load and parse but only produces BufferedImages. JSVG does its own loading and parsing to produce an object that can be easily rendered and uses its own internal SVG Document. Neither allow access to the SVG Dom tree which prohibits editing. JSVG is faster but does not implement the full SVG feature set.</p>
 * <p>Batik is a proper implementation of SVG so the document contents can be manipulated in line with the specification. Using Batik to do things is the <b>proper</b> way of doing some of the desired operations such as adding filter nodes. It is also slower and something of a pain to work with.</p>
 * <p>This class utilises the JSVG loader and (hopefully) the Batik loader.</p>
 * <p>As something of a hack, the files are read into strings before being loaded. This permits amending the document as a string instead of properly manipulating the Dom tree. Fortunately the main operations such as colour substitution and adding filters can be done a) easily, and b) without too much risk to the document integrity. They are also probably faster.</p>
 */
public class SVGLoaders {
    private static final SVGLoader RENDERABLE_SVG_LOADER = new SVGLoader();
    private static final String PARSER_NAME = XMLResourceDescriptor.getXMLParserClassName();
    private static final SAXSVGDocumentFactory BATIK_LOADER = new SAXSVGDocumentFactory(PARSER_NAME);
    private static final Logger log = LogManager.getLogger(SVGLoaders.class);
    private static final DOMImplementation DOM_IMPLEMENTATION = SVGDOMImplementation.getDOMImplementation();
    private static final String SVG_NS = SVGDOMImplementation.SVG_NAMESPACE_URI;
    private static final String MAPTOOL_URI = AppConstants.MT_THEME_CSS;
    private static final String XLINK_URI =  "http://www.w3.org/1999/xlink";
    public static final Map<String, String> SVG_FILE_STRINGS_CACHE = new HashMap<>();
    public static SVGDocument getRenderableSVG(String resourcePath){
        return getRenderableFromString(getSVGFileAsString(resourcePath));
    }
    public static org.w3c.dom.svg.SVGDocument getBatikSVG(String resourcePath){
        return getBatikSVGFromString(getSVGFileAsString(resourcePath));
    }
    public static @Nullable String getSVGFileAsString(String resourcePath) {
        if(SVG_FILE_STRINGS_CACHE.containsKey(resourcePath)){
            return SVG_FILE_STRINGS_CACHE.get(resourcePath);
        }
        try {
            URI uri = SVGUtil.class.getClassLoader().getResource(resourcePath).toURI();
            Path path = Paths.get(uri);
            String fileString = Files.readString(path, StandardCharsets.UTF_8);
            fileString = fileString.replaceAll("\n", "").replaceAll("\r", "");
            SVG_FILE_STRINGS_CACHE.put(resourcePath, fileString);
            return fileString;
        } catch (NullPointerException | URISyntaxException | IOException e) {
            log.info("Invalid path for getSVGDocument: " + resourcePath);
            return null;
        }
    }
    private static InputStream createInputStream(String stringToStream) {
        return new ByteArrayInputStream(stringToStream.getBytes(StandardCharsets.UTF_8));
    }
    private static SVGDocument getRenderableFromStream(InputStream inputStream) {
        return RENDERABLE_SVG_LOADER.load(inputStream);
    }
    static SVGDocument getRenderableFromString(String svgDocAsString) {
        return getRenderableFromStream(createInputStream(svgDocAsString));
    }
    public static org.w3c.dom.svg.SVGDocument getBatikSVGFromString(String svgDocAsString) {
        return getBatikSVGFromStream(createInputStream(svgDocAsString));
    }
    public static org.w3c.dom.svg.SVGDocument getBatikSVGFromStream(InputStream inputStream) {
        try {
            return BATIK_LOADER.createSVGDocument(MAPTOOL_URI, inputStream);
        } catch (IOException e) {
            log.info("Invalid inputStream or something loading SVGDocument with Batik");
            return null;
        }
    }

}
