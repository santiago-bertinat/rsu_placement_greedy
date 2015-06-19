package greedy.components;

import java.util.*;

import greedy.components.Point;
import greedy.components.Rsu;

public class Segment implements Comparable<Segment> {
  public Point start;
  public Point end;
  public int vehicles_amount;
  public int speed;
  public double qos_potenciality;
  public Rsu rsu;

  public Segment(Point start, Point end, int vehicles_amount, int speed) {
    this.start = start;
    this.end = end;
    this.vehicles_amount = vehicles_amount;
    this.speed = speed;

    this.qos_potenciality = vehicles_amount * distance() / (speed * 1000);
  }

  public double distance() {
    return Point.twoPointsDistance(start, end);
  }

  public int compareTo(Segment segment) {
    if (segment.qos_potenciality > qos_potenciality) return -1;
    if (segment.qos_potenciality == qos_potenciality) return 0;
    return 1;
  }

  public void print() {
    System.out.println("#####");

    System.out.print("start x: ");
    System.out.println(start.x);
    System.out.print("start y: ");
    System.out.println(start.y);
    System.out.print("end x: ");
    System.out.println(end.x);
    System.out.print("end y: ");
    System.out.println(end.y);
  }
}
