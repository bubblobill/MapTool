package net.rptools.maptool.util;

import com.github.weisj.jsvg.geometry.path.BuildHistory;
import com.github.weisj.jsvg.geometry.path.PathCommand;
import com.github.weisj.jsvg.geometry.path.PathParser;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class DrawingUtil {
    public static Path2D svgToPath2D(String pathString){
        PathParser pp = new PathParser(pathString);
        PathCommand[] commands = pp.parsePathCommand();
        Path2D path = new Path2D.Float();
        BuildHistory hist = new BuildHistory();
        for (PathCommand pathCommand : commands) {
            pathCommand.appendPath(path, hist);
        }
        path.trimToSize();
        path = new Path2D.Float(AffineTransform.getTranslateInstance(-path.getBounds2D().getCenterX(), -path.getBounds2D().getCenterY()).createTransformedShape(path));
        return path;
    }
}
