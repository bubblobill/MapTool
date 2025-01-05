package net.rptools.maptool.client.ui.zone.renderer.halo;

import net.rptools.lib.CodeTimer;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.client.ui.theme.RessourceManager;
import net.rptools.maptool.client.ui.zone.renderer.TokenLocation;
import net.rptools.maptool.model.*;
import net.rptools.maptool.util.GraphicsUtil;
import net.rptools.maptool.util.ImageManager;
import net.rptools.maptool.util.svg.SVGDocumentUtils;
import net.rptools.maptool.util.svg.SVGUtil;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HaloStore {
    private static final Logger log = LogManager.getLogger(HaloStore.class);
    private static final CellPoint ORIGIN = new CellPoint(0, 0);
    private static final int DEFAULT_SIZE = AppPreferences.defaultGridSize.get();
    private static final double ROOT2 = Math.sqrt(2d);
    private static final double ROOT3 = Math.sqrt(3d);
    public static final Map<Integer, Object> haloCache = new HashMap<>();
    public static final Map<String, Shape> gridShapesMap = new HashMap<>();
    private final CodeTimer timer;
    int gridSize = DEFAULT_SIZE;
    boolean isIso = false;
    boolean isHex = false;
    boolean vert = false;
    String[] gridTypes = new String[]{GridFactory.HEX_HORI, GridFactory.HEX_VERT, GridFactory.ISOMETRIC, GridFactory.NONE, GridFactory.SQUARE};
    private String currentGridType = GridFactory.NONE;
    private Grid grid;

    HaloStore() {
        this.timer = CodeTimer.get();
        RessourceManager.clearHaloCache();
        for (String gridType : gridTypes) {
            Shape inside = new Area(GraphicsUtil.createGridShape(gridType, DEFAULT_SIZE + 0.4));
            Shape outside = new Area(GraphicsUtil.createGridShape(gridType, DEFAULT_SIZE + DEFAULT_SIZE / 12d));
            gridShapesMap.put(gridType+"_outside", outside);
            gridShapesMap.put(gridType+"_inside", inside);
        }
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
        currentGridType = GridFactory.getGridType(grid);
        gridSize = grid.getSize();
        isIso = grid.isIsometric();
        isHex = grid.isHex();
        vert = currentGridType.equals(GridFactory.HEX_VERT);
    }
    public String getCurrentGridType() {
        return currentGridType;
    }
    public Object getHaloObject(Halo halo, TokenLocation location) {
        timer.start("HaloStore - getHaloObject");
        int hashCode = halo.hashCode();
        if (haloCache.containsKey(hashCode)) {
            timer.stop("HaloStore - getHaloObject");
            return haloCache.get(hashCode);
        }
        Object haloObject = createHaloObject(halo, location);
        if(haloObject == null) {
            log.info("Using fallback halo object. haloObject == null for creating " + halo.getType());
            if (halo.getType() == Halo.Type.IMAGE) {
                haloObject = ImageManager.TRANSFERING_IMAGE;
            } else {
                haloObject = gridShapesMap.get(getCurrentGridType()+"_outside");
            }
        } else {
            haloCache.put(hashCode, haloObject);
        }
        timer.stop("HaloStore - getHaloObject");
        return haloObject;
    }

    private Object createHaloObject(Halo halo, TokenLocation location) {
        Object haloObject = null;
        double size = gridSize;
        boolean transformIso = false;

        switch (halo.getType()) {
            case CELL -> haloObject = gridShapesMap.get(getCurrentGridType() + "_outside");
            case CIRCLE -> {
                double rH = size * ROOT2 * 0.8;
                double rV = isIso ? rH / 2d : rH;
                haloObject = new Ellipse2D.Double(-rH / 2d, -rV / 2d, rH, rV);
            }
            case DRAWING -> haloObject = halo.getDrawing();
            case FOOTPRINT ->  haloObject = createFootprintShapes(location.token.getFootprint(grid));
            case FOOTPRINT_BOUNDS -> haloObject = createFootprintShapes(location.token.getFootprint(grid)).getBounds2D();
            case FOOTPRINT_CONVEX -> haloObject = GraphicsUtil.convexHull(createFootprintShapes(location.token.getFootprint(grid)));
            case ROUNDED_FOOTPRINT_BOUNDS -> {
                            Rectangle2D bounds = createFootprintShapes(location.token.getFootprint(grid)).getBounds2D();
                            haloObject = new RoundRectangle2D.Double(
                                    -bounds.getCenterX(), -bounds.getCenterY(), bounds.getWidth(), bounds.getHeight(), gridSize / 12d, gridSize / 12d);
            }
            case RECTANGLE -> {
                double rH = size * ROOT2 * 0.8;
                haloObject = new Rectangle2D.Double(-rH / 2d, -rH / 2d, rH, rH);
                transformIso = true;
            }
            case ROUNDED_RECTANGLE -> {
                double rH = size * 0.8 * (vert ? ROOT3 : ROOT2);
                double rV = size * 0.8 * (isHex && !vert ? ROOT3 : ROOT2);
                haloObject = new RoundRectangle2D.Double(-rH / 2d, -rV / 2d, rH, rV, rH - size / 2d, rV - size / 2d);
                transformIso = true;
            }
            case SNOWFLAKE -> {
                haloObject = GraphicsUtil.snowflake();
                transformIso = true;
            }
            case STOCK_IMAGE -> {
                SVGDocumentUtils.LoadRecord loadRecord = RessourceManager.getHalo(halo.getStockImageOrDefault());
                if(loadRecord.image() == null && loadRecord.batikSVG() != null){
                    haloObject = Objects.requireNonNull( loadRecord.batikSVG());
                    if(!halo.getSvgFilters().isEmpty()){
                        haloObject = SVGUtil.addFilters(haloObject, halo.getSvgFilters());
                    }
                    haloObject = SVGUtil.colourSubstitute((SVGOMDocument) haloObject, halo.getColourList());
                } else {
                    haloObject = loadRecord.image();
                }
            }
            case IMAGE -> haloObject = ImageManager.getImage(halo.getImageId());
            default -> log.info("Unhandled HaloType in createHalo: " + halo.getType());
        }

        if (isIso && transformIso) {
            haloObject = AffineTransform.getRotateInstance(Math.TAU / 8).createTransformedShape((Shape) haloObject);
            haloObject = AffineTransform.getScaleInstance(1, 0.5).createTransformedShape((Shape) haloObject);
        }
//        log.info(halo.toString());
        return haloObject;
    }

    private Shape createFootprintShapes(TokenFootprint fp) {
        Shape outsideShape = gridShapesMap.get(getCurrentGridType()+"_outside");
        Shape insideShape = gridShapesMap.get(getCurrentGridType()+"_inside");
        Shape outline;
        Grid grid = GridFactory.createGrid(getCurrentGridType());
        if (fp.getScale() < 1 || getCurrentGridType().equals(GridFactory.NONE)) {
            outsideShape = AffineTransform.getScaleInstance(fp.getScale(), fp.getScale()).createTransformedShape(outsideShape);
            Area a = new Area(outsideShape);
            a.subtract(new Area(AffineTransform.getScaleInstance(fp.getScale(), fp.getScale()).createTransformedShape(insideShape)));
        } else {
            Area outlineArea = new Area();
            Area outsideArea = new Area(outsideShape);
            Area insideArea = new Area(insideShape);
            fp.getOccupiedCells(ORIGIN).forEach(cp -> {
                ZonePoint zp = grid.convert(cp);
                outlineArea.add(outsideArea.createTransformedArea(AffineTransform.getTranslateInstance(zp.x, zp.y)));
                insideArea.add(insideArea.createTransformedArea(AffineTransform.getTranslateInstance(zp.x, zp.y)));
            });
            outlineArea.subtract(insideArea);
            outline = outlineArea.createTransformedArea(AffineTransform.getTranslateInstance(-outlineArea.getBounds2D().getCenterX(), -outlineArea.getBounds2D().getCenterY()));
            Rectangle2D outlineBounds2D = outline.getBounds2D();
            Rectangle2D shapeBounds2D = outsideShape.getBounds2D();
            double proportion = Math.min(outlineBounds2D.getWidth(), outlineBounds2D.getHeight()) / Math.min(shapeBounds2D.getWidth(), shapeBounds2D.getHeight());
            outsideShape = AffineTransform.getScaleInstance(proportion, proportion).createTransformedShape(outsideShape);
        }
        return outsideShape;
    }
}
