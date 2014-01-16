package team000;


import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NavigatingState implements RobotState {
  
  private static final int INITIAL_ATTEMPTED_ROTATIONS = 1;
  private static final int START_BUGGING_ROTATIONS = -(INITIAL_ATTEMPTED_ROTATIONS + 1);
  private static final int FINAL_ATTEMPTED_ROTATIONS = -4;
  private static final int BACKWARDS_ONE_ROTATION = -1;
  private final RobotController rc;
  private final Knowledge knowledge;
  private final RobotState previousState;
  private final MapLocation destination;
  private Direction currentDirection;
  private Direction desiredDirection;
  private boolean isBugging;
  private int buggingRotations;
  private BuggingOrientation buggingOrientation;

  public NavigatingState(RobotController rc, Knowledge knowledge, RobotState previousState,
      MapLocation destination) {
    this.rc = rc;
    this.knowledge = knowledge;
    this.previousState = previousState;
    this.destination = destination;
    this.isBugging = false;
    update();
    this.currentDirection = desiredDirection;
  }

  @Override
  public RobotState run() throws GameActionException {
    knowledge.update();
    update();
    if (knowledge.enemiesNearby) {
      return new SkirmishMicroState(rc, knowledge, this).run();
    }
    if (knowledge.myLocation.isAdjacentTo(destination)) {
      return previousState.run();
    }
    if (rc.isActive()) {
      if (isBugging) {
        isBugging = attemptBugMove();
      } else if (!attemptMoveTowardsDestination()) {
        initBugging();
        attemptBugMove();
      }
    }
    rc.setIndicatorString(0, "isBugging: " + isBugging + ", orientation: " + buggingOrientation);
    rc.setIndicatorString(1, "currentDirection: " + currentDirection + ", desiredDirection: " + desiredDirection);
    rc.setIndicatorString(2, "buggingRotations: " + buggingRotations);
    return this;
  }
  
  private void initBugging() {
    isBugging = true;
    buggingOrientation = BuggingOrientation.createRandom();
    buggingRotations = 0;
    currentDirection = desiredDirection;
    rotateCurrentDirection(START_BUGGING_ROTATIONS);
  }

  private void rotateCurrentDirection(int numberOfRotations) {
    currentDirection = buggingOrientation.rotate(currentDirection, numberOfRotations);
    buggingRotations += numberOfRotations;
  }

  private void update() {
    Direction previousDesiredDirection = desiredDirection;
    desiredDirection = knowledge.myLocation.directionTo(destination);
    if (desiredDirection != previousDesiredDirection) {
      if (isBugging) {
        int difference = (desiredDirection.ordinal() - previousDesiredDirection.ordinal())
            % BuggingOrientation.NUMBER_OF_DIRECTIONS;
        if (difference < -(BuggingOrientation.NUMBER_OF_DIRECTIONS / 2)) {
          difference += BuggingOrientation.NUMBER_OF_DIRECTIONS; // Want to end between -4 and 4
        }
        buggingRotations -= difference * buggingOrientation.sign;
      }
    }
  }

  private boolean attemptBugMove() throws GameActionException {
    boolean stillBugging = true;
    int rotations = INITIAL_ATTEMPTED_ROTATIONS;
    if (currentDirection.isDiagonal()) {
      rotations++;
    }
    rotateCurrentDirection(rotations);
    for (; rotations >= FINAL_ATTEMPTED_ROTATIONS; rotations--) {
      if (rc.canMove(currentDirection)) {
        break;
      }
      rotateCurrentDirection(BACKWARDS_ONE_ROTATION);
    }
    if (buggingRotations >= 0) {
      if (rc.canMove(desiredDirection)) {
        move(desiredDirection);
      }
      stillBugging = false;
    }
    if (rc.isActive()) {
      if (rc.canMove(currentDirection)) {
        move(currentDirection);
      }
    }
    return stillBugging;
  }

  private boolean attemptMoveTowardsDestination() throws GameActionException {
    if (desiredDirection == Direction.OMNI) {
      return true;
    }
    if (rc.canMove(desiredDirection)) {
      move(desiredDirection);
      return true;
    }
    return false;
  }

  private void move(Direction direction) throws GameActionException {
    rc.move(direction);
    currentDirection = direction;
  }

}
