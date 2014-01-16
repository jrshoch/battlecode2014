package team000;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SpawningSoldiersState implements RobotState {
  
  private final RobotController rc;
  
  public SpawningSoldiersState(RobotController rc) {
    this.rc = rc;
  }

  @Override
  public RobotState run() throws GameActionException {
    if (rc.isActive()) {
      if (!attemptAttack()) {
        if (rc.senseRobotCount() < GameConstants.MAX_ROBOTS) {
          MapLocation myLocation = rc.getLocation();
          Direction desiredDirection = myLocation.directionTo(rc.senseEnemyHQLocation());
          for (int i = 0; i < 8; i++, desiredDirection = desiredDirection.rotateLeft()) {
            if (rc.senseObjectAtLocation(myLocation.add(desiredDirection)) == null) {
              rc.spawn(desiredDirection);
              break;
            }
          }
        }
      }
    }
    return this;
  }

  private boolean attemptAttack() {
    return false;
  }

}
