package greedy;

import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;

import greedy.components.Rsu;
import greedy.components.RsuType;
import greedy.components.Segment;
import greedy.components.Point;

public class Greedy {
  public static int coordinates_amount = 121;
  public static ArrayList<Segment> segments = new ArrayList<Segment>();
  public static ArrayList<Rsu> road_side_units = new ArrayList<Rsu>();
  public static ArrayList<RsuType> rsu_types = new ArrayList<RsuType>();
  public static double qos = 0;
  public static double coverage_acceptance = 0.8;

  public static void main(String[] args) {
    loadSegments();
    loadRsuTypes();

    // Ascending order
    Collections.sort(segments);

    ArrayList<Segment> intersected_segments =  new ArrayList<Segment>();
    for (int i = segments.size() - 1; i >= 0; i--) {
      Segment segment = segments.get(i);

      if (!intersected_segments.contains(segment)) {
        Random generator = new Random();
        double rsu_position = generator.nextDouble();

        Point rsu_center = rsuPosition(segment, rsu_position);
        Rsu rsu = new Rsu(rsu_center, rsu_types.get(rsu_types.size() - 1));
        road_side_units.add(rsu);
        segment.rsu = rsu;

        intersected_segments = findIntersections();
      }
    }

    saveResults("greedy/qos_results.txt");

    System.out.println("RESULT: ");
    System.out.println(qos);
    System.out.println("COST: ");
    System.out.println(String.valueOf(road_side_units.size() * 227.50));

    double qos_found = qos;

    while (qos > qos_found * 0.9) {
      double[] qos_loss = new double[segments.size()];
      double old_qos = qos;

      // System.out.println("QOS LOSS:");
      for (int i = 0; i < segments.size(); i++) {
        Segment segment = segments.get(i);

        if (segment.rsu == null || segment.rsu.rsu_type == null) {
          qos_loss[i] = 20000;
        }else {
          RsuType rsu_type = segment.rsu.rsu_type;
          int index_of_type = rsu_types.indexOf(rsu_type);
          segment.rsu.setRsuType(index_of_type <= 1 ? null : rsu_types.get(index_of_type - 1));

          findIntersections();

          qos_loss[i] = old_qos - qos;
          segment.rsu.setRsuType(rsu_type);
        }

        // System.out.println(qos_loss[i]);
      }

      int min = 0;
      for (int i = 1; i < segments.size(); i++) {
        if (qos_loss[i] < qos_loss[min]) {
          min = i;
        }
      }

      // System.out.print("MINIMO QOS:");
      // System.out.println(min);
      // System.out.println(qos_loss[min]);

      Segment segment = segments.get(min);
      RsuType rsu_type = segment.rsu.rsu_type;
      int index_of_type = rsu_types.indexOf(rsu_type);
      // System.out.print("type:");
      // System.out.println(index_of_type);

      if (index_of_type <= 1) {
        road_side_units.remove(segment.rsu);
        segment.rsu = null;
      }else {
        segment.rsu.setRsuType(rsu_types.get(index_of_type - 1));
      }

      findIntersections();

      // System.out.println("QOS: ");
      // System.out.println(qos);
    }

    System.out.println("RESULT: ");
    System.out.println(qos);
    System.out.println("COST: ");
    double cost = 0;
    for (Rsu rsu : road_side_units) {
      if (rsu.rsu_type != null)
        cost += rsu.rsu_type.cost;
    }
    System.out.println(cost);

    saveResults("greedy/cost_results.txt");
  }

