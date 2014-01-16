package team000;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class RobotPlayer {
  
  private static Random RANDOM;
  private static RobotController RC;
  private static int ID;
  private static int ID_PARITY;

  public static void run(RobotController rc) {
    RC = rc;
    ID = rc.getRobot().getID();
    ID_PARITY = ID % 2;
    RANDOM = new Random(ID);
    RobotState state;
    switch (RC.getType()) {
    case HQ:
      state = initializeHqState();
      break;
    case SOLDIER:
      state = initializeSoldierState();
      break;
    default:
      state = new NullState();
      break;
    }
    while (true) {
      try {
        state = state.run();
        RC.yield();
      } catch (Exception e) {
        rc.breakpoint();
        e.printStackTrace();
      }
    }
  }

  private static RobotState initializeHqState() {
    return new SpawningSoldiersState(RC);
  }

  private static RobotState initializeSoldierState() {
    Knowledge knowledge = new Knowledge(RC);
    return new NavigatingState(RC, knowledge, new NullState(), knowledge.ENEMY_HQ_LOCATION);
//    return new HqAndPastrsAttackingState(RC);
  }
  
  public static boolean generateRandomBoolean() {
    return RANDOM.nextInt(2) == ID_PARITY;
  }

  private static boolean attemptAttack() throws GameActionException {
    return false;
//    RobotInfo[] attackableRobotInfos = getAttackableRobotInfos().getNearbyRobots();
//    if (attackableRobotInfos.length <= 0) {
//      return false;
//    }
//    double minimumHealth = Integer.MAX_VALUE;
//    MapLocation minimumHealthLocation = null;
//    for (RobotInfo attackableRobotInfo : attackableRobotInfos) {
//      double enemyRobotHealth = attackableRobotInfo.health;
//      if (enemyRobotHealth < minimumHealth) {
//        minimumHealth = enemyRobotHealth;
//        minimumHealthLocation = attackableRobotInfo.location;
//      }
//    }
//    if (minimumHealthLocation == null) {
//      // Enemy HQ in attack radius?
//      return false;
//    }
//    RC.attackSquare(minimumHealthLocation);
//    return true;
  }
  
  private static void runAsHq() throws GameActionException {
    if (RC.isActive()) {
      if (!attemptAttack()) {
        if (RC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
          MapLocation myLocation = RC.getLocation();
          Direction desiredDirection = myLocation.directionTo(RC.senseEnemyHQLocation());
          for (int i = 0; i < 8; i++, desiredDirection = desiredDirection.rotateLeft()) {
            if (RC.senseObjectAtLocation(myLocation.add(desiredDirection)) == null) {
              RC.spawn(desiredDirection);
              break;
            }
          }
        }
      }
    }
  }
  
}
