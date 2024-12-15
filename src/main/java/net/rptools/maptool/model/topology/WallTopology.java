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
package net.rptools.maptool.model.topology;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.rptools.lib.GeometryUtil;
import net.rptools.maptool.model.GUID;
import net.rptools.maptool.server.proto.VertexDto;
import net.rptools.maptool.server.proto.WallDto;
import net.rptools.maptool.server.proto.WallTopologyDto;
import net.rptools.maptool.server.proto.drawing.DoublePointDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineSegment;

/**
 * Topology based on thin connected walls.
 *
 * <p>Wall topology forms a graph where the walls themselves are the edges of the graph.
 */
public final class WallTopology implements Topology {
  private static final Logger log = LogManager.getLogger(WallTopology.class);

  public static final class GraphException extends Exception {
    public GraphException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Represents the end of one or more walls.
   *
   * <p>Multiple {@code Vertex} instances may be created for a single vertex. All such instances can
   * be used interchangeably and with compare equal.
   */
  public final class Vertex {
    private final GUID id;
    private final VertexInternal internal;

    private Vertex(GUID id, VertexInternal internal) {
      this.id = id;
      this.internal = internal;
    }

    private WallTopology getOriginator() {
      return WallTopology.this;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Vertex vertex && Objects.equals(id, vertex.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    public GUID id() {
      return id;
    }

    public int getZIndex() {
      return internal.zIndex();
    }

    public Point2D getPosition() {
      var position = new Point2D.Double();
      position.setLocation(internal.position());
      return position;
    }

    public void setPosition(double x, double y) {
      internal.position().setLocation(x, y);
    }

    public void setPosition(Point2D position) {
      setPosition(position.getX(), position.getY());
    }

    public void bringToFront() {
      internal.zIndex(++nextZIndex);
    }
  }

  /**
   * Represents a single wall.
   *
   * <p>Multiple {@code Wall} instances may be created for a single wall. All such instances can be
   * used interchangeably and will compare equal.
   */
  public final class Wall {
    private final Vertex from;
    private final Vertex to;

    private Wall(Vertex from, Vertex to) {
      this.from = from;
      this.to = to;
    }

    private WallTopology getOriginator() {
      return WallTopology.this;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Wall wall
          && Objects.equals(from, wall.from)
          && Objects.equals(to, wall.to);
    }

    @Override
    public int hashCode() {
      return Objects.hash(from, to);
    }

    public Vertex from() {
      return from;
    }

    public Vertex to() {
      return to;
    }

    public int getZIndex() {
      return Math.max(from.getZIndex(), to.getZIndex());
    }

    /**
     * Represent this wall as a {@link LineSegment}.
     *
     * <p>This is a convenience method for getting the coordinates from {@link #from()} and {@link
     * #to()}. Since {@link LineSegment} cannot carry user data, the result does not include any
     * attached wall data.
     *
     * @return The {@link LineSegment} representation of the wall.
     */
    public LineSegment asSegment() {
      return new LineSegment(
          GeometryUtil.point2DToCoordinate(from.getPosition()),
          GeometryUtil.point2DToCoordinate(to.getPosition()));
    }
  }

  /**
   * Internal payload for a vertex.
   *
   * <p>Users of {@link WallTopology} will interact with the {@link Vertex} wrapper rather than with
   * {@code VertexInternal} directly.
   *
   * <p>The position is intentionally mutable as vertices are free to move about the plane without
   * affecting the graph structure.
   */
  private static final class VertexInternal {
    private final GUID id;
    private final Point2D position;
    private int zIndex;

    public VertexInternal(GUID id, Point2D position) {
      this.id = id;
      this.position = position;
      this.zIndex = -1;
    }

    public GUID id() {
      return id;
    }

    public Point2D position() {
      return position;
    }

    public int zIndex() {
      return zIndex;
    }

    public void zIndex(int zIndex) {
      this.zIndex = zIndex;
    }
  }

  /**
   * Internal representation of a wall.
   *
   * <p>Even though the underlying graph is undirected, walls still have a concept of a direction
   * This is only important when considering orientation, which is why it is not part of the graph
   * structure.
   *
   * <p>Uses of {@link WallTopology} will consume {@link Wall} instead of {@code WallInternal}. That
   * representation has all IDs already resolved to {@link Vertex}.
   *
   * @param from The ID of the source vertex.
   * @param to The ID of the target vertex.
   */
  private record WallInternal(GUID from, GUID to) {
    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }
  }

