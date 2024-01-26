package GraphXings.Gruppe4.Strategies;

import GraphXings.Data.Coordinate;
import GraphXings.Data.Edge;
import GraphXings.Data.Graph;
import GraphXings.Data.Vertex;
import GraphXings.Game.GameMove;
import GraphXings.Game.GameState;
import GraphXings.Gruppe4.CanvasObservations.SampleParameters;
import GraphXings.Gruppe4.Common.Helper;
import GraphXings.Gruppe4.MutableRTree;
import GraphXings.Gruppe4.StrategiesStopWatch;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MinimizePlaceAtBorder extends StrategyClass {

    private int topBorder;
    private int bottomBorder;
    private int rightBorder;

    private int leftBorder;

    private int border = 0;

    public MinimizePlaceAtBorder(Graph g, GameState gs, MutableRTree<Edge, LineFloat> tree, int width, int height, SampleParameters sampleParameters, StrategiesStopWatch strategiesStopWatch) {
        super(g, gs, tree,  width, height, sampleParameters, strategiesStopWatch.getWatch(StrategyName.MinimizePlaceAtBorder));
        moveQuality = Long.MAX_VALUE;
        topBorder = 0;
        bottomBorder = height-1;
        rightBorder = width-1;
        leftBorder = 0;
    }



    /**
     * Execute the main strategy and calculate a game move.
     * The move must be stored and retrievable by getGameMove.
     *
     * Strategy: search for a vertex at the border and place its neighbour next to it
     * (use the move with the lowest number of crossings)
     * if this is not possible use a free vertex with at least one free neighbour an place it at the border
     * if the border is full: new border goes one coordinate inside
     *
     * @param lastMove The last opponent move.
     * @return True on success, false otherwise
     */
    @Override
    public boolean executeStrategy(GameMove lastMove) {
        super.startExecuteStrategy();

        var vertexCoordinates = gs.getVertexCoordinates();
        var placedVertices = gs.getPlacedVertices();
        var usedCoordinates = gs.getUsedCoordinates();

        //find candidate coordinates (all coordinates at the border)
        var freeCoordinatesAtBorder = Helper.findFreeCoordinatesAtBorder(usedCoordinates, rightBorder, leftBorder, topBorder, bottomBorder, sampleParameters.samples());
        while (freeCoordinatesAtBorder.isEmpty()){
            freeCoordinatesAtBorder = Helper.findFreeCoordinatesAtBorder(usedCoordinates, --rightBorder, ++leftBorder, ++topBorder, --bottomBorder, sampleParameters.samples());
        }

        for (var vertex : placedVertices){
            var neighbourVertex = Helper.pickIncidentVertex(g, vertexCoordinates, vertex);
            var vertexCoordinate = vertexCoordinates.get(vertex);
            //check for the vertex, if it is at the border and if it has an unplaced neighbour and if there is a free coordinate at the border
            var coordinate_neighbour = findFreeNeighbourCoordinate(vertexCoordinate);
            if (Helper.isAtBorder(vertexCoordinate, rightBorder, leftBorder, topBorder, bottomBorder) && neighbourVertex.isPresent() && coordinate_neighbour.isPresent()){
                //compute intersections for the new optional move to check the quality
                long current_move_quality = computeMoveQuality(neighbourVertex.get(), coordinate_neighbour.get().get(0));

                //only update game move, if the quality of the optional current move is better than the quality of the best move
                if (this.moveQuality > current_move_quality){
                    gameMove = Optional.of(new GameMove(neighbourVertex.get(), coordinate_neighbour.get().get(0)));
                    this.moveQuality = current_move_quality;
                }
            }
            //move with the best quality was found
            if (moveQuality == 0){
                return true;
            }
        }

        //take unused vertex with at least 1 free neighbour and put it on the free coordinate on the border

        var freeCoordinateAtBorder = freeCoordinatesAtBorder.get().get(0);
        for (Vertex vertex : g.getVertices()){
            if (!placedVertices.contains(vertex) && Helper.numIncidentVertices(g, gs, vertex, true) >= 1){
                long current_move_quality = computeMoveQuality(vertex, freeCoordinateAtBorder);
                if (moveQuality > current_move_quality){
                    gameMove = Optional.of(new GameMove(vertex, freeCoordinateAtBorder));
                    moveQuality = current_move_quality;
                }
            }
            if (moveQuality == 0){
                return true;
            }
        }

        super.stopExecuteStrategy();

        return gameMove.isPresent();
    }


    /**
     * This should return a fixed strategy name
     * which is used by the GameObserver.
     *
     * @return A strategy name
     */
    @Override
    public StrategyName getStrategyName() {
        return StrategyName.MinimizePlaceAtBorder;
    }

    public Optional<List<Coordinate>> filterCoordinates(Rectangle region, List<Coordinate> coordinateList){

        coordinateList.removeIf(coordinate -> coordinate.getX() < region.x1() || coordinate.getX() > region.x2() || coordinate.getY() < region.y1() || coordinate.getY() > region.y2());
        return Optional.of(coordinateList);
    }

    /*public boolean executeStrategy(GameMove lastMove){
        var vertexCoordinates = gs.getVertexCoordinates();
        var placedVertices = gs.getPlacedVertices();
        var usedCoordinates = gs.getUsedCoordinates();

        //find candidate coordinates (all coordinates at the border)
        var freeCoordinatesAtBorder = Helper.findFreeCoordinatesAtBorder(usedCoordinates, rightBorder, leftBorder, topBorder, bottomBorder);
        while (freeCoordinatesAtBorder.isEmpty()){
            freeCoordinatesAtBorder = Helper.findFreeCoordinatesAtBorder(usedCoordinates, --rightBorder, ++leftBorder, ++topBorder, --bottomBorder);
        }

        //find candidate vertices (neighbours of all vertices at the border)
        for (Vertex vertex : placedVertices){
            var neighbourVertices = Helper.getAllNeighbourVertices(g, gs, vertex);
            var coordinate_vertex = vertexCoordinates.get(vertex);
            var neighbourCoordinates = findFreeNeighbourCoordinate(coordinate_vertex);

            //check for each vertex if it is at the border and if it has a free neighbour
            if (Helper.isAtBorder(coordinate_vertex, rightBorder, leftBorder, topBorder, bottomBorder) && neighbourVertices.isPresent() && neighbourCoordinates.isPresent()){

                //var move_lowestIntersection = chooseLowestIntersection(neighbourVertices.get(), neighbourCoordinates.get());
                //var move_lowestEdgeLength = minimizeEdgeLengths(neighbourVertices.get(), neighbourCoordinates.get());

                gameMove = chooseLowestIntersection(neighbourVertices.get(), neighbourCoordinates.get());
            }

            if (moveQuality == 0){
                return true;
            }
        }


        List<Vertex> sampleVertices = new ArrayList<>();
        /*var lowestDensity = tree.findLowestDensity(5);
        List<Coordinate> sampleCoordinates;
        if (lowestDensity.isPresent()){
            sampleCoordinates = filterCoordinates(lowestDensity.get(), freeCoordinatesAtBorder.get()).get();

        } else {
            sampleCoordinates = List.of(freeCoordinatesAtBorder.get().get(0));
        }
        for (Vertex vertex : g.getVertices()){
            if (!placedVertices.contains(vertex) && Helper.numIncidentVertices(g, gs, vertex, true) >= 1){
                sampleVertices.add(vertex);
            }
        }
        gameMove = chooseLowestIntersection(sampleVertices, List.of(freeCoordinatesAtBorder.get().get(0)));


        return gameMove.isPresent();
    }*/


    private Optional<List<Coordinate>> findFreeNeighbourCoordinate(Coordinate coordinate){

        var usedCoordinates = gs.getUsedCoordinates();
        int xValue_old = coordinate.getX();
        int yValue_old = coordinate.getY();

        List<Coordinate> freeNeighbourCoordinates = new ArrayList<>();

        //coordinate is at top or bottom border
        if(yValue_old == topBorder || yValue_old == bottomBorder) {
            //Check left field
            if (xValue_old < rightBorder && Helper.isCoordinateFree(usedCoordinates, xValue_old + 1, yValue_old)) {
                freeNeighbourCoordinates.add(new Coordinate(xValue_old + 1, yValue_old));
                //return Optional.of(new Coordinate(xValue_old + 1, yValue_old));
            }

            //check right field
            if (xValue_old > leftBorder && Helper.isCoordinateFree(usedCoordinates, xValue_old - 1, yValue_old)) {
                freeNeighbourCoordinates.add(new Coordinate(xValue_old - 1, yValue_old));
                //return Optional.of(new Coordinate(xValue_old - 1, yValue_old));
            }
        }

        //coordinate is at left/right border
        if (xValue_old == leftBorder || xValue_old == rightBorder){
            //check field under
            if (yValue_old < bottomBorder && Helper.isCoordinateFree(usedCoordinates, xValue_old, yValue_old+1)){
                freeNeighbourCoordinates.add(new Coordinate(xValue_old, yValue_old+1));
                //return Optional.of(new Coordinate(xValue_old, yValue_old+1));
            }
            //check field above
            if (yValue_old > topBorder && Helper.isCoordinateFree(usedCoordinates, xValue_old, yValue_old-1)){
                freeNeighbourCoordinates.add(new Coordinate(xValue_old, yValue_old-1));
                //return Optional.of(new Coordinate(xValue_old, yValue_old-1));
            }
        }
        if (freeNeighbourCoordinates.isEmpty()){
            return Optional.empty();
        } else {
            return Optional.of(freeNeighbourCoordinates);
        }
    }




    /**
     * finds the next free coordinate at the border from the given coordinate
     * @param coordinate
     * @return next free coordinate
     */
    private Optional<Coordinate> findNextFreeCoordinateAtBorder (Coordinate coordinate){
        int height = this.height - border;
        int width = this.width - border;
        int xValue_old = coordinate.getX();
        int yValue_old = coordinate.getY();
        int i = 1;
        if(yValue_old == border || yValue_old == height-1){
            //coordinate is at top or bottom border

            while (xValue_old + i < width-1 || xValue_old - i > border) {
                //check field to the right side
                int xValue_new = xValue_old + i;
                if (xValue_new < width-1 && Helper.isCoordinateFree(gs.getUsedCoordinates(), xValue_new, yValue_old)){
                    return Optional.of(new Coordinate(xValue_new, yValue_old));
                }

                //check field to the left side
                xValue_new = xValue_old - i;
                if (xValue_new > border && Helper.isCoordinateFree(gs.getUsedCoordinates(), xValue_new, yValue_old)){
                    return Optional.of(new Coordinate(xValue_new, yValue_old));
                }
                i++;
            }

        } else {
           //coordinate is at left or right border
            while (yValue_old + i < height-1 || yValue_old - i > border){
                //check field above
                int yValue_new = yValue_old + i;
                if (yValue_new < height-1 && Helper.isCoordinateFree(gs.getUsedCoordinates(), xValue_old, yValue_new)){
                    return Optional.of(new Coordinate(xValue_old, yValue_new));
                }

                //check field under
                yValue_new = yValue_old - i;
                if (yValue_new > border && Helper.isCoordinateFree(gs.getUsedCoordinates(), xValue_old, yValue_new)){
                    return Optional.of(new Coordinate(xValue_old, yValue_new));
                }

                i++;
            }
        }

        // given coordinate is not at the border or no next coordinate is free
        return Optional.empty();
    }





}
