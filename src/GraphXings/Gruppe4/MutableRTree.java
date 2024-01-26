package GraphXings.Gruppe4;

import GraphXings.Data.*;
import GraphXings.Gruppe4.Common.FastSegment;
import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.Iterables;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Line;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.github.davidmoten.rtree2.geometry.internal.LineFloat;
import com.github.davidmoten.rtree2.geometry.internal.RectangleFloat;

import java.util.*;
import java.util.stream.StreamSupport;

public class MutableRTree<T, S extends Geometry> {

    private RTree<T, S> tree;
    private TreeSetup setup;
    private int width = 0;
    private int height = 0;

    public enum TreeSetup {
        // Less than 10k entries
        SMALL,

        // Greater than 10k entries
        BIG
    }

    public MutableRTree(TreeSetup size, int width, int height) {
        setup = size;
        initRTree(width, height);
    }

    private void initRTree(int width, int height) {
        this.width = width;
        this.height = height;
        if (setup == TreeSetup.SMALL) {
            tree = RTree.maxChildren(4).create();
        } else {
            tree = RTree.star().maxChildren(6).create();
        }
    }

    public MutableRTree() {
        // Use a R-Tree by default
        tree = RTree.maxChildren(4).create();
    }

    public void add(T value, S geometry) {
        tree = tree.add(value, geometry);
    }

    public void add(Entry<? extends T, ? extends S> entry) {
        tree = tree.add(entry);
    }

    public void addAll(List<Entry<T, S>> entries) {
        tree = tree.add(entries);
    }

    public RTree<T, S> get() {
        return tree;
    }

    public void reset(int width, int height) {
        initRTree(width, height);
    }

    public long getIntersections(S geometry) {
        // retrieve the minimal bounding box from the geometry
        var rectBB = geometry.mbr();

        // Get all potential intersections with the given bounding box
        Iterable<Entry<T, S>> potentialIntersections = tree.search(rectBB);

        // Count all elements
        return Iterables.size(potentialIntersections);
    }

    /**
     * NOT GENERIC: THIS DOES ONLY WORK WITH EDGE TREES!
     * Calculate the crossing angle of intersecting edges
     * @param line The line which should be checked for intersections
     * @return Angle
     */
    public double computeSumOfCrossingAngles(LineFloat line) {
        // retrieve the minimal bounding box from the geometry
        var rectBB = line.mbr();

        // Get all potential intersections with the given bounding box
        Iterable<Entry<T, S>> potentialIntersections = tree.search(rectBB);

        // Use the crossing calculator on this reduced set of edges
        double result = 0;
        for (var entry1 : potentialIntersections) {
            Edge e1 = (Edge) entry1.value();
            LineFloat l1 = (LineFloat) entry1.geometry();
            for (var entry2 : potentialIntersections) {
                Edge e2 = (Edge) entry2.value();
                LineFloat l2 = (LineFloat) entry2.geometry();
                if (e1.equals(e2)) {
                    continue;
                }
                if (e1.isAdjacent(e2)) {
                    continue;
                }

                FastSegment s1 = new FastSegment(new Coordinate((int) l1.x1(), (int) l1.y1()), new Coordinate((int) l1.x2(), (int) l1.y2()));
                FastSegment s2 = new FastSegment(new Coordinate((int) l2.x1(), (int) l2.y1()), new Coordinate((int) l2.x2(), (int) l2.y2()));
                // TODO: Can we use l1.intersects(l2) instead?
                if (FastSegment.intersect(s1,s2))
                {
                    result += FastSegment.squaredCosineOfAngle(s1,s2);
                }
            }
        }

        return result;
    }

    public Optional<Rectangle> findHighestDensity(int tiling) {
        int w = width / tiling;
        int h = height / tiling;

        Rectangle highestDensity = null;
        long maxCrossings = Long.MIN_VALUE;
        for (int i = 0; i < tiling - 1; i++) {
            for (int k = 0; k < tiling - 1; k++) {
                var rect = RectangleFloat.create(i * w, k * h, (i + 1) * w, (k + 1) * h);
                var crossings = Iterables.size(tree.search(rect));
                if (crossings > maxCrossings) {
                    highestDensity = rect;
                    maxCrossings = crossings;
                }
            }
        }

        if (highestDensity == null || maxCrossings <= 0) {
            return Optional.empty();
        }
        return Optional.of(highestDensity);
    }

    public Optional<Rectangle> findLowestDensity(int tiling) {
        int w = width / tiling;
        int h = height / tiling;

        Rectangle lowestDensity = null;
        long maxCrossings = Long.MAX_VALUE;
        for (int i = 0; i < tiling - 1; i++) {
            for (int k = 0; k < tiling - 1; k++) {
                var rect = RectangleFloat.create(i * w, k * h, (i + 1) * w, (k + 1) * h);
                var crossings = Iterables.size(tree.search(rect));
                if (crossings < maxCrossings) {
                    lowestDensity = rect;
                    maxCrossings = crossings;
                }
            }
        }

        if (lowestDensity == null || maxCrossings <= 0) {
            return Optional.empty();
        }
        return Optional.of(lowestDensity);
    }

    /**
     * Retrieve elements T (edges/vertices) from tree structure
     * by querying for a geometry.
     * @param geometry Geometry like a rectangle or line.
     * @return Iterator with the resulting entries.
     */
    public Iterable<Entry<T, S>> getElementsFromGeometry(Geometry geometry) {
        var rect = geometry.mbr();

        return tree.search(rect);
    }

}