  /**
   * Return type for {@link #string(Point2D, Consumer)} that ensures walls are built in a valid
   * fashion.
   */
  public final class StringBuilder {
    // These vertices and walls are not added to the graph until build() is called.
    private final Point2D firstVertexPosition;
    private final ArrayList<Point2D> vertexPositions = new ArrayList<>();

    private StringBuilder(Point2D startingPoint) {
      this.firstVertexPosition = new Point2D.Double(startingPoint.getX(), startingPoint.getY());
    }

    public void push(Point2D nextPoint) {
      this.vertexPositions.add(new Point2D.Double(nextPoint.getX(), nextPoint.getY()));
    }

    /** Finish the string and return the last vertex. */
    public void build() {
      if (vertexPositions.isEmpty()) {
        // Insufficient points to add to the graph, otherwise vertices would dangle.
        return;
      }

      var previousVertex = createVertex();
      previousVertex.setPosition(firstVertexPosition);

      for (int i = 0; i < vertexPositions.size(); ++i) {
        var currentPosition = vertexPositions.get(i);

        var currentVertex = createVertex();
        currentVertex.setPosition(currentPosition);

        try {
          createWallImpl(previousVertex.id(), currentVertex.id());
        } catch (GraphException e) {
          throw new RuntimeException(
              "Unexpected scenario: wall already exists despite vertex being new", e);
        }

        previousVertex = currentVertex;
      }
    }
  }

  // Vertices are identified by GUID. Attached data is maintained in vertexInternalById
  private final Graph<GUID, WallInternal> graph;
  private final Map<GUID, VertexInternal> vertexInternalById = new LinkedHashMap<>();
  private transient int nextZIndex = 0;

  public WallTopology() {
    this.graph =
        GraphTypeBuilder.<GUID, WallInternal>undirected()
            .allowingMultipleEdges(false)
            .allowingSelfLoops(false)
            .weighted(false)
            .buildGraph();
  }

  public WallTopology(WallTopology other) {
    this();

    other
        .getVertices()
        .sorted(Comparator.comparingInt(Vertex::getZIndex))
        .forEach(
            vertex -> {
              VertexInternal newVertexInternal;
              try {
                newVertexInternal = createVertexImpl(vertex.id());
              } catch (GraphException e) {
                log.error(
                    "Unexpected scenario: graph has duplicate vertex ID {}. Skipping.",
                    vertex.id(),
                    e);
                return;
              }
              newVertexInternal.position.setLocation(vertex.getPosition());
            });

    other
        .getWalls()
        .sorted(Comparator.comparingInt(Wall::getZIndex))
        .forEach(
            wall -> {
              try {
                createWallImpl(wall.from().id(), wall.to().id());
              } catch (GraphException e) {
                log.error(
                    "Unexpected scenario: topology has duplicate wall ({}, {}). Skipping.",
                    wall.from().id(),
                    wall.to().id(),
                    e);
              }
            });
  }

  private VertexInternal createVertexImpl(GUID id) throws GraphException {
    var added = graph.addVertex(id);
    if (!added) {
      throw new GraphException("Vertex with that GUID is already in the graph", null);
    }

    // Because we store the vertices in a LinkedHashMap, they will always be iterated in insertion
    // order. We also include a z-index so that other components can put vertices in other data
    // structures and sort them again by z-order.
    var data = new VertexInternal(id, new Point2D.Double());
    data.zIndex(++nextZIndex);
    var previous = vertexInternalById.put(id, data);
    assert previous == null : "Invariant not held";

    return data;
  }

  private WallInternal createWallImpl(GUID fromId, GUID toId) throws GraphException {
    var wall = new WallInternal(fromId, toId);

    boolean added;
    try {
      added = graph.addEdge(fromId, toId, wall);
    } catch (IllegalArgumentException e) {
      throw new GraphException("One of the vertices does not exist in the graph", e);
    }

    if (!added) {
      throw new GraphException("Wall with those vertices is already in the graph", null);
    }
    return wall;
  }

  // region These exist to support serialization. They are little loose for general use.

  /**
   * Adds a new vertex to the graph.
   *
   * @param id The ID of the vertex
   * @throws GraphException If a vertex with ID {@code id} already exists.
   */
  public Vertex createVertex(GUID id) throws GraphException {
    var vertexInternal = createVertexImpl(id);
    return new Vertex(id, vertexInternal);
  }

