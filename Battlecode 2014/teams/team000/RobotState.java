package team000;

import battlecode.common.GameActionException;

public interface RobotState {
  
  RobotState run() throws GameActionException;

}
