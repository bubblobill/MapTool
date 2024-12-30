/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.client.ui.zone.renderer;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.util.ColorUtil;
import com.twelvemonkeys.image.ImageUtil;
import net.rptools.lib.CodeTimer;
import net.rptools.lib.EasingUtil;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.model.Grid;
import net.rptools.maptool.model.Halo;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.model.Zone;
import net.rptools.maptool.util.svg.SVGUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class HaloRenderer {
    private static final Logger log = LogManager.getLogger(HaloRenderer.class);
    private static final Color TRANSPARENT = new Color(1f, 1f, 1f, 0f);
    private static final Halo.Type[] TYPES = Halo.Type.values();
    private static final float[] opacities = new float[]{
            0.27f, 0.27f, 0f, 1f, 0.5f, 0.07f, 0.01f, 0f
    };
    private static final float[] radii = new float[]{
            0f, 0.6f, 0.64f, 0.7f, 0.8f, 0.9f, 0.95f, 1f
    };
    private static final Halo DEFAULT_HALO = new Halo();
    private static final int DRAW_TIMES = 12;
    private static final Map<String, Map<List<Color>, SVGDocument>> svgDocCache = new HashMap<>();
    private static final Map<Integer, float[]> COS_CACHE = new HashMap<>();
    private static final Map<Integer, float[]> SIN_CACHE = new HashMap<>();
    static int count = 0;
    private static float[] flArr = new float[]{
            0.27781263f, 0.120835948f, 0.15626961f, 0.11720221f, 0.08790165f, 0.06592624f, 0.025f, 0.015f, 0.01f
    };
    final float lineW = AppPreferences.haloLineWidth.get();
    private final CodeTimer timer;
    private final HaloStore haloStore = new HaloStore();
    private final Map<Color, Color[]> colourCache = new HashMap<>();
    private final float HALO_WIDTH = AppPreferences.haloLineWidth.get();
    private final float MAX_MAX_WIDTH = 12f;
    private final float MIN_MAX_WIDTH = 4.5f;
    private Graphics2D g2d;
    Shape haloShape;
    Image haloImage;
    String haloSVGString;
    Halo.Type type;
    private AffineTransform svgTransform;
    private AffineTransform isoTransform;
    private AffineTransform spinTransform;
    private Halo halo;
    private ZoneRenderer renderer;
    private Grid grid;
    private Token token;
    private TokenLocation location;
    private boolean isoFigure = false;
    private boolean debug = true;
    private boolean initialised = false;
    private float[] widthArr;
    private static float lineWeight = AppPreferences.haloLineWeight.get();
    HaloRenderer() {
        this.timer = CodeTimer.get();

        float[] cos = new float[DRAW_TIMES + 1];
        float[] sin = new float[DRAW_TIMES + 1];
        float total = 0;
        for (int i = 0; i < DRAW_TIMES + 1; i++) {
            sin[i] = (float) EasingUtil.easeInSin((double) i * 1 / DRAW_TIMES + 1) - 1;
            cos[i] = (float) EasingUtil.easeOutSin((double) i * 1 / DRAW_TIMES + 1);
            total += cos[i];
        }
        for (int i = 0; i < cos.length; i++) {
            cos[i] /= total;
        }
        widthArr = Arrays.copyOfRange(sin, 1, sin.length);
        flArr = Arrays.copyOfRange(cos, 0, cos.length - 1);
    }

    public static boolean isDouble(Object o) {
        return o.getClass().isAssignableFrom(Double.class);
    }

    public static boolean isFloat(Object o) {
        return o.getClass().isAssignableFrom(Float.class);
    }

    public static boolean isInt(Object o) {
        return o.getClass().isAssignableFrom(Integer.class);
    }

    public static boolean isNumber(Object o) {
        return o.getClass().isAssignableFrom(Number.class);
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void gridChanged(ZoneRenderer zoneRenderer) {
        setRenderer(zoneRenderer);
    }

    public void setRenderer(ZoneRenderer zoneRenderer) {
        renderer = zoneRenderer;
        Zone zone = renderer.getZone();
        grid = zone.getGrid();
        haloStore.setGrid(grid);
        initialised = true;
        log.info(this.getClass().getSimpleName() + " initialised");
    }

    /**
     * Paints halo at token location
     * @param graphics2D Graphics object source
     * @param location TokenLocation object
     * @param debug draw debug shapes
     */
    public void renderHalo(Graphics2D graphics2D, TokenLocation location, boolean debug) {
        this.debug = true;
        this.location = location;
        this.token = location.token;
        if (!token.hasHalo() && !debug) {
            return;
        }

        halo = token.getHalo();
        // validate halo exists or use default
        if (halo == null) {
            if (debug) {
                halo = new Halo(TYPES[count], Halo.Style.TUBE, token.getHaloColor());
                if (halo.getType().equals(Halo.Type.DRAWING)) {
                    try {
                        halo.setDrawing("m14 0.047 -0.73 2.4c-1.1 0.14 -2.1 0.43 -3.2 0.85l-1.8 -1.7 -1.6 0.95 0.56 2.4c-0.87 0.67 -1.6 1.4 -2.3 2.3l-2.4 -0.56 -0.95 1.6 1.7 1.8c-0.42 1 -0.7 2.1 -0.85 3.2l-2.4 0.73v1.9l2.4 0.73c0.14 1.1 0.43 2.1 0.85 3.2l-1.7 1.8 0.95 1.6 2.4 -0.56c0.67 0.87 1.4 1.6 2.3 2.3l-0.56 2.4 1.6 0.95 1.8 -1.7c1 0.42 2.1 0.7 3.2 0.85l0.73 2.4h1.9l0.73 -2.4c1.1 -0.14 2.1 -0.43 3.2 -0.85l1.8 1.7 1.6 -0.95 -0.56 -2.4c0.87 -0.67 1.6 -1.4 2.3 -2.3l2.4 0.56 0.95 -1.6 -1.7 -1.8c0.42 -1 0.7 -2.1 0.85 -3.2l2.4 -0.73v-1.9l-2.3 -0.73c-0.21 -1.1 -0.5 -2.2 -0.86 -3.2l1.6 -1.8 -0.95 -1.6 -2.4 0.56c-0.67 -0.87 -1.4 -1.6 -2.3 -2.4l0.53 -2.4 -1.6 -0.95 -1.8 1.6c-1 -0.36 -2.1 -0.64 -3.2 -0.86l-0.73 -2.3z");

                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }
                }
            } else {
                DEFAULT_HALO.setColourList(List.of(token.getHaloColor()));
                halo = DEFAULT_HALO;
            }
        }
        isoFigure = token.getShape() == Token.TokenShape.FIGURE && !token.isFlippedIso();

        timer.start("HaloRenderer - renderHalo");

        /*
            Halo object - determine type
         */
        haloShape = null;
        haloSVGString = null;
        haloImage = null;
        Object haloObject = haloStore.getHaloObject(halo, location);

        type = halo.getType();
        try {
            if (type.equals(Halo.Type.IMAGE)) {
                haloImage = (BufferedImage) haloObject;
            } else if (type.equals(Halo.Type.STOCK_IMAGE)) {
                if (haloObject.getClass().isAssignableFrom(String.class)) {
                    haloSVGString = (String) haloObject;
//                    haloSVGString = SVGUtil.addFilter(haloSVGString);
                } else {
                    haloImage = (BufferedImage) haloObject;
                }
            } else {
                haloShape = (Shape) haloObject;
                haloShape = AffineTransform.getTranslateInstance(
                                haloShape.getBounds2D().getCenterX() + token.getAnchorX(),
                                haloShape.getBounds2D().getCenterY() + token.getAnchorY())
                        .createTransformedShape(haloShape);
            }
        } catch (ClassCastException cce) {
            //invalid renderable thing
            log.info("invalid object. \n" + cce.getLocalizedMessage() + "\n", cce + "\n" + cce.getCause());
            timer.stop("HaloRenderer - renderHalo");
            return;
        }

        /*
            Set up graphics
         */
        g2d = graphics2D;
        RenderUtils.GraphicsState.store(g2d);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        RenderUtils.orientGraphicsOnTokenImage(g2d, location);
        g2d.scale(renderer.getScale(), renderer.getScale());

        if (halo.getOpacity() < 1f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, halo.getOpacity()));
        }

        /*
            Transforms
         */
        double theta = halo.getRotation();
        theta += halo.isUseFacing() && token.hasFacing() ? token.getFacing() : 0;
        if(theta != 0) {
            spinTransform = AffineTransform.getRotateInstance(theta);
        } else {
            spinTransform = null;
        }

        if(grid.isIsometric()) {
            if (isoFigure && !halo.isIsoFlipped()) {
                isoTransform = AffineTransform.getTranslateInstance(0, token.getHeight() / 4d);
            } else if (halo.isIsoFlipped()) {
                isoTransform = new AffineTransform();
                isoTransform.rotate(Math.PI / 4d);
                isoTransform.scale(1d, 0.5);
            }
        } else {
            isoTransform = null;
        }
        scaleObject();

        if (haloSVGString != null) {
            paintSVG(g2d, location, halo);
        } else if (haloImage != null) {
            paintImageHalo(g2d, location, halo);
        } else {
            haloShape = new Path2D.Double((Shape) haloObject);
            haloShape = AffineTransform.getTranslateInstance(
                    haloShape.getBounds2D().getCenterX(),
                    haloShape.getBounds2D().getCenterY()
            ).createTransformedShape(haloShape);

            if (halo.getStyle().equals(Halo.Style.LINE)) {
                paintLineHalo(g2d, location, haloShape, halo);
            }
            if (halo.getStyle().equals(Halo.Style.GLOW)) {
                paintGlowHalo(g2d, location, haloShape, halo);
            }
            if (halo.getStyle().equals(Halo.Style.TUBE)) {
                paintTubeHalo(g2d, location, haloShape, halo);
            }
        }

        RenderUtils.GraphicsState.restore(g2d);
        timer.stop("HaloRenderer - renderHalo");

        count++;
        count = count == TYPES.length ? 0 : count;
    }

    private void scaleObject() {
        double oW, oH, scale;
        if (type.equals(Halo.Type.IMAGE) || (type.equals(Halo.Type.STOCK_IMAGE) && haloImage != null)) {
            oW = com.twelvemonkeys.image.ImageUtil.getWidth(haloImage);
            oH = com.twelvemonkeys.image.ImageUtil.getHeight(haloImage);
        } else if (type.equals(Halo.Type.STOCK_IMAGE) && haloSVGString != null) {
            Dimension2D floatSize = SVGUtil.getSVGDimensions(haloSVGString);
            assert floatSize != null;
            oW = floatSize.getWidth(); //haloSVGString.computeShape().getBounds2D().getWidth();
            oH = floatSize.getHeight(); //haloSVGString.computeShape().getBounds2D().getHeight();
        } else {
            oW = haloShape.getBounds2D().getWidth();
            oH = haloShape.getBounds2D().getHeight();
        }

        // scale to fit token space
        scale = Math.max(location.scaledWidth / oW, location.scaledHeight / oH);
        //scale to halo scale value
        scale *= halo.getScaleFactor();
        if (type.equals(Halo.Type.IMAGE) || (type.equals(Halo.Type.STOCK_IMAGE) && haloImage != null)) {
            haloImage = com.twelvemonkeys.image.ImageUtil.createScaled(haloImage, (int) Math.ceil(oW * scale), (int) Math.ceil(oH * scale), Image.SCALE_SMOOTH);
        } else if (type.equals(Halo.Type.STOCK_IMAGE) && haloSVGString != null) {
            haloSVGString = SVGUtil.setSVGSize(haloSVGString, oW * scale, oH * scale);
            svgTransform = AffineTransform.getTranslateInstance(-oW * scale / 2d, -oH * scale / 2d);
        } else {
            haloShape = AffineTransform.getScaleInstance(scale, scale).createTransformedShape(haloShape);
        }
    }

    private void paintSVG(Graphics2D haloG, TokenLocation location, Halo halo) {
        if (svgTransform != null) {
            haloG.transform(svgTransform);
        }
//        SVGDocument haloSVG = null;
        SVGDocument haloSVG = SVGUtil.colourSubstitute(haloSVGString, halo.getColourList());
//        if (svgDocCache.containsKey(haloSVGString)) {
//            if (svgDocCache.get(haloSVGString).containsKey(halo.getColourList())) {
//                haloSVG = svgDocCache.get(haloSVGString).get(halo.getColourList());
//            }
//        }
//        if (haloSVG == null) {
//            haloSVG = SVGUtil.colourSubstitute(haloSVGString, halo.getColourList());
//            svgDocCache.put(haloSVGString, Map.of(halo.getColourList(), haloSVG));
//        }

        haloSVG.render(renderer, haloG);

    }

    private void paintImageHalo(Graphics2D haloG, TokenLocation location, Halo halo) {
//        log.info(this.getClass().getSimpleName() + " paintImageHalo");
        // scale image relative to bounds and apply halo image scale
        double maxScale = Math.max(location.scaledWidth / ImageUtil.getWidth(haloImage), location.scaledHeight / ImageUtil.getHeight(haloImage));
        AffineTransform imageTransform = AffineTransform.getScaleInstance(maxScale * halo.getScaleFactor(), maxScale * halo.getScaleFactor());

        // centre image
        double imageCx = -ImageUtil.getWidth(haloImage) / 2d;
        double imageCy = -ImageUtil.getHeight(haloImage) / (isoFigure ? 4d / 3d : 2d);
        imageTransform.concatenate(
                AffineTransform.getTranslateInstance(
                        imageCx + token.getAnchorX() * renderer.getScale(), imageCy + token.getAnchorY() * renderer.getScale())
        );

        if (isoTransform != null) {
            imageTransform.concatenate(isoTransform);
        }
        haloG.drawImage(haloImage, imageTransform, renderer);
    }

    private void paintLineHalo(Graphics2D haloG, TokenLocation location, Shape haloShape, Halo halo) {
        haloG.setStroke(
                new BasicStroke((float) Math.clamp(lineW * renderer.getScale(), 0.6, 10))
        );
        haloG.setColor(halo.getColourList().getFirst());
        haloG.draw(haloShape);
    }

    private void paintGlowHalo(Graphics2D haloG, TokenLocation location, Shape haloShape, Halo halo) {
        float startWidth = (float) Math.min(HALO_WIDTH * renderer.getScale(), 4d);
        RenderUtils.DynamicStroke stroke = new RenderUtils.DynamicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        Color c = location.token.getHaloColor();
        if (c == null) {
            c = new Color(net.rptools.lib.image.ImageUtil.negativeColourInt(renderer.getZone().getGridColor()));
        }
//        Shape oldClip = haloG.getClip();
//        Area a = new Area(oldClip);
//        Shape inside = AffineTransform.getScaleInstance(0.94, 0.94).createTransformedShape(haloShape);
//        inside = AffineTransform.getTranslateInstance(inside.getBounds2D().getCenterX(), inside.getBounds2D().getCenterY()).createTransformedShape(inside);
//        a.add(new Area(inside));
//        haloG.setClip(a);
        Color lineColour;
        Color fillColour = TRANSPARENT;
        float opacity = 1f;
        float width;
        for (int i = 0; i < flArr.length; i++) {
            opacity -= flArr[i];
            width = (float) mapToRange(widthArr[i], 0, 1, startWidth, startWidth * 3);
            lineColour = ColorUtil.withAlpha(c, opacity);
            haloG.setColor(lineColour);

            stroke.setWidth(width);
            haloG.setStroke(stroke);
            if (halo.isFilled() && i == 0) {
                fillColour = lineColour;
            }
            haloG.draw(haloShape);
        }
//        haloG.setClip(oldClip);
//        if (halo.isFilled()) {
//            haloG.setColor(fillColour);
//            haloG.fill(inside);
//        }
    }

    private void paintTubeHalo(Graphics2D haloG, TokenLocation location, Shape haloShape, Halo halo) {
//        log.info("paintTubeHalo " + location.token.getName());
        List<Color> colours = new ArrayList<>();
        if (halo.getColourList().size() == DRAW_TIMES) {
            colours.addAll(halo.getColourList());
        } else {
            Color hc = location.token.getHaloColor();
            if (hc == null) {
                hc = new Color(AppPreferences.haloColor.getDefault());
            }
            colours = getTubeColours(hc);
            halo.setColourList(colours);
            HaloStore.haloCache.put(halo.hashCode(), halo);
        }

        float startWidth = Math.clamp(HALO_WIDTH * (float) renderer.getScale(), MIN_MAX_WIDTH, MIN_MAX_WIDTH);
        double specularOffset = startWidth / 3d;
        Shape shadowShape = AffineTransform.getTranslateInstance(specularOffset, specularOffset).createTransformedShape(haloShape);
        Shape highlightShape = AffineTransform.getTranslateInstance(-specularOffset, -specularOffset).createTransformedShape(haloShape);
        BasicStroke stroke = new BasicStroke(startWidth);

        haloG.setColor(Color.BLUE);
        haloG.draw(haloShape);
        haloG.draw(location.boundsCache);
        float[] cosFactors = COS_CACHE.get(DRAW_TIMES);
        for (int i = 0; i < DRAW_TIMES; i++) {
            if (i > 0) {
                stroke = new BasicStroke(startWidth * cosFactors[i]);
            }
            haloG.setStroke(stroke);
            haloG.setColor(colours.get(i));
            if (i <= DRAW_TIMES / 3) {
                haloG.draw(shadowShape);
            } else if (i <= 2 * DRAW_TIMES / 3) {
                haloG.draw(haloShape);
            } else {
                haloG.draw(highlightShape);
            }
        }
    }

    public static double mapToRange(double map_value, double in_min, double in_max, double out_min, double outmax) {
        if (Math.abs(in_max - in_min) < 1e-12) {
            throw new ArithmeticException("/ 0");
        }
        double ratio = (outmax - out_min) / (in_max - in_min);
        return ratio * (map_value - in_min) + out_min;
    }

    private List<Color> getTubeColours(Color c) {
        float[] sinFactors = new float[DRAW_TIMES];
        if (SIN_CACHE.containsKey(DRAW_TIMES)) {
            sinFactors = COS_CACHE.get(DRAW_TIMES);
        } else {
            double increment = Math.PI / (DRAW_TIMES * 2d);
            float[] cosFactors = new float[DRAW_TIMES];
            for (int i = 0; i < DRAW_TIMES; i++) {
                sinFactors[i] = (float) Math.sin(increment * i);
                sinFactors[i] = (float) Math.cos(increment * i);
            }
            SIN_CACHE.put(DRAW_TIMES, sinFactors);
            COS_CACHE.put(DRAW_TIMES, cosFactors);
        }
        Color[] colours = new Color[DRAW_TIMES];
        float[] HSB = new float[3];
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), HSB);
        float adjustment = (1f - HSB[2]) * 0.7f;
        final int SPECULAR_MARK = (int) (DRAW_TIMES * 0.66);
        for (int i = 0; i < DRAW_TIMES; i++) {
            Color nc = new Color(Color.HSBtoRGB(HSB[0], HSB[1], HSB[2] * sinFactors[i] + adjustment));
            if (i == DRAW_TIMES - 1) {
                colours[SPECULAR_MARK] = nc;
            } else if (i >= SPECULAR_MARK) {
                colours[i + 1] = nc;
            } else {
                colours[i] = nc;
            }
        }
        return Arrays.stream(colours).toList();
    }

    private Color[] getGlowColours(Color c) {
        if (colourCache.containsKey(c)) {
            return colourCache.get(c);
        } else {
            Color[] arr = new Color[opacities.length];
            for (int i = 0; i < opacities.length; i++) {
                if (opacities[i] == 0f) {
                    arr[i] = TRANSPARENT;
                } else {
                    arr[i] = ImageUtil.createTranslucent(c, (int) (opacities[i] * 255));
                }
            }
            colourCache.put(c, arr);
            log.info(Arrays.deepToString(arr));
            return arr;
        }
    }

    public float constrainFloat(float value, float lowBound, float highBound) {
        return Math.max(lowBound, Math.min(highBound, value));
    }
}

