package greedy.components;

public class Point {
  public double x;
  public double y;

  public Point(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public static double twoPointsDistance(Point point1, Point point2){
    double theta = point1.x - point2.x;
    double dist = Math.sin(deg2rad(point1.y)) * Math.sin(deg2rad(point2.y)) + Math.cos(deg2rad(point1.y)) * Math.cos(deg2rad(point2.y)) * Math.cos(deg2rad(theta));
    dist = Math.acos(dist);
    dist = rad2deg(dist);
    dist = dist * 60 * 1.1515;
    dist = dist * 1.609344 * 1000;

    return (dist);
  }

  private static double deg2rad(double deg) {
    return (deg * Math.PI / 180.0);
  }

  private static double rad2deg(double rad) {
    return (rad * 180.0 / Math.PI);
  }
}