  public void createWall(GUID fromId, GUID toId) throws GraphException {
    createWallImpl(fromId, toId);
  }

  // endregion

  // region Other builder methods

  private Vertex createVertex() {
    var id = new GUID();
    VertexInternal vertexInternal;
    try {
      vertexInternal = createVertexImpl(id);
    } catch (GraphException e) {
      throw new RuntimeException("Impossible case: vertex already exists with new GUID", e);
    }
    return new Vertex(id, vertexInternal);
  }

  public void string(Point2D startingPoint, Consumer<StringBuilder> action) {
    var builder = new StringBuilder(startingPoint);
    action.accept(builder);
    builder.build();
  }

  // endregion

  // region Main interface

  public Stream<Vertex> getVertices() {
    return this.graph.vertexSet().stream()
        .map(
            id -> {
              var vertexInternal = vertexInternalById.get(id);
              return new Vertex(id, vertexInternal);
            });
  }

  public Stream<Wall> getWalls() {
    return this.graph.edgeSet().stream()
        .map(
            wall -> {
              var fromId = wall.from();
              var toId = wall.to();
              var fromInternal = vertexInternalById.get(fromId);
              var toInternal = vertexInternalById.get(toId);
              return new Wall(new Vertex(fromId, fromInternal), new Vertex(toId, toInternal));
            });
  }

  public void removeVertex(Vertex vertex) {
    if (this != vertex.getOriginator()) {
      throw new RuntimeException("Foreign vertex");
    }

    graph.removeVertex(vertex.id());
    vertexInternalById.remove(vertex.id());

    removeDanglingVertices();
  }

  public void removeWall(Wall wall) {
    if (this != wall.getOriginator()) {
      throw new RuntimeException("Wall is not part of this topology");
    }

    var removedWall = graph.removeEdge(wall.from().id(), wall.to().id());
    if (removedWall != null) {
      removeDanglingVertices();
    }
  }

  public Wall brandNewWall() {
    var from = createVertex();
    var to = createVertex();

    WallInternal newWall;
    try {
      newWall = createWallImpl(from.id(), to.id());
    } catch (GraphException e) {
      throw new RuntimeException(
          "Unexpected scenario: wall already exists despite both vertices being new", e);
    }

    return new Wall(from, to);
  }

  public Wall newWallStartingAt(Vertex from) {
    if (this != from.getOriginator()) {
      throw new RuntimeException("Vertex is not part of this topology");
    }

    var newVertex = createVertex();

    WallInternal newWall;
    try {
      newWall = createWallImpl(from.id(), newVertex.id());
    } catch (GraphException e) {
      throw new RuntimeException(
          "Unexpected scenario: wall already exists despite the one vertex being new", e);
    }

    return new Wall(from, newVertex);
  }

  public Vertex splitWall(Wall wall) {
    if (this != wall.getOriginator()) {
      throw new RuntimeException("Wall is not part of this topology");
    }

    // Remove wall and replace with two new walls connected through a new vertex.

    var removed = graph.removeEdge(wall.from().id(), wall.to().id());
    if (removed == null) {
      throw new RuntimeException("Wall does not exist");
    }
    // Don't trust the contents of `wall`, instead use `removed`.

    var newVertex = createVertex();

    try {
      createWallImpl(removed.from(), newVertex.id());
      createWallImpl(newVertex.id(), removed.to());
    } catch (GraphException e) {
      throw new RuntimeException("Unexpected scenario: wall already exists for new vertex", e);
    }

    return newVertex;
  }

