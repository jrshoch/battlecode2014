package smarterrush;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class RobotPlayer {
  
  private static Random RAND;
  private static MapLocation ENEMY_HQ_LOCATION;
  private static RobotController RC;
  private static Team ENEMY_TEAM;

  public static void run(RobotController rc) {
    RAND = new Random();
    RC = rc;
    ENEMY_HQ_LOCATION = RC.senseEnemyHQLocation();
    ENEMY_TEAM = RC.getTeam().opponent();
    switch (RC.getType()) {
    case HQ:
      while (true) {
        try {
          runAsHq();
          RC.yield();
        } catch (Exception e) {
          rc.breakpoint();
          e.printStackTrace();
        }
      }
    case SOLDIER:
      while (true) {
        try {
          runAsSoldier();
          RC.yield();
        } catch (Exception e) {
          rc.breakpoint();
          e.printStackTrace();
        }
      }
    case PASTR:
      while (true) {
        
      }
    case NOISETOWER:
      while (true) {
        
      }
    }
  }

  private static void runAsSoldier() throws GameActionException {
    if (RC.isActive()) {
      if (!attemptAttack()) {
        MapLocation destination = determineSoldierDestination();
        attemptMoveTowards(destination);
      }
    }
  }

  private static MapLocation determineSoldierDestination() {
    MapLocation[] enemyPastrLocations = RC.sensePastrLocations(ENEMY_TEAM);
    if (enemyPastrLocations.length == 0) {
      return ENEMY_HQ_LOCATION;
    }
    MapLocation nearestEnemyPastr = null;
    int distanceToNearest = Integer.MAX_VALUE;
    for (MapLocation enemyPastrLocation : enemyPastrLocations) {
      int distance = RC.getLocation().distanceSquaredTo(enemyPastrLocation);
      if (distance < distanceToNearest) {
        distanceToNearest = distance;
        nearestEnemyPastr = enemyPastrLocation;
      }
    }
    return nearestEnemyPastr;
  }

  private static boolean attemptAttack() throws GameActionException {
    Robot[] enemyAttackableRobots =
        RC.senseNearbyGameObjects(Robot.class, RC.getType().attackRadiusMaxSquared,
            ENEMY_TEAM);
    if (enemyAttackableRobots.length <= 0) {
      return false;
    }
    double minimumHealth = Integer.MAX_VALUE;
    MapLocation minimumHealthLocation = null;
    for (Robot enemyAttackableRobot : enemyAttackableRobots) {
      RobotInfo enemyRobotInfo = RC.senseRobotInfo(enemyAttackableRobot);
      double enemyRobotHealth = enemyRobotInfo.health;
      if (enemyRobotHealth < minimumHealth) {
        minimumHealth = enemyRobotHealth;
        minimumHealthLocation = enemyRobotInfo.location;
      }
    }
    if (minimumHealthLocation == null) {
      // Enemy HQ in attack radius?
      return false;
    }
    RC.attackSquare(minimumHealthLocation);
    return true;
  }
  
  private static void attemptMoveTowards(MapLocation destination) throws GameActionException {
    Direction desiredDirection = RC.getLocation().directionTo(destination);
    if (RAND.nextBoolean()) {
      for (int i = 0; i < 8; i++) {
        if (RC.canMove(desiredDirection)) {
          if (directionPutsMeInRangeOfEnemyHq(desiredDirection)) {
            return;
          }
          RC.move(desiredDirection);
          break;
        } 
        desiredDirection = desiredDirection.rotateLeft();
      }
    } else {
      for (int i = 0; i < 8; i++) {
        if (RC.canMove(desiredDirection)) {
          if (directionPutsMeInRangeOfEnemyHq(desiredDirection)) {
            return;
          }
          RC.move(desiredDirection);
          break;
        } 
        desiredDirection = desiredDirection.rotateRight();
      }
    }
  }

  private static boolean directionPutsMeInRangeOfEnemyHq(Direction desiredDirection) {
    int distance = RC.getLocation().add(desiredDirection).distanceSquaredTo(ENEMY_HQ_LOCATION);
    return distance <= RobotType.HQ.attackRadiusMaxSquared;
  }

  private static void runAsHq() throws GameActionException {
    if (RC.isActive()) {
      if (!attemptAttack()) {
        RC.spawn(RC.getLocation().directionTo(ENEMY_HQ_LOCATION));
      }
    }
  }

}
