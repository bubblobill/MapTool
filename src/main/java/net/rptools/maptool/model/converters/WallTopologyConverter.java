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
package net.rptools.maptool.model.converters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAliasType;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.rptools.maptool.model.GUID;
import net.rptools.maptool.model.topology.WallTopology;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Converts {@link WallTopology} to a nice representation that is independent of the runtime
 * representation.
 *
 * <p>The format of a serialized {@link WallTopology} looks like this:
 *
 * <pre>{@code
 * <vertices>
 *   <vertex id="8A647D10EE1B4AFA8BEF4B93AEFB624D">
 *     <position x="0.0" y="0.0"/>
 *   </vertex>
 *   <vertex id="E3A964F13221428582E491AB3D3D3764">
 *     <position x="100.0" y="50.0"/>
 *   </vertex>
 *   <vertex id="C510571E42C74080AF34EC00D47FCF97">
 *     <position x="200.0" y="300.0"/>
 *   </vertex>
 * </vertices>
 * <walls>
 *   <wall from="8A647D10EE1B4AFA8BEF4B93AEFB624D" to="E3A964F13221428582E491AB3D3D3764"/>
 *   <wall from="E3A964F13221428582E491AB3D3D3764" to="C510571E42C74080AF34EC00D47FCF97"/>
 * </walls>
 * ...
 * }</pre>
 */
public class WallTopologyConverter extends AbstractCollectionConverter {
  private static final Logger log = LogManager.getLogger(WallTopologyConverter.class);

  public WallTopologyConverter(XStream xStream) {
    super(xStream.getMapper());
    xStream.processAnnotations(GraphRepresentation.class);
  }

  @Override
  public boolean canConvert(Class type) {
    return WallTopology.class.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    var walls = (WallTopology) source;
    var representation = new GraphRepresentation();

    walls
        .getVertices()
        .sorted(Comparator.comparingInt(WallTopology.Vertex::getZIndex))
        .map(
            vertex ->
                new VertexRepresentation(
                    vertex.id(), vertex.getPosition().getX(), vertex.getPosition().getY()))
        .forEach(representation.vertices::add);
    walls
        .getWalls()
        .sorted(Comparator.comparingInt(WallTopology.Wall::getZIndex))
        .map(wall -> new WallRepresentation(wall.from().id(), wall.to().id()))
        .forEach(representation.walls::add);

    context.convertAnother(representation);
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    var walls = new WallTopology();
    var representation =
        (GraphRepresentation) context.convertAnother(walls, GraphRepresentation.class);
    for (var vertexRepresentation : representation.vertices) {
      try {
        var vertex = walls.createVertex(new GUID(vertexRepresentation.id));
        vertex.setPosition(vertexRepresentation.x, vertexRepresentation.y);
      } catch (WallTopology.GraphException e) {
        log.error(
            "A vertex with ID {} is already defined; skipping this one",
            vertexRepresentation.id,
            e);
      }
    }
    for (var wallRepresentation : representation.walls) {
      try {
        walls.createWall(new GUID(wallRepresentation.from), new GUID(wallRepresentation.to));
      } catch (WallTopology.GraphException e) {
        log.error(
            "A wall with vertices ({}, {}) is already defined; skipping this one",
            wallRepresentation.from,
            wallRepresentation.to,
            e);
      }
    }

    walls.removeDanglingVertices();

    return walls;
  }

  // region Intermediate representation that contains only the essence of the wall graph.

  private static final class GraphRepresentation {
    public final List<VertexRepresentation> vertices = new ArrayList<>();
    public final List<WallRepresentation> walls = new ArrayList<>();
  }

  @XStreamAliasType("vertex")
  private static final class VertexRepresentation {
    @XStreamAsAttribute public final String id;
    @XStreamAsAttribute public final double x;
    @XStreamAsAttribute public final double y;

    public VertexRepresentation(GUID id, double x, double y) {
      this.id = id.toString();
      this.x = x;
      this.y = y;
    }
  }

  @XStreamAliasType("wall")
  private static final class WallRepresentation {
    @XStreamAsAttribute public final String from;
    @XStreamAsAttribute public final String to;

    public WallRepresentation(GUID from, GUID to) {
      this.from = from.toString();
      this.to = to.toString();
    }
  }

  // endregion
}
