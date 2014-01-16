package team000;

import battlecode.common.Direction;

public enum BuggingOrientation {
  CLOCKWISE(1),
  COUNTER_CLOCKWISE(-1);

  public static final int NUMBER_OF_DIRECTIONS = Direction.values().length - 2;
  private static final Direction[] rotations = { Direction.NORTH, Direction.NORTH_EAST,
      Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
      Direction.NORTH_WEST };
  public final int sign;

  private BuggingOrientation (int sign) {
    this.sign = sign;
  }
  
  public static BuggingOrientation createRandom() {
    boolean randomBoolean = RobotPlayer.generateRandomBoolean();
    return randomBoolean ? CLOCKWISE : COUNTER_CLOCKWISE;
  }

  public Direction rotate(Direction desiredDirection, int numberOfRotations) {
    Direction result =
        rotations[(sign * numberOfRotations + desiredDirection.ordinal() + NUMBER_OF_DIRECTIONS)
            % NUMBER_OF_DIRECTIONS];
    return result;
  }

}