  private static void saveResults(String file_location) {
    try {
      File file = new File(file_location);

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter file_writer = new FileWriter(file.getAbsoluteFile());
      BufferedWriter buffer = new BufferedWriter(file_writer);
      for (Rsu rsu : road_side_units) {
        buffer.write(String.valueOf(rsu.center.x) + ',');
        buffer.write(String.valueOf(rsu.center.y) + ',');
        buffer.write(String.valueOf(rsu.radius) + '\n');
      }

      buffer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static Point rsuPosition(Segment segment, double rsu_position) {
    double lat_ini = segment.start.x;
    double lng_ini = segment.start.y;
    double lat_fin = segment.end.x;
    double lng_fin = segment.end.y;

    double a =(lng_fin-lng_ini)/(double)(lat_fin-lat_ini);
    double x;
    double y;

    if (lat_fin>lat_ini)
      x = lat_ini + Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*rsu_position/(double)Math.sqrt(1+(a*a));
    else
      x = lat_ini - Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*rsu_position/(double)Math.sqrt(1+(a*a));

    if (lng_fin>lng_ini)
      y = lng_ini + Math.abs(Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*a*rsu_position/(double)Math.sqrt(1+(a*a)));
    else
      y = lng_ini - Math.abs(Math.sqrt((lng_fin-lng_ini)*(lng_fin-lng_ini) + (lat_fin-lat_ini)*(lat_fin-lat_ini))*a*rsu_position/(double)Math.sqrt(1+(a*a)));

    return new Point(x, y);
  }

  private static ArrayList<Segment> findIntersections() {
    ArrayList<Segment> intersected_segments = new ArrayList<Segment>();
    qos = 0;


    // System.out.println("QOS:");
    for (Segment segment : segments){

      double divitions = 10;
      double module_section = segment.distance() / divitions;
      double intersections = 0;

      double coverered_distance = 0;

      //System.out.println("SEGMENT");
      //segment.print();

      double x_length = Math.abs(segment.start.x - segment.end.x) / divitions;
      double y_length = Math.abs(segment.start.y - segment.end.y) / divitions;

      for (int j = 0; j < divitions; j++) {
        double x = segment.start.x;
        if (segment.start.x < segment.end.x) {
          x = segment.start.x + j * x_length;
        }else if (segment.start.x > segment.end.x) {
          x = segment.start.x - j * x_length;
        }

        double y = segment.start.y;
        if (segment.start.y < segment.end.y) {
          y = segment.start.y + j * y_length;
        }else if (segment.start.y > segment.end.y) {
          y = segment.start.y - j * y_length;
        }

        Point aux_point = new Point(x, y);
        for (Rsu rsu : road_side_units) {
          if (rsu.belongsToCircle(aux_point)) {
            intersections++;
            break;
          }
        }
      }

      if (intersections > divitions * coverage_acceptance) {
        // Segment is covered above 80%
        intersected_segments.add(segment);
      }

      coverered_distance = intersections * module_section;
      // System.out.println(intersections);
      // System.out.println(segment.distance());
      qos += segment.vehicles_amount * (coverered_distance) / (segment.speed * 1000);
    }
    // System.out.println("#######");
    return intersected_segments;
  }

  private static void loadSegments() {
    Point[] coordinates = new Point[coordinates_amount];
    double ideal_qos = 0;

    try{
      // Load coordinates
      File file = new File("greedy/coordinates.txt");
      FileInputStream input_stream = new FileInputStream(file);
      BufferedReader buffer = new BufferedReader(new InputStreamReader(input_stream));

      String line = null;
      String [] line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
        line_tokens = line.split(" ");

        Point point = new Point(Double.parseDouble(line_tokens[1]), Double.parseDouble(line_tokens[2]));
        coordinates[Integer.parseInt(line_tokens[0])] = point;
      }
      buffer.close();

      // Load segments
      file = new File("greedy/instances/normal.txt");
      input_stream = new FileInputStream(file);
      buffer = new BufferedReader(new InputStreamReader(input_stream));

      line = null;
      line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
          line_tokens = line.split(" ");

          Point start = coordinates[Integer.parseInt(line_tokens[0])];
          Point end = coordinates[Integer.parseInt(line_tokens[1])];
          int vehicles_amount = Integer.parseInt(line_tokens[2]);
          int speed = Integer.parseInt(line_tokens[3]);

          ideal_qos += vehicles_amount * Point.twoPointsDistance(start, end) / (speed * 1000);
          segments.add(new Segment(start, end, vehicles_amount, speed));
      }
      buffer.close();


      System.out.println("IDEAL RESULT:");
      System.out.println(ideal_qos);
    }catch(IOException e){
      System.out.println(e + " there was a problem reading the file");
    }
  }

  private static void loadRsuTypes() {
    try{
      File file = new File("greedy/rsu_types.txt");
      FileInputStream input_stream = new FileInputStream(file);
      BufferedReader buffer = new BufferedReader(new InputStreamReader(input_stream));

      String line = null;
      String [] line_tokens = null;
      for (line = buffer.readLine(); line != null; line = buffer.readLine()){
        line_tokens = line.split(" ");

        rsu_types.add(new RsuType(Double.parseDouble(line_tokens[2]), Double.parseDouble(line_tokens[3])));
      }
      buffer.close();

    }catch(IOException e){
      System.out.println(e + " there was a problem reading the file");
    }
  }
}