  /**
   * Merge {@code first} with {@code second}.
   *
   * <p>How this merger is done is up to the implementation. Either {@code first} or {@code second}
   * may remaining in the graph while the other is removed, or both may be removed and replaced with
   * a new vertex. Callers should use the return value if the new or surviving vertex is required.
   *
   * @param first The vertex to remove by merging.
   * @param second The vertex to augment by merging {@code remove} into it.
   * @return The merged vertex.
   */
  public Vertex merge(Vertex first, Vertex second) {
    if (this != first.getOriginator()) {
      throw new RuntimeException("Foreign vertex");
    }
    if (this != second.getOriginator()) {
      throw new RuntimeException("Foreign vertex");
    }

    // Current implementation is to remove `first` and keep `second.

    // A copy is essential since graph.edgesOf() returns a live set.
    var incidentWalls = new ArrayList<>(graph.edgesOf(first.id()));

    var wasRemoved = graph.removeVertex(first.id());
    if (!wasRemoved) {
      // Psych! The vertex does not exist.
      throw new RuntimeException("Unable unable merge vertex that does not exist!");
    }

    // Anything that used to point to or from `first` now has to point to or from `second`.
    for (var oldWall : incidentWalls) {
      var firstIsSource = oldWall.from().equals(first.id());
      var neighbour = firstIsSource ? oldWall.to() : oldWall.from();

      // Three cases:
      // 1. `neighbour` is `second`. Drop such contracted walls.
      // 2. A wall already exists from `neighbour` to `second`. Merge into the existing wall.
      // 3. No wall exists from `neighbour` to `second`. Copy the wall over.

      if (neighbour.equals(second.id())) {
        // The wall was contracted. Drop it as redundant.
        continue;
      }

      var existingWall = graph.getEdge(second.id(), neighbour); // Order doesn't matter.
      if (existingWall == null) {
        // Simply duplicate the wall for `second` instead of `first`.
        try {
          var from = firstIsSource ? second.id() : neighbour;
          var to = firstIsSource ? neighbour : second.id();
          createWallImpl(from, to);
        } catch (GraphException e) {
          throw new RuntimeException(
              "Unexpected scenario: wall already exists despite not existing", e);
        }
      } else {
        // TODO When walls have data to merge, do it here, accounting for whether the walls point
        //  in the same or opposite direction.
        // Merge obsolete wall into the kept wall.
        var secondIsSource = existingWall.from().equals(second.id());
        var reversed = firstIsSource != secondIsSource;
      }
    }

    removeDanglingVertices();

    return second;
  }

  // endregion

  /**
   * Ensures that any vertices with no walls are removed from the graph.
   *
   * <p>This is usually called automatically when needed, so don't call this method without reason.
   * Deserializing is an exception where improper XML could result in vertices being added without
   * associated walls.
   */
  public void removeDanglingVertices() {
    var verticesToRemove = new ArrayList<GUID>();
    for (var vertex : graph.vertexSet()) {
      if (graph.edgesOf(vertex).isEmpty()) {
        verticesToRemove.add(vertex);
      }
    }
    graph.removeAllVertices(verticesToRemove);

    for (var vertexId : verticesToRemove) {
      vertexInternalById.remove(vertexId);
    }
  }

  @Override
  public VisionResult addSegments(Coordinate origin, Envelope bounds, Consumer<Coordinate[]> sink) {
    getWalls()
        .forEach(
            wall -> {
              var segment = wall.asSegment();
              // Very rough bounds check so we don't include everything.
              if (bounds.intersects(segment.p0, segment.p1)) {
                sink.accept(new Coordinate[] {segment.p0, segment.p1});
              }
            });
    return VisionResult.Possible;
  }

  public WallTopologyDto toDto() {
    var builder = WallTopologyDto.newBuilder();
    for (var vertex : this.vertexInternalById.values()) {
      builder.addVertices(
          VertexDto.newBuilder()
              .setId(vertex.id().toString())
              .setPosition(
                  DoublePointDto.newBuilder()
                      .setX(vertex.position().getX())
                      .setY(vertex.position().getY())));
    }
    for (var wall : this.graph.edgeSet()) {
      // No wall data to send yet.
      builder.addWalls(
          WallDto.newBuilder().setFrom(wall.from().toString()).setTo(wall.to().toString()));
    }
    return builder.build();
  }

  public static WallTopology fromDto(WallTopologyDto dto) {
    var topology = new WallTopology();
    for (var vertexDto : dto.getVerticesList()) {
      topology.createVertex();
      Vertex vertex;
      try {
        vertex = topology.createVertex(new GUID(vertexDto.getId()));
      } catch (GraphException e) {
        log.error("Unexpected error while adding vertex to graph", e);
        continue;
      }

      vertex.setPosition(vertexDto.getPosition().getX(), vertexDto.getPosition().getY());
    }
    for (var wallDto : dto.getWallsList()) {
      try {
        topology.createWall(new GUID(wallDto.getFrom()), new GUID(wallDto.getTo()));
      } catch (GraphException e) {
        log.error("Unexpected error while adding wall to topology", e);
      }
    }
    return topology;
  }
}
