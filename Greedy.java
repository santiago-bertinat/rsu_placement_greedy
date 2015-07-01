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
  public static ArrayList<Segment> sorted_segments = new ArrayList<Segment>();
  public static ArrayList<Segment> segments = new ArrayList<Segment>();
  public static ArrayList<RsuType> rsu_types = new ArrayList<RsuType>();
  public static double qos = 0;
  public static double coverage_acceptance = 0.95;
  public static double coverage_stop = 0.8;

  public static void main(String[] args) {
    loadSegments();
    loadRsuTypes();

    // Ascending order
    Collections.sort(sorted_segments);

    ArrayList<Segment> intersected_segments =  new ArrayList<Segment>();
    for (int i = sorted_segments.size() - 1; i >= 0; i--) {
      Segment segment = sorted_segments.get(i);

      if (!intersected_segments.contains(segment)) {
        Random generator = new Random();
        double rsu_position = generator.nextDouble();

        Point rsu_center = rsuPosition(segment, rsu_position);
        Rsu rsu = new Rsu(rsu_center, rsu_types.get(rsu_types.size() - 1));
        segment.rsu = rsu;

        intersected_segments = findIntersections();
      }
    }

    saveResults("greedy/qos_results.txt");
    saveResultsForAE("greedy/qos_results_AE.txt");

    System.out.println("RESULT: ");
    System.out.println(qos);
    System.out.println("COST: ");
    double cost = 0;
    for (Segment segment : segments) {
      if (segment.rsu != null) {
        cost += segment.rsu.rsu_type.cost;
      }
    }
    System.out.println(cost);

    double qos_found = qos;

    while (qos >= qos_found * coverage_stop) {
      double[] qos_loss = new double[segments.size()];
      double old_qos = qos;

      // System.out.println("QOS LOSS:");
      for (int i = 0; i < sorted_segments.size(); i++) {
        Segment segment = sorted_segments.get(i);

        Rsu aux_rsu = segment.rsu;
        if (segment.rsu == null || segment.rsu.rsu_type == null) {
          qos_loss[i] = 20000;
        }else {
          RsuType rsu_type = segment.rsu.rsu_type;
          int index_of_type = rsu_types.indexOf(rsu_type);

          if (index_of_type == 0) {
            segment.rsu = null;
          }else {
            segment.rsu.setRsuType(rsu_types.get(index_of_type - 1));
          }

          findIntersections();

          if (index_of_type == 0) {
            segment.rsu = aux_rsu;
          }else {
            segment.rsu.setRsuType(rsu_type);
          }
        }
      }

      int min = 0;
      for (int i = 1; i < sorted_segments.size(); i++) {
        if (qos_loss[i] < qos_loss[min]) {
          min = i;
        }
      }

      Segment segment = sorted_segments.get(min);
      RsuType rsu_type = segment.rsu.rsu_type;
      int index_of_type = rsu_types.indexOf(rsu_type);

      if (index_of_type == 0) {
        segment.rsu = null;
      }else {
        segment.rsu.setRsuType(rsu_types.get(index_of_type - 1));
      }

      findIntersections();
    }

    System.out.println("RESULT: ");
    System.out.println(qos);
    System.out.println("COST: ");
    cost = 0;
    for (Segment segment : segments) {
      if (segment.rsu != null) {
        cost += segment.rsu.rsu_type.cost;
      }
    }
    System.out.println(cost);

    saveResults("greedy/cost_results.txt");
    saveResultsForAE("greedy/cost_results_AE.txt");
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
      for (Segment segment : segments) {
        if (segment.rsu != null) {
          Rsu rsu = segment.rsu;
          buffer.write(String.valueOf(rsu.center.x) + ',');
          buffer.write(String.valueOf(rsu.center.y) + ',');
          buffer.write(String.valueOf(rsu.radius) + ',');
          buffer.write(String.valueOf(rsu.rsu_type.cost) + '\n');
        }
      }

      buffer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void saveResultsForAE(String file_location) {
    try {
      File file = new File(file_location);

      // if file doesnt exists, then create it
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter file_writer = new FileWriter(file.getAbsoluteFile());
      BufferedWriter buffer = new BufferedWriter(file_writer);

      for (Segment segment : segments) {
        if (segment.rsu != null) {
          RsuType rsu_type = segment.rsu.rsu_type;
          int index_of_type = rsu_types.indexOf(rsu_type) + 1;
          double position = Point.twoPointsDistance(segment.start, segment.rsu.center) / segment.distance();
          buffer.write(String.valueOf((float)(index_of_type + position)) + ",");
        } else {
          buffer.write("0,");
        }
      }

      buffer.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static Point rsuPosition(Segment segment, double rsu_position) {
    double x_length = Math.abs(segment.start.x - segment.end.x) * rsu_position;
    double y_length = Math.abs(segment.start.y - segment.end.y) * rsu_position;

    double x = segment.start.x;
    if (segment.start.x < segment.end.x) {
      x = segment.start.x + x_length;
    }else if (segment.start.x > segment.end.x) {
      x = segment.start.x - x_length;
    }

    double y = segment.start.y;
    if (segment.start.y < segment.end.y) {
      y = segment.start.y + y_length;
    }else if (segment.start.y > segment.end.y) {
      y = segment.start.y - y_length;
    }

    return new Point(x, y);
  }

  private static ArrayList<Segment> findIntersections() {
    ArrayList<Segment> intersected_segments = new ArrayList<Segment>();
    qos = 0;

    for (Segment segment : segments){

      double divitions = 10;
      double module_section = segment.distance() / divitions;
      double intersections = 0;

      double coverered_distance = 0;

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
        for (Segment aux_segment : segments) {
          Rsu rsu = aux_segment.rsu;
          if (rsu != null && rsu.belongsToCircle(aux_point)) {
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

      qos += segment.vehicles_amount * (coverered_distance) / (segment.speed * 1000);
    }
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

          Segment segment = new Segment(start, end, vehicles_amount, speed);
          segments.add(segment);
          sorted_segments.add(segment);
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
