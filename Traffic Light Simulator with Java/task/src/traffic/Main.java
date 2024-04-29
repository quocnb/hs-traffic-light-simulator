package traffic;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class Main {

  enum Command {
    Add(1), Delete(2), System(3), Quit(0);

    final int value;
    Command(int i) {
      value = i;
    }

    public static Command fromInt(int value) {
      for (Command command : Command.values()) {
        if (command.value == value) {
          return command;
        }
      }
      throw new IllegalArgumentException("Invalid value for Command enum: " + value);
    }

    @Override
    public String toString() {
      String display = switch (this) {
          case Add -> "Add road";
          case Delete -> "Delete road";
          case System -> "Open system";
          case Quit -> "Quit";
      };
      return String.format("%d. %s", value, display);
    }
  }

  private final static Scanner scanner = new Scanner(System.in);
  static int maxRoads;
  static int interval;
  static boolean isSystemMode;
  static long startTime = 0;
  static Thread stopwatchThread;
  private static LinkedList<Road> roads = new LinkedList<>();

  public static void main(String[] args) {
    System.out.println("Welcome to the traffic management system!");
    System.out.print("Input the number of roads: ");
    maxRoads = readIntValue();
    System.out.print("Input the interval: ");
    interval = readIntValue();
    stopwatchThread = stopwatchThread();
    stopwatchThread.start();
    startTime = System.currentTimeMillis();

    while (true) {
      cleanConsole();
      System.out.println("Menu:");
      for (Command cmd : Command.values()) {
        System.out.println(cmd.toString());
      }
      Command cmd;
      try {
        int input = Integer.parseInt(scanner.nextLine());
        cmd = Command.fromInt(input);
      } catch (Exception e) {
        System.out.println("Incorrect option");
        scanner.nextLine();
        continue;
      }
      switch (cmd) {
        case Add:
          addRoad();
          break;
        case Delete:
          deleteRoad();
          break;
        case System:
          openSystem();
          break;
        case Quit:
          quit();
          return;
      }
      scanner.nextLine();
      isSystemMode = false;
    }
  }

  static void cleanConsole() {
    try {
      var clearCommand = System.getProperty("os.name").contains("Windows")
              ? new ProcessBuilder("cmd", "/c", "cls")
              : new ProcessBuilder("clear");
      clearCommand.inheritIO().start().waitFor();
    } catch (IOException | InterruptedException e) {}
  }

  static int readIntValue() {
    while (true) {
      try {
        int number = Integer.parseInt(scanner.nextLine());
        if (number > 0) {
          return number;
        }
      } catch (Exception e) { }
      System.out.print("Error! Incorrect Input. Try again: ");
    }
  }

  static void addRoad() {
    System.out.print("Input road name: ");
    String roadName = scanner.nextLine();
    if (roads.size() >= maxRoads) {
      System.out.println("Queue is full");
      return;
    }
    Road road;
    if (roads.isEmpty()) {
      road = new Road(roadName, true, interval);
    } else {
      Road lastRoad = roads.getLast();
      int timeLeft = 0;
      if (lastRoad.isOpen) {
        timeLeft = lastRoad.timeRemaining;
      } else {
        timeLeft = lastRoad.timeRemaining + interval;
      }
      road = new Road(roadName, false, timeLeft);
    }
    roads.add(road);
    System.out.println(roadName + " Added!");
  }

  static void deleteRoad() {
    if (roads.isEmpty()) {
      System.out.println("Queue is empty");
      return;
    }
    Road road = roads.poll();
    System.out.println(road.name + " deleted!");
    if (roads.isEmpty()) {
      return;
    }
    if (road.isOpen) {
      int count = 0;
      for (Road curRoad: roads) {
        if (count == 0) {
          curRoad.isOpen = true;
          curRoad.timeRemaining = interval + 1;
        } else {
          curRoad.timeRemaining = count * interval;
        }
        count += 1;
      }
    } else {
      for (Road curRoad : roads) {
        if (curRoad.isOpen) {
          return;
        }
        curRoad.timeRemaining -= interval;
      }
    }

  }

  static void openSystem() {
    isSystemMode = true;
  }

  private static void updateRoads(long elapsedTime) {
    for (Road road : roads) {
      road.updateState(elapsedTime, roads.size());
    }
  }

  static void quit() {
    System.out.println("Bye!");
    if (stopwatchThread != null) {
      stopwatchThread.interrupt();
    }
  }

  private static void printSystemInfo(long elapsedTime) {
    System.out.println("! " + elapsedTime + "s. have passed since system startup !");
    System.out.println("! Number of roads: " + maxRoads + " !");
    System.out.println("! Interval: " + interval + " !");
    System.out.println();
    for (Road road : roads) {
      System.out.println(road.getInfo());
    }
    System.out.println();
    System.out.println("! Press \"Enter\" to open menu !");
  }

  private static Thread stopwatchThread() {
    Thread stopwatchThread = new Thread(() -> {
      try {
        while (true) {
          Thread.sleep(1000);
          if (isSystemMode) {
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            updateRoads(elapsedTime);
            printSystemInfo(elapsedTime);
          }
        }
      } catch (InterruptedException e) {
        System.err.println(e.getMessage());
      }
    });
    stopwatchThread.setName("QueueThread");
    stopwatchThread.setDaemon(true); // Set as daemon to stop when main thread stops
    return stopwatchThread;
  }

  static class Road {
    private final String name;
    private boolean isOpen;
    private int timeRemaining;

    public Road(String name, boolean isOpen, int timeRemaining) {
      this.name = name;
      this.isOpen = isOpen;
      this.timeRemaining = timeRemaining;
    }

    public String getName() {
      return name;
    }

    public void updateState(long elapsedTime, int roadCount) {
      if (elapsedTime == 1) {
        return;
      }
      timeRemaining--;
      if (timeRemaining == 0) {
        if (roadCount > 1) {
          isOpen = !isOpen;
        }
        timeRemaining = isOpen ? interval : interval * (roadCount - 1);
      }
    }

    public String getInfo() {
      if (isOpen) {
        return "Road \"" + name + "\" will be open for " + timeRemaining + "s.";
      } else {
        return "Road \"" + name + "\" will be closed for " + timeRemaining + "s.";
      }
    }
  }
}
