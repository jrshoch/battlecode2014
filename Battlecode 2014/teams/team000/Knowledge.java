package team000;

import battlecode.common.Clock;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Knowledge {
  
  private final RobotController rc;
  public final MapLocation ENEMY_HQ_LOCATION;
  public final MapLocation HQ_LOCATION;
  public final Team MY_TEAM;
  public final Team ENEMY_TEAM;
  public MapLocation myLocation;
  private double previousHealth;
  public RobotType type;
  public int roundsUntilHealing;
  public int round;
  private int roundLastAttacked;
  public double health;
  public boolean enemiesNearby;

  public Knowledge(RobotController rc) {
    this.rc = rc;
    this.ENEMY_HQ_LOCATION = rc.senseEnemyHQLocation();
    this.HQ_LOCATION = rc.senseHQLocation();
    this.MY_TEAM = rc.getTeam();
    this.ENEMY_TEAM = MY_TEAM.opponent();
    this.type = rc.getType();
    this.previousHealth = type.maxHealth;
    update();
  }
  
  public void update() {
    round = Clock.getRoundNum();
    myLocation = rc.getLocation();
    previousHealth = health;
    health = rc.getHealth();
    if (health < previousHealth) {
      roundLastAttacked = round;
    }
    roundsUntilHealing = GameConstants.HEAL_TURN_DELAY - (round - roundLastAttacked);
    Robot[] nearbyEnemyRobots =
      rc.senseNearbyGameObjects(Robot.class, type.sensorRadiusSquared, ENEMY_TEAM);
    enemiesNearby = nearbyEnemyRobots.length > 0;
  }

}
