package donothing;

import battlecode.common.RobotController;

public class RobotPlayer {

  public static void run(RobotController rc) {
    while (true) {
      try {
        rc.yield();
      } catch (Exception e) {
        rc.breakpoint();
        e.printStackTrace();
      }
    }
  }
}
