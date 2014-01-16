package blindrush;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class RobotPlayer {
  
  private static MapLocation ENEMY_HQ_LOCATION;
  private static RobotController RC;
  private static Team ENEMY_TEAM;

  public static void run(RobotController rc) {
    ENEMY_HQ_LOCATION = rc.senseEnemyHQLocation();
    RC = rc;
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
        attemptMoveTowards(ENEMY_HQ_LOCATION);
      }
    }
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
    for (int i = 0; i < 8; i++) {
      if (RC.canMove(desiredDirection)) {
        RC.move(desiredDirection);
        break;
      } 
      desiredDirection = desiredDirection.rotateLeft();
    }
  }

  private static void runAsHq() throws GameActionException {
    if (RC.isActive()) {
      if (!attemptAttack()) {
        if (RC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
          RC.spawn(RC.getLocation().directionTo(ENEMY_HQ_LOCATION));
        }
      }
    }
  }

}
