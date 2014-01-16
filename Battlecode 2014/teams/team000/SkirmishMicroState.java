package team000;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class SkirmishMicroState implements RobotState {
  
  private final RobotController rc;
  private final Knowledge knowledge;
  private final RobotState previousState;

  public SkirmishMicroState(RobotController rc, Knowledge knowledge, NavigatingState previousState) {
    this.rc = rc;
    this.knowledge = knowledge;
    this.previousState = previousState;
  }

  @Override
  public RobotState run() throws GameActionException {
    // TODO Auto-generated method stub
    return null;
  }

}
