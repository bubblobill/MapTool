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
package net.rptools.maptool.client;

import net.rptools.maptool.client.utilities.RandomSuffixFactory;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.Asset;
import net.rptools.maptool.model.AssetManager;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.model.Zone;
import net.rptools.maptool.model.drawing.DrawablePaint;
import net.rptools.maptool.model.drawing.DrawableTexturePaint;
import net.rptools.maptool.util.StringUtil;

import java.awt.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapToolUtil {
  private static final Random RAND = new SecureRandom();

  private static RandomSuffixFactory randomSuffixFactory = new RandomSuffixFactory();
  private static AtomicInteger nextTokenId = new AtomicInteger(1);

  /** The map of color names to color values */
  private static final Map<String, Color> COLOR_MAP = new TreeMap<String, Color>();
  private static final Map<String, Color> COLOR_MAP_HTML5 = new TreeMap<String, Color>(String.CASE_INSENSITIVE_ORDER);

  private static final Map<String, Color> COLOR_MAP_HTML = new HashMap<String, Color>();

  /** Set up the color map */
  static {
    // Built-in Java colors that happen to match the values used by HTML...
    COLOR_MAP.put("black", Color.BLACK);
    COLOR_MAP.put("blue", Color.BLUE);
    COLOR_MAP.put("cyan", Color.CYAN);
    COLOR_MAP.put("gray", Color.GRAY);
    COLOR_MAP.put("magenta", Color.MAGENTA);
    COLOR_MAP.put("red", Color.RED);
    COLOR_MAP.put("white", Color.WHITE);
    COLOR_MAP.put("yellow", Color.YELLOW);

    // The built-in Java colors that DO NOT match the HTML colors...
    COLOR_MAP.put("darkgray", new Color(0xA9, 0xA9, 0xA9)); // Color.DARK_GRAY
    COLOR_MAP.put("green", new Color(0x00, 0x80, 0x00)); // Color.GREEN
    COLOR_MAP.put("lightgray", new Color(0xD3, 0xD3, 0xD3)); // Color.LIGHT_GRAY
    COLOR_MAP.put("orange", new Color(0xFF, 0xA5, 0x00)); // Color.ORANGE
    COLOR_MAP.put("pink", new Color(0xFF, 0xC0, 0xCB)); // Color.PINK

    // And the HTML colors that don't exist at all as built-in Java values...
    COLOR_MAP.put("aqua", new Color(0x00, 0xFF, 0xFF)); // same as Color.CYAN
    COLOR_MAP.put("fuchsia", new Color(0xFF, 0x00, 0xFF)); // same as Color.MAGENTA
    COLOR_MAP.put("lime", new Color(0xBF, 0xFF, 0x00));
    COLOR_MAP.put("maroon", new Color(0x80, 0x00, 0x00));
    COLOR_MAP.put("navy", new Color(0x00, 0x00, 0x80));
    COLOR_MAP.put("olive", new Color(0x80, 0x80, 0x00));
    COLOR_MAP.put("purple", new Color(0x80, 0x00, 0x80));
    COLOR_MAP.put("silver", new Color(0xC0, 0xC0, 0xC0));
    COLOR_MAP.put("teal", new Color(0x00, 0x80, 0x80));
    // Additional Gray colors
    COLOR_MAP.put("gray25", new Color(0x3F, 0x3F, 0x3F));
    COLOR_MAP.put("gray50", new Color(0x6F, 0x7F, 0x7F));
    COLOR_MAP.put("gray75", new Color(0xBF, 0xBF, 0xBF));

    /**
     * The full list of named colours in HTML5 (apparently)
     */
    COLOR_MAP_HTML5.put("AliceBlue", new Color(0xF0F8FF));
    COLOR_MAP_HTML5.put("AntiqueWhite", new Color(0xFAEBD7));
    COLOR_MAP_HTML5.put("Aqua", new Color(0x00FFFF));
    COLOR_MAP_HTML5.put("Aquamarine", new Color(0x7FFFD4));
    COLOR_MAP_HTML5.put("Azure", new Color(0xF0FFFF));
    COLOR_MAP_HTML5.put("Beige", new Color(0xF5F5DC));
    COLOR_MAP_HTML5.put("Bisque", new Color(0xFFE4C4));
    COLOR_MAP_HTML5.put("Black", new Color(0x000000));
    COLOR_MAP_HTML5.put("BlanchedAlmond", new Color(0xFFEBCD));
    COLOR_MAP_HTML5.put("Blue", new Color(0x0000FF));
    COLOR_MAP_HTML5.put("BlueViolet", new Color(0x8A2BE2));
    COLOR_MAP_HTML5.put("Brown", new Color(0xA52A2A));
    COLOR_MAP_HTML5.put("BurlyWood", new Color(0xDEB887));
    COLOR_MAP_HTML5.put("CadetBlue", new Color(0x5F9EA0));
    COLOR_MAP_HTML5.put("Chartreuse", new Color(0x7FFF00));
    COLOR_MAP_HTML5.put("Chocolate", new Color(0xD2691E));
    COLOR_MAP_HTML5.put("Coral", new Color(0xFF7F50));
    COLOR_MAP_HTML5.put("CornflowerBlue", new Color(0x6495ED));
    COLOR_MAP_HTML5.put("Cornsilk", new Color(0xFFF8DC));
    COLOR_MAP_HTML5.put("Crimson", new Color(0xDC143C));
    COLOR_MAP_HTML5.put("Cyan", new Color(0x00FFFF));
    COLOR_MAP_HTML5.put("DarkBlue", new Color(0x00008B));
    COLOR_MAP_HTML5.put("DarkCyan", new Color(0x008B8B));
    COLOR_MAP_HTML5.put("DarkGoldenRod", new Color(0xB8860B));
    COLOR_MAP_HTML5.put("DarkGray", new Color(0xA9A9A9));
    COLOR_MAP_HTML5.put("DarkGrey", new Color(0xA9A9A9));
    COLOR_MAP_HTML5.put("DarkGreen", new Color(0x006400));
    COLOR_MAP_HTML5.put("DarkKhaki", new Color(0xBDB76B));
    COLOR_MAP_HTML5.put("DarkMagenta", new Color(0x8B008B));
    COLOR_MAP_HTML5.put("DarkOliveGreen", new Color(0x556B2F));
    COLOR_MAP_HTML5.put("DarkOrange", new Color(0xFF8C00));
    COLOR_MAP_HTML5.put("DarkOrchid", new Color(0x9932CC));
    COLOR_MAP_HTML5.put("DarkRed", new Color(0x8B0000));
    COLOR_MAP_HTML5.put("DarkSalmon", new Color(0xE9967A));
    COLOR_MAP_HTML5.put("DarkSeaGreen", new Color(0x8FBC8F));
    COLOR_MAP_HTML5.put("DarkSlateBlue", new Color(0x483D8B));
    COLOR_MAP_HTML5.put("DarkSlateGray", new Color(0x2F4F4F));
    COLOR_MAP_HTML5.put("DarkSlateGrey", new Color(0x2F4F4F));
    COLOR_MAP_HTML5.put("DarkTurquoise", new Color(0x00CED1));
    COLOR_MAP_HTML5.put("DarkViolet", new Color(0x9400D3));
    COLOR_MAP_HTML5.put("DeepPink", new Color(0xFF1493));
    COLOR_MAP_HTML5.put("DeepSkyBlue", new Color(0x00BFFF));
    COLOR_MAP_HTML5.put("DimGray", new Color(0x696969));
    COLOR_MAP_HTML5.put("DimGrey", new Color(0x696969));
    COLOR_MAP_HTML5.put("DodgerBlue", new Color(0x1E90FF));
    COLOR_MAP_HTML5.put("FireBrick", new Color(0xB22222));
    COLOR_MAP_HTML5.put("FloralWhite", new Color(0xFFFAF0));
    COLOR_MAP_HTML5.put("ForestGreen", new Color(0x228B22));
    COLOR_MAP_HTML5.put("Fuchsia", new Color(0xFF00FF));
    COLOR_MAP_HTML5.put("Gainsboro", new Color(0xDCDCDC));
    COLOR_MAP_HTML5.put("GhostWhite", new Color(0xF8F8FF));
    COLOR_MAP_HTML5.put("Gold", new Color(0xFFD700));
    COLOR_MAP_HTML5.put("GoldenRod", new Color(0xDAA520));
    COLOR_MAP_HTML5.put("Gray", new Color(0x808080));
    COLOR_MAP_HTML5.put("Grey", new Color(0x808080));
    COLOR_MAP_HTML5.put("Green", new Color(0x008000));
    COLOR_MAP_HTML5.put("GreenYellow", new Color(0xADFF2F));
    COLOR_MAP_HTML5.put("HoneyDew", new Color(0xF0FFF0));
    COLOR_MAP_HTML5.put("HotPink", new Color(0xFF69B4));
    COLOR_MAP_HTML5.put("IndianRed", new Color(0xCD5C5C));
    COLOR_MAP_HTML5.put("Indigo", new Color(0x4B0082));
    COLOR_MAP_HTML5.put("Ivory", new Color(0xFFFFF0));
    COLOR_MAP_HTML5.put("Khaki", new Color(0xF0E68C));
    COLOR_MAP_HTML5.put("Lavender", new Color(0xE6E6FA));
    COLOR_MAP_HTML5.put("LavenderBlush", new Color(0xFFF0F5));
    COLOR_MAP_HTML5.put("LawnGreen", new Color(0x7CFC00));
    COLOR_MAP_HTML5.put("LemonChiffon", new Color(0xFFFACD));
    COLOR_MAP_HTML5.put("LightBlue", new Color(0xADD8E6));
    COLOR_MAP_HTML5.put("LightCoral", new Color(0xF08080));
    COLOR_MAP_HTML5.put("LightCyan", new Color(0xE0FFFF));
    COLOR_MAP_HTML5.put("LightGoldenRodYellow", new Color(0xFAFAD2));
    COLOR_MAP_HTML5.put("LightGray", new Color(0xD3D3D3));
    COLOR_MAP_HTML5.put("LightGrey", new Color(0xD3D));
    COLOR_MAP_HTML5.put("LightGreen", new Color(0x90EE90));
    COLOR_MAP_HTML5.put("LightPink", new Color(0xFFB6C1));
    COLOR_MAP_HTML5.put("LightSalmon", new Color(0xFFA07A));
    COLOR_MAP_HTML5.put("LightSeaGreen", new Color(0x20B2AA));
    COLOR_MAP_HTML5.put("LightSkyBlue", new Color(0x87CEFA));
    COLOR_MAP_HTML5.put("LightSlateGray", new Color(0x778899));
    COLOR_MAP_HTML5.put("LightSlateGrey", new Color(0x778899));
    COLOR_MAP_HTML5.put("LightSteelBlue", new Color(0xB0C4DE));
    COLOR_MAP_HTML5.put("LightYellow", new Color(0xFFFFE0));
    COLOR_MAP_HTML5.put("Lime", new Color(0x00FF00));
    COLOR_MAP_HTML5.put("LimeGreen", new Color(0x32CD32));
    COLOR_MAP_HTML5.put("Linen", new Color(0xFAF0E6));
    COLOR_MAP_HTML5.put("Magenta", new Color(0xFF00FF));
    COLOR_MAP_HTML5.put("Maroon", new Color(0x800000));
    COLOR_MAP_HTML5.put("MediumAquaMarine", new Color(0x66CDAA));
    COLOR_MAP_HTML5.put("MediumBlue", new Color(0x0000CD));
    COLOR_MAP_HTML5.put("MediumOrchid", new Color(0xBA55D3));
    COLOR_MAP_HTML5.put("MediumPurple", new Color(0x9370DB));
    COLOR_MAP_HTML5.put("MediumSeaGreen", new Color(0x3CB371));
    COLOR_MAP_HTML5.put("MediumSlateBlue", new Color(0x7B68EE));
    COLOR_MAP_HTML5.put("MediumSpringGreen", new Color(0x00FA9A));
    COLOR_MAP_HTML5.put("MediumTurquoise", new Color(0x48D1CC));
    COLOR_MAP_HTML5.put("MediumVioletRed", new Color(0xC71585));
    COLOR_MAP_HTML5.put("MidnightBlue", new Color(0x191970));
    COLOR_MAP_HTML5.put("MintCream", new Color(0xF5FFFA));
    COLOR_MAP_HTML5.put("MistyRose", new Color(0xFFE4E1));
    COLOR_MAP_HTML5.put("Moccasin", new Color(0xFFE4B5));
    COLOR_MAP_HTML5.put("NavajoWhite", new Color(0xFFDEAD));
    COLOR_MAP_HTML5.put("Navy", new Color(0x000080));
    COLOR_MAP_HTML5.put("OldLace", new Color(0xFDF5E6));
    COLOR_MAP_HTML5.put("Olive", new Color(0x808000));
    COLOR_MAP_HTML5.put("OliveDrab", new Color(0x6B8E23));
    COLOR_MAP_HTML5.put("Orange", new Color(0xFFA500));
    COLOR_MAP_HTML5.put("OrangeRed", new Color(0xFF4500));
    COLOR_MAP_HTML5.put("Orchid", new Color(0xDA70D6));
    COLOR_MAP_HTML5.put("PaleGoldenRod", new Color(0xEEE8AA));
    COLOR_MAP_HTML5.put("PaleGreen", new Color(0x98FB98));
    COLOR_MAP_HTML5.put("PaleTurquoise", new Color(0xAFEEEE));
    COLOR_MAP_HTML5.put("PaleVioletRed", new Color(0xDB7093));
    COLOR_MAP_HTML5.put("PapayaWhip", new Color(0xFFEFD5));
    COLOR_MAP_HTML5.put("PeachPuff", new Color(0xFFDAB9));
    COLOR_MAP_HTML5.put("Peru", new Color(0xCD853F));
    COLOR_MAP_HTML5.put("Pink", new Color(0xFFC0CB));
    COLOR_MAP_HTML5.put("Plum", new Color(0xDDA0DD));
    COLOR_MAP_HTML5.put("PowderBlue", new Color(0xB0E0E6));
    COLOR_MAP_HTML5.put("Purple", new Color(0x800080));
    COLOR_MAP_HTML5.put("Red", new Color(0xFF0000));
    COLOR_MAP_HTML5.put("RosyBrown", new Color(0xBC8F8F));
    COLOR_MAP_HTML5.put("RoyalBlue", new Color(0x4169E1));
    COLOR_MAP_HTML5.put("SaddleBrown", new Color(0x8B4513));
    COLOR_MAP_HTML5.put("Salmon", new Color(0xFA8072));
    COLOR_MAP_HTML5.put("SandyBrown", new Color(0xF4A460));
    COLOR_MAP_HTML5.put("SeaGreen", new Color(0x2E8B57));
    COLOR_MAP_HTML5.put("SeaShell", new Color(0xFFF5EE));
    COLOR_MAP_HTML5.put("Sienna", new Color(0xA0522D));
    COLOR_MAP_HTML5.put("Silver", new Color(0xC0C0C0));
    COLOR_MAP_HTML5.put("SkyBlue", new Color(0x87CEEB));
    COLOR_MAP_HTML5.put("SlateBlue", new Color(0x6A5ACD));
    COLOR_MAP_HTML5.put("SlateGray", new Color(0x708090));
    COLOR_MAP_HTML5.put("SlateGrey", new Color(0x708090));
    COLOR_MAP_HTML5.put("Snow", new Color(0xFFFAFA));
    COLOR_MAP_HTML5.put("SpringGreen", new Color(0x00FF7F));
    COLOR_MAP_HTML5.put("SteelBlue", new Color(0x4682B4));
    COLOR_MAP_HTML5.put("Tan", new Color(0xD2B48C));
    COLOR_MAP_HTML5.put("Teal", new Color(0x008080));
    COLOR_MAP_HTML5.put("Thistle", new Color(0xD8BFD8));
    COLOR_MAP_HTML5.put("Tomato", new Color(0xFF6347));
    COLOR_MAP_HTML5.put("Turquoise", new Color(0x40E0D0));
    COLOR_MAP_HTML5.put("Violet", new Color(0xEE82EE));
    COLOR_MAP_HTML5.put("Wheat", new Color(0xF5DEB3));
    COLOR_MAP_HTML5.put("White", new Color(0xFFFFFF));
    COLOR_MAP_HTML5.put("WhiteSmoke", new Color(0xF5F5F5));
    COLOR_MAP_HTML5.put("Yellow", new Color(0xFFFF00));
    COLOR_MAP_HTML5.put("YellowGreen", new Color(0x9ACD32));
    /*
     * These are valid HTML colors. When getFontColor() is called, if one of these is selected then the name is returned. When another value is selected, the Color is converted to the '#112233f'
     * notation and returned instead -- even if it's a name in COLOR_MAP, above.
     */
    String[] html = {
      "black", "white", "fuchsia", "aqua", "silver", "red", "lime", "blue", "yellow", "gray",
      "purple", "maroon", "navy", "olive", "green", "teal"
    };
    for (String s : html) {
      Color c = COLOR_MAP.get(s);
      assert c != null : "HTML color not in predefined list?";
      COLOR_MAP_HTML.put(s, c);
    }
  }
public static Color getRandomColor(){
    int idx = getRandomNumber(COLOR_MAP_HTML5.size() - 1);
    return COLOR_MAP_HTML5.get(COLOR_MAP_HTML5.keySet().stream().toList().get(idx));
}
  public static int getRandomNumber(int max) {
    return getRandomNumber(0, max);
  }

  public static int getRandomNumber(int min, int max) {
    return RAND.nextInt(max - min + 1) + min;
  }

  public static float getRandomRealNumber(float max) {
    return getRandomRealNumber(0, max);
  }

  public static float getRandomRealNumber(float min, float max) {
    return (max - min) * RAND.nextFloat() + min;
  }

  public static boolean percentageCheckAbove(int percentage) {
    return RAND.nextDouble() * 100 > percentage;
  }

  public static boolean percentageCheckBelow(int percentage) {
    return RAND.nextDouble() * 100 < percentage;
  }

  private static final Pattern NAME_PATTERN = Pattern.compile("^(.*)\\s+(\\d+)\\s*$");

  /**
   * Determine what the name of the new token should be. This method tries to choose a token name
   * which is (a) unique and (b) adheres to a numeric sequence.
   *
   * @param zone the map that the token is being placed onto
   * @param token the new token to be named
   * @param force if {@code false} a new name will not be generated unless the token naming
   *     prefrence in {@link AppPreferences} is {@link Token#NAME_USE_CREATURE}.
   * @return the new token's algorithmically generated name
   */
  public static String nextTokenId(Zone zone, Token token, boolean force) {
    boolean isToken = token.getLayer().isTokenLayer();
    String baseName = token.getName();
    String newName;
    Integer newNum = null;

    if (isToken && AppPreferences.newTokenNaming.get().equals(Token.NAME_USE_CREATURE)) {
      newName = I18N.getString("Token.name.creature");
    } else if (!force) {
      return baseName;
    } else if (baseName == null) {
      int nextId = nextTokenId.getAndIncrement();
      char ch = (char) ('a' + MapTool.getPlayerList().indexOf(MapTool.getPlayer()));
      return ch + Integer.toString(nextId);
    } else {
      baseName = baseName.trim();
      Matcher m = NAME_PATTERN.matcher(baseName);
      if (m.find()) {
        newName = m.group(1);
        try {
          newNum = Integer.parseInt(m.group(2));
        } catch (NumberFormatException nfe) {
          /*
           * This exception happens if the number is too big to fit inside an integer. In this case, we use the original name as the filename and assign a new number as the suffix.
           */
          newName = baseName;
        }
      } else {
        newName = baseName;
      }
    }
    boolean random =
        (isToken && AppPreferences.duplicateTokenNumber.get().equals(Token.NUM_RANDOM));

    var tokenNumberDisplay = AppPreferences.tokenNumberDisplay.get();
    boolean addNumToGM = !tokenNumberDisplay.equals(Token.NUM_ON_NAME);
    boolean addNumToName = !tokenNumberDisplay.equals(Token.NUM_ON_GM);

    /*
     * If the token already has a number suffix, if the preferences indicate that token numbering should be random and this token is on the Token layer, or if the token already exists somewhere on
     * this map, then we need to choose a new name.
     */
    if (newNum != null || random || zone.getTokenByName(newName) != null) {

      if (random) {
        do {
          newNum = randomSuffixFactory.nextSuffixForToken(newName);
        } while (nameIsDuplicate(zone, newName, newNum, addNumToName, addNumToGM));

      } else {
        newNum = zone.findFreeNumber(addNumToName ? newName : null, addNumToGM);
      }

      if (addNumToName) {
        newName += " ";
        newName += newNum;
      }

      // GM names just get a number
      if (addNumToGM) {
        token.setGMName(Integer.toString(newNum));
      }
    }
    return newName;
  }

  private static boolean nameIsDuplicate(
      Zone zone, String newName, Integer newNum, boolean playerName, boolean gmName) {
    boolean result = false;

    if (playerName) {
      result = zone.getTokenByName(newName + " " + newNum) != null;
    }
    if (gmName) {
      result = zone.getTokenByGMName(Integer.toString(newNum)) != null;
    }
    return result;
  }

  public static boolean isDebugEnabled() {
    return System.getProperty("MAPTOOL_DEV") != null;
  }

  public static boolean isValidColor(String name) {
    return COLOR_MAP.containsKey(name);
  }

  public static boolean isHtmlColor(String name) {
    return COLOR_MAP_HTML.containsKey(name);
  }

  /**
   * Returns a {@link Color} object if the parameter can be evaluated as a color. This includes a
   * text search against a list of known colors (case-insensitive; see {@link #COLOR_MAP}) and
   * conversion of the string into a color using {@link Color#decode(String)}. Invalid strings cause
   * <code>COLOR_MAP.get("black")</code> to be returned. Calls {@link #convertStringToColor(String)}
   * if the parameter is not a recognized color name.
   *
   * @param name a recognized color name or an integer color value in octal or hexadecimal form
   *     (such as <code>#123</code>, <code>0x112233</code>, or <code>0X111222333</code>)
   * @return the corresponding Color object or {@link Color#BLACK} if not in a recognized format
   */
  public static Color getColor(String name) {
    name = name.trim();
    Color c = COLOR_MAP.get(name);
    if (c != null) {
      return c;
    }
    c = COLOR_MAP_HTML5.get(name);
    if (c != null) {
      return c;
    }
    c = convertStringToColor(name);
    return c;
  }

  /**
   * Converts the incoming string value to a Color object and stores <code>val</code> and the Color
   * as a key/value pair in a cache. The incoming string may start with a <code>#</code> to indicate
   * a numeric color value in CSS format. Any errors cause {@link #COLOR_MAP} <code>.get("black")
   * </code> to be returned.
   *
   * @param val color value to interpret
   * @return Color object
   */
  private static Color convertStringToColor(String val) {
    Color c;
    if (StringUtil.isEmpty(val)) {
      c = COLOR_MAP.get("black");
    } else {
      try {
        c = Color.decode(val);
        COLOR_MAP.put(val.toLowerCase(), c);
      } catch (NumberFormatException nfe) {
        c = COLOR_MAP.get("black");
      }
    }
    return c;
  }

  public static Set<String> getColorNames() {
    return COLOR_MAP.keySet();
  }

  public static void uploadTexture(DrawablePaint paint) {
    if (paint == null) {
      return;
    }
    if (paint instanceof DrawableTexturePaint) {
      Asset asset = ((DrawableTexturePaint) paint).getAsset();
      uploadAsset(asset);
    }
  }

  public static void uploadAsset(Asset asset) {
    if (asset == null) {
      return;
    }
    if (!AssetManager.hasAsset(asset.getMD5Key())) {
      AssetManager.putAsset(asset);
    }
    if (!MapTool.isHostingServer() && !MapTool.getCampaign().containsAsset(asset.getMD5Key())) {
      MapTool.serverCommand().putAsset(asset);
    }
  }
}
