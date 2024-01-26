package GraphXings.Gruppe4.Common;

import GraphXings.Data.Coordinate;
import GraphXings.Data.Segment;

public class FastSegment extends Segment {

    /**
     * Constructs a segment between the two given coordinates.
     *
     * @param start The first coordinate.
     * @param end   The second coordinate.
     */
    public FastSegment(Coordinate start, Coordinate end) {
        super(start, end);
    }


    public static double squaredCosineOfAngle(Segment s1, Segment s2)
    {
        if (s1.isVertical() && s2.isVertical())
        {
            return 1;
        }
        if (s2.isVertical())
        {
            Segment swap = s1;
            s1 = s2;
            s2 = swap;
        }
        double ax;
        double ay;
        if (s1.isVertical())
        {
            ax = 0;
            ay = 1;
        }
        else
        {
            ax = s1.getA().getQ();
            ay = s1.getA().getP();
        }

        double bx = s2.getA().getQ();
        double by = s2.getA().getP();
        double a = ax * bx + ay * by;
        double x1 = ax * ax + ay * ay;
        double x2 = bx * bx + by * by;
        double sqrts = q_rsqrt((float)x1) * q_rsqrt((float)x2);
        double frac = a * sqrts;
        return frac * frac;

        //return Math.pow((ax * bx +  ay * by)/(Math.sqrt(Math.pow(ax,2)+Math.pow(ay,2))*Math.sqrt(Math.pow(bx,2)+Math.pow(by,2))),2);
    }

    /**
     * Good old Quake hack to calculate the inverse square root
     * @param number
     * @return
     */
    public static float q_rsqrt(float number) {
        float xhalf = 0.5f * number;
        int i = Float.floatToIntBits(number);
        i = 0x5f3759df - (i >> 1);                  // what the fuck?
        number = Float.intBitsToFloat(i);
        number *= (1.5f - xhalf * number * number); // 1st iteration
        //number *= (1.5f - xhalf * number * number); // 2nd iteration

        return number;
    }



}
