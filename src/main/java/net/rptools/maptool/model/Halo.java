package net.rptools.maptool.model;

import net.rptools.lib.MD5Key;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.client.MapToolUtil;
import net.rptools.maptool.client.ui.Halos;
import net.rptools.maptool.server.Mapper;
import net.rptools.maptool.server.proto.halo.HaloDto;
import net.rptools.maptool.util.DrawingUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Halo {
    private Type type = Type.get(AppPreferences.haloType.get());
    private Style style = Style.get(AppPreferences.haloStyle.get());
    private boolean filled = AppPreferences.haloFilled.get();
    private boolean isoFlipped = AppPreferences.haloIsoFlip.get();
    private boolean useFacing = AppPreferences.haloUseFacing.get();
    private float opacity = AppPreferences.haloOverlayOpacity.get() / 255f;
    private MD5Key imageId = null;
    private double scaleFactor = 1.15d;
    private double rotation = 0;
    //TODO: line-weight
    private Halos stockImage = Type.get(AppPreferences.haloType.get()).equals(Type.STOCK_IMAGE) ?
            Halos.get(AppPreferences.haloImage.get()) :
            null;
    private List<Path2D> drawingList = new ArrayList<>();
    private List<String> svgPaths = new ArrayList<>();
    private List<Color> colourList = new ArrayList<Color>(List.of(new Color(AppPreferences.haloColor.get()))){
        public void clean(){
            this.removeIf(Objects::isNull);
            this.trimToSize();
        }
        @Override
        public String toString() {
            this.clean();
            if(isEmpty()){
                return "";
            }
            return String.join(
                    ", ",
                    this.stream().map(color -> String.format("#%06x", color.getRGB() & 0x00FFFFFF)).toList()
            );
        }

        @Override
        public Color getFirst() {
            this.clean();
            if(!isEmpty()) {
                return super.getFirst();
            } else {
                return MapToolUtil.getRandomColor();
            }
        }
    };


    public Halo() {}

    public Halo(Type type, Style style, @Nullable Object filled, @Nullable Object opacity, @Nullable Object scaleFactor, @Nullable Object rotation, @Nullable MD5Key imageId, @Nullable Halos stockImage, @Nullable List<Color> colourList, @Nullable List<Path2D> drawingList, @Nullable List<String> svgPaths) {
        this.type = type;
        this.style = style;
        if(filled != null) {
            this.filled = (boolean) filled;
        }
        if(opacity != null) {
            this.opacity = (float) opacity;
        }
        if(scaleFactor != null) {
            this.scaleFactor = (double) scaleFactor;
        }
        if(rotation != null) {
            this.rotation = (double) rotation;
        }
        this.colourList = colourList;
        this.drawingList = drawingList;
        this.svgPaths = svgPaths;
        this.imageId = imageId;
        this.stockImage = stockImage;
    }

    public Halo(Type type, boolean filled, List<Color> colourList) {
        this.type = type;
        this.filled = filled;
        this.colourList = colourList;
    }

    public Halo(Type type, Style haloStyle, Color color){
        this.type = type;
        this.style = haloStyle;
        this.addColour(color);
    }
    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setStyle(Style style) {
        this.style = style;
    }
    public Style getStyle() {
        return style;
    }

    public List<String> getSvgPaths() {
        return svgPaths;
    }
    public boolean hasSVGPath(){ return !svgPaths.isEmpty(); }



    public void setImageId(MD5Key md5Key) {
        this.imageId = md5Key;
    }
    public MD5Key getImageId() {
        return this.imageId;
    }
    public boolean hasImageId(){
        return imageId != null;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
    public double getRotation() {
        return rotation;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }
    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }
    public boolean isFilled() {
        return filled;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }
    public float getOpacity() {
        return this.opacity;
    }

    public void setIsoFlipped(boolean isoFlipped) {
        this.isoFlipped = isoFlipped;
    }
    public boolean isIsoFlipped() {
        return isoFlipped;
    }

    public void setUseFacing(boolean useFacing) {
        this.useFacing = useFacing;
    }
    public boolean isUseFacing() {
        return useFacing;
    }

    public void setColourList(List<Color> colourList) {
        this.colourList = colourList;
    }
    public List<Color> getColourList() {
        return colourList;
    }
    public void addColour(Color colour) {
        if(this.colourList == null){
            this.colourList = new ArrayList<>();
        }
        if(colour != null) {
            this.colourList.add(colour);
        }
    }
    public void removeColour(Color colour) {
        if(this.colourList == null){
            return;
        }
        this.colourList.remove(colour);
    }
    public Color getColour() {
        Color c = null;
        if(this.colourList != null){
            if(!getColourList().isEmpty()) {
                c = getColourList().getFirst();
            }
        }
        return c;
    }

    public void setColour(Color colour){
        if(this.colourList == null){
            this.colourList = new ArrayList<>();
        } else {
            getColourList().clear();
        }
        if(colour != null) {
            getColourList().add(colour);
        }
    }

    public void setDrawingList(List<Path2D> drawingList) {
        this.drawingList = drawingList;
    }
    public List<Path2D> getDrawingList() {
        return drawingList;
    }
    public boolean hasDrawing(){
        return !drawingList.isEmpty();
    }
    public void setDrawing(Path2D drawing) {
        if(this.drawingList == null){
            this.drawingList = new ArrayList<>();
        }
        this.drawingList.add(drawing);
    }
    public void addDrawing(String svgPath) {
        if(this.svgPaths == null){
            this.svgPaths = new ArrayList<>();
        }
        this.svgPaths.add(svgPath);
        this.drawingList.add(DrawingUtil.svgToPath2D(svgPath));
    }
    public void setDrawing(String svgPath) {
        if(this.svgPaths == null){
            this.svgPaths = new ArrayList<>();
        }
        this.svgPaths.clear();
        this.drawingList.clear();
        this.svgPaths.add(svgPath);
        this.drawingList.add(DrawingUtil.svgToPath2D(svgPath));
    }
    public Path2D getDrawing() {
        if (!drawingList.isEmpty()) {
            return drawingList.getFirst();
        }
        return null;
    }



    public void setStockImage(@Nullable Halos stockImage) {
        this.stockImage = stockImage;
    }
    public Halos getStockImage() {
        return stockImage;
    }
    public boolean hasStockImage(){
        return stockImage != null;
    }
    public Halos getStockImageOrDefault() {
        return stockImage == null ? Halos.getDefault() : stockImage;
    }




    @Override
    public int hashCode() {
        return Objects.hash(getType(), getStyle(), isFilled(), getOpacity(), getRotation(), getColourList(), getDrawingList(), getSvgPaths(), getImageId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Halo)) return false;
        return o.hashCode() == this.hashCode();
    }

    @Override
    public String toString() {
        return "Halo{" +
                "type=" + type +
                ", style=" + style +
                ", filled=" + filled +
                ", opacity=" + opacity +
                ", colourList=" + colourList.toString() +
                ", drawingList=" + drawingList +
                ", svgPaths=" + svgPaths +
                ", imageId=" + imageId +
                ", scaleFactor=" + scaleFactor +
                ", rotation=" + rotation +
                ", stockImage=" + stockImage +
                '}';
    }

    public enum Type {
        CELL,
        CIRCLE,
        FOOTPRINT_CONVEX,
        DRAWING,
        FOOTPRINT,
        FOOTPRINT_BOUNDS,
        IMAGE,
        ROUNDED_FOOTPRINT_BOUNDS,
        ROUNDED_RECTANGLE,
        RECTANGLE,
        SNOWFLAKE,
        STOCK_IMAGE;
        public static int getDefaultIndex(){
            return CELL.ordinal();
        }
        public static Type get(int index){
            return values()[index];
        }
        public static Type get(String string){
            return Arrays.stream(values()).filter(value ->
                    value.name().equals(string.toUpperCase())).toList().getFirst();
        }
    }

    public enum Style {
        GLOW,
        LINE,
        TUBE;
        public static int getDefaultIndex(){
            return LINE.ordinal();
        }
        public static Style get(int index){
            return values()[index];
        }
        public static Style get(String string){
            return Arrays.stream(values()).filter(value ->
                    value.name().equals(string.toUpperCase())).toList().getFirst();
        }
    }



    public static Halo fromDto(HaloDto haloDto) {
        Halo h = new Halo();
        h.setStyle(Style.values()[haloDto.getStyle()]);
        h.setType(Type.values()[haloDto.getType()]);
        h.setStockImage(Halos.get(haloDto.getStockImage()));
        h.setFilled(haloDto.getFilled());
        h.setIsoFlipped(haloDto.getIsoFlipped());
        h.setUseFacing(haloDto.getUseFacing());
        h.setScaleFactor(haloDto.getScaleFactor());
        h.setRotation(haloDto.getRotation());
        h.setImageId(new MD5Key(haloDto.getMd5Key()));
        h.setColourList(haloDto.getColorListList().stream().map(Color::new).collect(Collectors.toList()));
        h.svgPaths = haloDto.getSvgPathsList();
        h.setDrawingList(haloDto.getPathListList().stream()
                .map(Mapper::map).collect(Collectors.toList()));
        return h;
    }
    public HaloDto toDto() {
        var dto = HaloDto.newBuilder();
        dto.setType(getType().ordinal());
        dto.setStyle(getStyle().ordinal());
        if(hasStockImage()){
            dto.setStockImage(getStockImage().ordinal());
        }
        dto.setFilled(isFilled());
        dto.setIsoFlipped(isIsoFlipped());
        dto.setUseFacing(isUseFacing());
        dto.setScaleFactor(getScaleFactor());
        dto.setRotation(getRotation());
        if(getImageId() != null) {
            dto.setMd5Key(getImageId().toString());
        }
        if(!getColourList().isEmpty()) {
            dto.addAllColorList(getColourList().stream().map(Color::getRGB).collect(Collectors.toList()));
        }
        if(!getSvgPaths().isEmpty()) {
            dto.addAllSvgPaths(getSvgPaths());
        }
        if(!getDrawingList().isEmpty()) {
            dto.addAllPathList(getDrawingList().stream().map(Mapper::map).collect(Collectors.toList()));
        }
        return dto.build();
    }
}
