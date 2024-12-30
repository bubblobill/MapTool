package net.rptools.maptool.client.ui;

import net.rptools.maptool.client.AppPreferences;

import java.util.Arrays;

public enum Halos {
    COG,
    CURLICUE,
    FIRECIRCLE,
    GRADUATED_HEX,
    GRADUATED_ISO,
    GRADUATED_SQUARE,
    GREG,
    HEXACIRCLE,
    HYPNOSWIRL,
    MAGIC_CIRCLE,
    RINGSTACK,
    SPIKE,
    SPIROGRAPH,
    THIS_WAY,
    TREFOLIAGE;
    public static int getDefaultIndex(){
        return COG.ordinal();
    }
    public static Halos getDefault(){
        return get(AppPreferences.haloImage.get());
    }
    public static Halos get(int index){
        return values()[index];
    }
    public static Halos get(String string){
        return Arrays.stream(values()).filter(value ->
            value.name().equals(string.toUpperCase())).toList().getFirst();
    }

}
