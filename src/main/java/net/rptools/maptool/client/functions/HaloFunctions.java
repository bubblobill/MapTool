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
package net.rptools.maptool.client.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.MapToolUtil;
import net.rptools.maptool.client.MapToolVariableResolver;
import net.rptools.maptool.client.functions.json.JSONMacroFunctions;
import net.rptools.maptool.client.ui.Halos;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.Halo;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.util.FunctionUtil;
import net.rptools.maptool.util.StringUtil;
import net.rptools.maptool.util.svg.SVGModificationFactory;
import net.rptools.parser.Parser;
import net.rptools.parser.ParserException;
import net.rptools.parser.VariableResolver;
import net.rptools.parser.function.AbstractFunction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HaloFunctions extends AbstractFunction {
    private static final Logger log = LogManager.getLogger(HaloFunctions.class);
    // TODO: This is a copy of the array in the {@link TokenPopupMenu} (which is apparently temporary)
    private static final HaloFunctions instance = new HaloFunctions();

    private HaloFunctions() {
        super(
                0,
                3,
                "getHalo",
                "setHalo",
                "hasHalo",
                "halo.getProps",
                "halo.setProps",
                "halo.getTypes",
                "halo.getStyles",
                "halo.getImages",
                "halo.getSVGFilters",
                "halo.remove",
                "halo.addColor",
                "halo.addColour",
                "halo.removeColor",
                "halo.removeColour",
                "halo.addDrawing",
                "halo.setDrawing"
        );
    }

    /**
     * Gets the singleton Halo instance.
     *
     * @return the Halo instance.
     */
    public static HaloFunctions getInstance() {
        return instance;
    }

    @Override
    public Object childEvaluate(
            Parser parser, VariableResolver resolver, String functionName, List<Object> parameters)
            throws ParserException {
        // First we have the old that deal with the old token.haloColor
        if (functionName.equalsIgnoreCase("getHalo")) {
            return getHalo((MapToolVariableResolver) resolver, parameters);
        } else if (functionName.equalsIgnoreCase("setHalo")) {
            return setHalo((MapToolVariableResolver) resolver, parameters);
        }
        // Now we move onto the new that deal with the Halo class
        else if (functionName.equalsIgnoreCase("hasHalo")) {
            FunctionUtil.checkNumberParam(functionName, parameters, 0, 2);
            Token token = FunctionUtil.getTokenFromParam(resolver, "hasHalo", parameters, 0, 1);
            return token.hasHalo();
        } else if (functionName.equalsIgnoreCase("halo.remove")) {
            FunctionUtil.checkNumberParam(functionName, parameters, 0, 2);
            Token token = FunctionUtil.getTokenFromParam(resolver, "removeHalo", parameters, 0, 1);
            token.removeHalo();
            return true;
        } else if (functionName.equalsIgnoreCase("halo.addColor") || functionName.equalsIgnoreCase("halo.addColour")) {
            FunctionUtil.checkNumberParam(functionName, parameters, 0, 3);
            Token token = FunctionUtil.getTokenFromParam(resolver, "removeHalo", parameters, 1, 2);
            token.getHalo().addColour(
                    MapToolUtil.getColor(
                        FunctionUtil.paramAsString(functionName, parameters, 0, true)
                    )
            );
            return true;
        } else if (functionName.equalsIgnoreCase("halo.removeColor") || functionName.equalsIgnoreCase("halo.removeColour")) {
            FunctionUtil.checkNumberParam(functionName, parameters, 0, 3);
            Token token = FunctionUtil.getTokenFromParam(resolver, "removeHalo", parameters, 1, 2);
            token.getHalo().removeColour(
                    MapToolUtil.getColor(
                            FunctionUtil.paramAsString(functionName, parameters, 0, true)
                    )
            );
            return true;
        } else if (functionName.equalsIgnoreCase("halo.addDrawing")) {
            FunctionUtil.checkNumberParam(functionName, parameters, 0, 3);
            Token token = FunctionUtil.getTokenFromParam(resolver, "removeHalo", parameters, 1, 2);
            token.getHalo().addDrawing(FunctionUtil.paramAsString(functionName, parameters, 0, true));
            return true;
        } else if (functionName.equalsIgnoreCase("halo.setDrawing")) {
            FunctionUtil.checkNumberParam(functionName, parameters, 0, 3);
            Token token = FunctionUtil.getTokenFromParam(resolver, "removeHalo", parameters, 1, 2);
            token.getHalo().setDrawing(FunctionUtil.paramAsString(functionName, parameters, 0, true));
            return true;
        } else if (functionName.equalsIgnoreCase("halo.getSVGFilters")) {
            return getHaloEnum(functionName, parameters);
        } else if (functionName.equalsIgnoreCase("halo.getTypes")) {
            return getHaloEnum(functionName, parameters);
        } else if (functionName.equalsIgnoreCase("halo.getStyles")) {
            return getHaloEnum(functionName, parameters);
        } else if (functionName.equalsIgnoreCase("halo.getImages")) {
            return getHaloEnum(functionName, parameters);
        } else if (functionName.equalsIgnoreCase("halo.setProps")) {
            return setHaloProps(resolver, functionName, parameters);
        } else if (functionName.equalsIgnoreCase("halo.getProps")) {
            return getHaloProps(resolver, functionName, parameters);
        }
        log.info("Unknown halo function.");
        throw new ParserException("Unknown function name: " + functionName);

    }

    /**
     * Gets the halo of the token.
     *
     * @param parameters The arguments.
     * @return the halo color.
     * @throws ParserException if an error occurs.
     */
    private Object getHalo(MapToolVariableResolver resolver, List<Object> parameters)
            throws ParserException {
        Token token;

        if (parameters.size() == 1) {
            if (!MapTool.getParser().isMacroTrusted()) {
                throw new ParserException(I18N.getText("macro.function.general.noPermOther", "getHalo"));
            }
            token = FindTokenFunctions.findToken(parameters.getFirst().toString(), null);
            if (token == null) {
                throw new ParserException(
                        I18N.getText("macro.function.general.unknownToken", "getHalo", parameters.getFirst().toString()));
            }
        } else if (parameters.isEmpty()) {
            token = resolver.getTokenInContext();
            if (token == null) {
                throw new ParserException(I18N.getText("macro.function.general.noImpersonated", "getHalo"));
            }
        } else {
            throw new ParserException(
                    I18N.getText("macro.function.general.tooManyParam", "getHalo", 1, parameters.size()));
        }
        return getHalo(token);
    }

    /**
     * Sets the halo of the token.
     *
     * @param parameters The arguments.
     * @return the halo color.
     * @throws ParserException if an error occurs.
     */
    private Object setHalo(MapToolVariableResolver resolver, List<Object> parameters)
            throws ParserException {

        Token token;
        Object value = parameters.get(0);

        switch (parameters.size()) {
            case 0:
                throw new ParserException(
                        I18N.getText("macro.function.general.notEnoughParam", "setHalo", 1, parameters.size()));
            default:
                throw new ParserException(
                        I18N.getText("macro.function.general.tooManyParam", "setHalo", 2, parameters.size()));
            case 1:
                token = resolver.getTokenInContext();
                if (token == null) {
                    throw new ParserException(
                            I18N.getText("macro.function.general.noImpersonated", "setHalo"));
                }
                break;
            case 2:
                if (!MapTool.getParser().isMacroTrusted()) {
                    throw new ParserException(I18N.getText("macro.function.general.noPermOther", "setHalo"));
                }
                token = FindTokenFunctions.findToken(parameters.get(1).toString(), null);
                if (token == null) {
                    throw new ParserException(
                            I18N.getText(
                                    "macro.function.general.unknownToken", "setHalo", parameters.get(1).toString()));
                }
        }
        setHalo(token, value);
        return value;
    }

    private Object getHaloEnum(String functionName, List<Object> parameters) {
        String delimiter;
        List<String> values = new ArrayList<>();
        if (!parameters.isEmpty()) {
            delimiter = (String) parameters.getFirst();
        } else {
            delimiter = "json";
        }
        switch (functionName) {
            case "halo.getTypes" ->
                    values = Arrays.stream(Halo.Type.values()).toList().stream().map(Enum::toString).toList();
            case "halo.getStyles" ->
                    values = Arrays.stream(Halo.Style.values()).toList().stream().map(Enum::toString).toList();
            case "halo.getImages" ->
                    values = Arrays.stream(Halos.values()).toList().stream().map(Enum::toString).toList();
            case "halo.getSVGFilters" ->
                    values = SVGModificationFactory.getAvailableFilters();
        }
        if ("json".equals(delimiter)) {
            final JsonArray jArr = new JsonArray();
            values.forEach(jArr::add);
            return jArr;
        } else {
            return String.join(delimiter, values);
        }
    }

    private boolean setHaloProps(VariableResolver resolver, String functionName, List<Object> parameters) throws ParserException {
        /*
         * setHaloProps(StrProp/JSON Object, token: currentToken(), mapName = current map)
         */
        FunctionUtil.checkNumberParam(functionName, parameters, 1, 3);
        Token token = FunctionUtil.getTokenFromParam(resolver, functionName, parameters, 1, 2);
        if (!MapTool.getParser().isMacroTrusted() &&
                (!token.isOwnedByAll() || !token.isOwner(MapTool.getPlayer().getName()) || !MapTool.getPlayer().isGM())) {
            throw new ParserException(I18N.getText("macro.function.general.noPermOther", "getHalo"));
        }
        Halo halo = token.getHalo();
        if (halo == null) {
            halo = new Halo();
        }
        JsonObject json;
        try { // try for json object
            json = FunctionUtil.paramAsJsonObject("setHaloProps", parameters, 0);
        } catch (ParserException pe) {
            try { // try for strProp
                json =
                        JSONMacroFunctions.getInstance()
                                .getJsonObjectFunctions()
                                .fromStrProp(
                                        FunctionUtil.paramAsString(
                                                "setHaloProps", parameters, 0, true),
                                        ";");
            } catch (ParserException pe2) {
                throw new ParserException(
                        I18N.getText(
                                "macro.function.input.illegalArgumentType", "unknown", "JSON Object/StringProp"));
            }
        }
        try {
            if (json != null && json.isJsonObject()) {
                JsonObject jObj = json.getAsJsonObject();
                for (String s : jObj.keySet()) {
                    switch (s.toLowerCase()) {
                        case "drawing", "path" -> halo.setDrawing(jObj.get(s).getAsString().toUpperCase());
                        case "filled", "fill" -> halo.setFilled(jObj.get(s).getAsBoolean());
                        case "isoflip", "flipiso" -> halo.setIsoFlipped(jObj.get(s).getAsBoolean());
                        case "facing", "usefacing" -> halo.setIsoFlipped(jObj.get(s).getAsBoolean());
                        case "image", "imageid", "assetid" ->
                                halo.setImageId(FunctionUtil.getAssetKeyFromString(jObj.get(s).getAsString()));
                        case "opacity" -> halo.setOpacity(jObj.get(s).getAsFloat());
                        case "rotation" -> halo.setRotation(jObj.get(s).getAsDouble());
                        case "scale" -> halo.setScaleFactor(jObj.get(s).getAsDouble());
                        case "stockimage", "stock_image" -> {
                            try {
                                halo.setStockImage(Halos.get(jObj.get(s).getAsString().toUpperCase()));
                            } catch (IllegalArgumentException iae) {
                                throw new ParserException(I18N.getText("macro.function.parse.enum.illegalArgumentType", functionName, jObj.get(s).getAsString(), Arrays.toString(Halos.values())));
                            }
                        }
                        case "style" -> {
                            try {
                                halo.setStyle(Halo.Style.get(jObj.get(s).getAsString().toUpperCase()));
                            } catch (IllegalArgumentException iae) {
                                throw new ParserException(I18N.getText("macro.function.parse.enum.illegalArgumentType", functionName, jObj.get(s).getAsString(), Arrays.toString(Halo.Style.values())));
                            }
                        }
                        case "type" -> {
                            try {
                                halo.setType(Halo.Type.get(jObj.get(s).getAsString().toUpperCase()));
                            } catch (IllegalArgumentException iae) {
                                throw new ParserException(I18N.getText("macro.function.parse.enum.illegalArgumentType", functionName, jObj.get(s).getAsString(), Arrays.toString(Halo.Type.values())));
                            }
                        }
                        case "colour", "color", "colours", "colors" -> {
                            final List<Color> colourList = new ArrayList<>();
                            JsonArray values;
                            if (jObj.get(s).isJsonArray()) {
                                values = jObj.get(s).getAsJsonArray();
                            } else {
                                values = JSONMacroFunctions.getInstance().getJsonArrayFunctions().fromStringList(jObj.get(s).getAsString(), ",");
                            }
                            values.forEach(
                                    jsonElement -> {
                                        String cs = jsonElement.getAsString();
                                        colourList.add(MapToolUtil.getColor(cs));
                                    });
                            halo.setColourList(colourList);
                        }
                        default -> throw new ParserException(
                                I18N.getText("macro.function.sound.illegalArgument", functionName, s)
                        );
                    }
                }
                token.setHalo(halo);
                return true;
            }
        } catch (IllegalArgumentException e) {
            throw new ParserException(I18N.getText(
                    "macro.function.input.illegalArgumentType", e, "unknown", "JSON Object/StringProp"));
//                log.info(e.getMessage(), e.fillInStackTrace());
        }
        Configurator.setLevel(log, Level.DEBUG);
        Configurator.setLevel(log, Level.INFO);
        return false;
    }

    private Object getHaloProps(VariableResolver resolver, String functionName, List<Object> parameters) throws ParserException {
        FunctionUtil.checkNumberParam(functionName, parameters, 0, 3);
        String delimiter = !parameters.isEmpty() ? parameters.getFirst().toString() : ";";
        Token token = FunctionUtil.getTokenFromParam(resolver, functionName, parameters, 1, 2);
        if (!token.hasHalo()) {
            return false;
        }
        Halo halo = token.getHalo();
        String type = String.valueOf(halo.getType());
        String imageId = String.valueOf(halo.getImageId());
        String stockImage = String.valueOf(halo.getStockImage());
        boolean filled = halo.isFilled();
        double opacity = halo.getOpacity();
        double rotation = halo.getRotation();
        double scale = halo.getScaleFactor();

        String style = String.valueOf(halo.getStyle());
        String drawing = Arrays.toString(halo.getSvgPaths().toArray());

        if ("json".equals(delimiter)) {
            JsonObject jArr = new JsonObject();
            jArr.addProperty("type", type);
            jArr.addProperty("style", style);
            jArr.addProperty("filled", filled);
            jArr.addProperty("opacity", opacity);
            jArr.addProperty("rotation", rotation);
            jArr.addProperty("scale", scale);
            jArr.addProperty("colours", Arrays.toString(halo.getColourList().toArray()));
            if (halo.hasImageId()) {
                jArr.addProperty("image", imageId);
            }
            if (halo.hasStockImage()) {
                jArr.addProperty("stockImage", stockImage);
            }
            if (halo.hasSVGPath()) {
                jArr.addProperty("svg", drawing);
            }
            return jArr;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("type=").append(type).append(delimiter);
            sb.append("filled=").append(filled).append(delimiter);
            sb.append("opacity=").append(opacity).append(delimiter);
            sb.append("colours=").append(halo.getColourList()).append(delimiter);
            sb.append("style=").append(style).append(delimiter);
            sb.append("rotation=").append(rotation).append(delimiter);
            sb.append("scale=").append(scale).append(delimiter);
            if (halo.hasImageId()) {
                sb.append("image=").append(imageId).append(delimiter);
            }
            if (halo.hasStockImage()) {
                sb.append("stockImage=").append(stockImage).append(delimiter);
            }
            if (halo.hasSVGPath()) {
                sb.append("svg=").append(drawing).append(delimiter);
            }
            return sb.toString();
        }
    }

    /**
     * Gets the halo for the token.
     *
     * @param token the token to get the halo for.
     * @return the halo.
     */
    public static Object getHalo(Token token) {
        if (token.getHaloColor() != null) {
            return "#" + Integer.toHexString(token.getHaloColor().getRGB()).substring(2);
        } else {
            return "None";
        }
    }

    /**
     * Sets the halo color of the token.
     *
     * @param token the token to set halo of.
     * @param value the value to set.
     */
    public static void setHalo(Token token, Object value) {
        Color haloColor;
        if (value instanceof Color) {
            haloColor = (Color) value;
        } else if (value instanceof BigDecimal) {
            haloColor = new Color(((BigDecimal) value).intValue());
        } else {
            String col = value.toString();
            if (StringUtil.isEmpty(col)
                    || col.equalsIgnoreCase("none")
                    || col.equalsIgnoreCase("default")) {
                haloColor = null;
            } else {
                haloColor = MapToolUtil.getColor(col);
            }
        }
        var cmd = MapTool.serverCommand();

        if (haloColor != null) {
            cmd.updateTokenProperty(token, Token.Update.setHaloColor, haloColor.getRGB());
        } else {
            cmd.updateTokenProperty(token, Token.Update.setHaloColor);
        }
    }
}
