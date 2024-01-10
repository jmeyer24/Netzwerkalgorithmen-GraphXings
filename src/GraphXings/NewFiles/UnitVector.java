package GraphXings.NewFiles;

public class UnitVector {
    private double x;
    private double y;
    private double length;

    public UnitVector(double x, double y) {
        this.length = Math.sqrt(x * x + y * y);
        this.x = x / this.length;
        this.y = y / this.length;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }
}
