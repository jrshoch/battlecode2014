package team000;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class HqAndPastrsAttackingState implements RobotState {
  
  private static final int DEBUG_LEVEL = 0;
  private static final Random RAND = new Random();
  private final RobotController rc;
  private final Knowledge knowledge;
  private final RobotState previousState;
  
  public HqAndPastrsAttackingState(RobotController rc) {
    this.rc = rc;
    this.knowledge = new Knowledge(rc);
    this.previousState = null;
  }

  @Override
  public RobotState run() throws GameActionException {
    knowledge.update();
    if (rc.isActive()) {
      Direction retreatDirection = micro();
      if (rc.isActive()) {
        if (retreatDirection == Direction.NONE) {
          MapLocation destination = determineSoldierDestination();
          attemptMoveTowards(destination);
        } else {
          attemptMove(retreatDirection);
        }
      }
    }
    return this;
  }

  private MapLocation determineSoldierDestination() {
    MapLocation[] enemyPastrLocations = rc.sensePastrLocations(knowledge.ENEMY_TEAM);
    if (enemyPastrLocations.length == 0) {
      return knowledge.ENEMY_HQ_LOCATION;
    }
    MapLocation nearestEnemyPastr = null;
    int distanceToNearest = Integer.MAX_VALUE;
    for (MapLocation enemyPastrLocation : enemyPastrLocations) {
      int distance = knowledge.myLocation.distanceSquaredTo(enemyPastrLocation);
      if (distance < distanceToNearest) {
        distanceToNearest = distance;
        nearestEnemyPastr = enemyPastrLocation;
      }
    }
    return nearestEnemyPastr;
  }
  
  private Direction micro() throws GameActionException {
    NearbyRobots attackableRobotInfos = getAttackableRobotInfos();
    RobotInfo[] attackableSoldierInfos = attackableRobotInfos.getNearby(true);
    RobotInfo[] attackableNotSoldierInfos = attackableRobotInfos.getNearby(false);
    if (attackableSoldierInfos.length == 0) {
      if (attackableNotSoldierInfos.length > 0) {
//        debug("No enemy soldiers within attacking range; attacking non-soldier.");
        attackWeakest(attackableNotSoldierInfos);
        return Direction.NONE;
      }
//      debug("No enemy robots nearby; not attacking.");
      return Direction.NONE;
    }
    RobotInfo[] attackingAlliedRobotInfos = getAttackingAlliedRobotInfos().getNearby(true);
    int outcome = simulateOutcome(attackableSoldierInfos, attackingAlliedRobotInfos);
    int selfDestructOutcome =
        simulateSelfDestruct(attackableSoldierInfos, attackingAlliedRobotInfos);
    if (selfDestructOutcome > outcome && selfDestructOutcome > 0) {
      rc.selfDestruct();
      return Direction.NONE;
    }
    if (outcome > 0) {
      attackWeakest(attackableSoldierInfos);
      return Direction.NONE;
    }
    MapLocation averageAttackableRobotLocation = getAverageLocation(attackableSoldierInfos);
    if (averageAttackableRobotLocation == knowledge.myLocation) {
      attackWeakest(attackableSoldierInfos);
      return Direction.NONE;
    }
    return knowledge.myLocation.directionTo(averageAttackableRobotLocation).opposite();
  }

  private int simulateSelfDestruct(RobotInfo[] attackableRobotInfos,
      RobotInfo[] attackingAlliedRobotInfos) {
    double destructDamage = knowledge.health * GameConstants.SELF_DESTRUCT_DAMAGE_FACTOR
            + GameConstants.SELF_DESTRUCT_BASE_DAMAGE;
    int outcome = -1;
    outcome += countDestructDeaths(attackableRobotInfos, knowledge.myLocation, destructDamage);
    outcome -= countDestructDeaths(attackingAlliedRobotInfos, knowledge.myLocation, destructDamage);
    return outcome;
  }

  private int countDestructDeaths(RobotInfo[] robotInfos, MapLocation destructLocation,
      double destructDamage) {
    int deaths = 0;
    for (RobotInfo attackableRobotInfo : robotInfos) {
      if (attackableRobotInfo.location.isAdjacentTo(destructLocation)
          && attackableRobotInfo.health < destructDamage) {
        deaths++;
      }
    }
    return deaths;
  }

  private NearbyRobots getAttackingAlliedRobotInfos() throws GameActionException {
    return getSoldiersWithinAttackRange(knowledge.MY_TEAM, true);
  }

  private NearbyRobots getAttackableRobotInfos() throws GameActionException {
    return getSoldiersWithinAttackRange(knowledge.ENEMY_TEAM, false);
  }

  private NearbyRobots getSoldiersWithinAttackRange(Team team, boolean includeSelf)
      throws GameActionException {
    Robot[] robots =
        rc.senseNearbyGameObjects(Robot.class, knowledge.type.attackRadiusMaxSquared, team);
    int length = includeSelf ? robots.length + 1 : robots.length;
    RobotInfo[] robotInfos = new RobotInfo[length];
    int numberOfSoldiers = 0;
    int[] soldierIndices = new int[length];
    for (int i = 0; i < robots.length; i++) {
      RobotInfo robotInfo = rc.senseRobotInfo(robots[i]);
      robotInfos[i] = robotInfo;
      if (robotInfo.type == RobotType.SOLDIER) {
        soldierIndices[numberOfSoldiers++] = i;
      }
    }
    if (includeSelf) {
      robotInfos[robots.length] = getMyRobotInfo();
      soldierIndices[numberOfSoldiers++] = robots.length;
    }
    if (numberOfSoldiers == 0) {
      return new NearbyRobots(robotInfos, false);
    }
    RobotInfo[] soldierInfos = new RobotInfo[numberOfSoldiers];
    for (int i = 0; i < numberOfSoldiers; i++) {
      soldierInfos[i] = robotInfos[soldierIndices[i]];
    }
    return new NearbyRobots(soldierInfos, true);
  }

  private RobotInfo getMyRobotInfo() {
    // TODO (jhoch): remove
    return new RobotInfo(rc.getRobot(), rc.getLocation(), rc.getHealth(), Direction.NONE,
        RobotType.SOLDIER, knowledge.MY_TEAM, rc.getActionDelay(), false, RobotType.SOLDIER, 0);
  }

  private int simulateOutcome(RobotInfo[] attackableRobotInfos,
      RobotInfo[] attackingAlliedRobotInfos) {
    int[] enemyBuckets = bucketRobotsByHealth(attackableRobotInfos);
    int[] alliedBuckets = bucketRobotsByHealth(attackingAlliedRobotInfos);
//    debug("alliedBuckets: " + printout(alliedBuckets) + "; enemyBuckets: " + printout(enemyBuckets));
    int totalEnemies = attackableRobotInfos.length;
    int totalAllies = attackingAlliedRobotInfos.length;
    int numberOfKilledAllies = 0;
    int numberOfKilledEnemies = 0;
    numberOfKilledEnemies += simulateHits(1, enemyBuckets); // my initial hit
    while (totalEnemies > 0 && totalAllies > 0) {
      numberOfKilledAllies += simulateHits(totalEnemies, alliedBuckets);
//      debug("numberOfKilledAllies: " + numberOfKilledAllies + "; alliedBuckets: " + printout(alliedBuckets));
      totalAllies -= numberOfKilledAllies;
      numberOfKilledEnemies += simulateHits(totalAllies, enemyBuckets);
//      debug("numberOfKilledEnemies: " + numberOfKilledEnemies + "; enemyBuckets: " + printout(enemyBuckets));
      totalEnemies -= numberOfKilledEnemies;
      if (numberOfKilledEnemies - numberOfKilledAllies != 0) {
        return numberOfKilledEnemies - numberOfKilledAllies;
      }
    }
    return totalAllies - totalEnemies;
  }

  private int[] bucketRobotsByHealth(RobotInfo[] robotInfos) {
    int numBuckets = (int) (RobotType.SOLDIER.maxHealth / RobotType.SOLDIER.attackPower);
    int[] buckets = new int[numBuckets];
    for (RobotInfo robotInfo : robotInfos) {
      buckets[(int) ((robotInfo.health - 0.01)/ RobotType.SOLDIER.attackPower)]++;
    }
    return buckets;
  }

  private int simulateHits(int totalTeamA, int[] bucketsTeamB) {
    int hitsTeamA = totalTeamA;
    int bucketIndex = 0;
    int kills = 0;
    while (hitsTeamA > 0 && bucketIndex < bucketsTeamB.length) {
//      debug("Checking bucket " + bucketIndex, 2);
      if (bucketsTeamB[bucketIndex] > 0) {
//        debug("Hitting bucket " + bucketIndex + " with " + bucketsTeamB[bucketIndex] + " robots; have " + hitsTeamA + " hits left.", 1);
        int bucketKills = Math.min(hitsTeamA / (bucketIndex + 1), bucketsTeamB[bucketIndex]);
//        debug(bucketKills + " kills.", 1);
        bucketsTeamB[bucketIndex] -= bucketKills;
        kills += bucketKills;
        hitsTeamA -= bucketKills * (bucketIndex + 1);
        if (hitsTeamA < (bucketIndex + 1)) {
          bucketsTeamB[bucketIndex] -= 1;
          bucketsTeamB[bucketIndex - hitsTeamA] += 1;
          hitsTeamA = 0;
        }
      }
      bucketIndex++;
    }
    return kills;
  }

  private void attackWeakest(RobotInfo[] attackableRobotInfos) throws GameActionException {
    double minHealth = Integer.MAX_VALUE;
    MapLocation weakestLocation = null;
    for (RobotInfo attackableRobotInfo : attackableRobotInfos) {
      if (attackableRobotInfo.health < minHealth) {
        minHealth = attackableRobotInfo.health;
        weakestLocation = attackableRobotInfo.location;
      }
    }
    if (weakestLocation != null) {
      rc.attackSquare(weakestLocation);
    }
  }

  private MapLocation getAverageLocation(RobotInfo[] robotInfos) {
    int totalX = 0;
    int totalY = 0;
    for (RobotInfo robotInfo : robotInfos) {
      MapLocation location = robotInfo.location;
      totalX += location.x;
      totalY += location.y;
    }
    return new MapLocation(Math.round(totalX), Math.round(totalY));
  }

  private void attemptMoveTowards(MapLocation destination) throws GameActionException {
    Direction desiredDirection = knowledge.myLocation.directionTo(destination);
    attemptMove(desiredDirection);
  }
    
  private void attemptMove(Direction desiredDirection) throws GameActionException {
    Direction attemptDirection = desiredDirection;
    if (RAND.nextBoolean()) {
      for (int i = 0; i < 8; i++) {
        if (rc.canMove(attemptDirection)) {
          if (directionPutsMeInRangeOfEnemyHq(attemptDirection)) {
            return;
          }
          rc.move(attemptDirection);
          break;
        } 
        attemptDirection = attemptDirection.rotateLeft();
      }
    } else {
      for (int i = 0; i < 8; i++) {
        if (rc.canMove(attemptDirection)) {
          if (directionPutsMeInRangeOfEnemyHq(attemptDirection)) {
            return;
          }
          rc.move(attemptDirection);
          break;
        } 
        attemptDirection = attemptDirection.rotateRight();
      }
    }
  }

  private boolean directionPutsMeInRangeOfEnemyHq(Direction desiredDirection) {
    int distance = knowledge.myLocation.add(desiredDirection).distanceSquaredTo(knowledge.ENEMY_HQ_LOCATION);
    return distance <= RobotType.HQ.attackRadiusMaxSquared;
  }

  private static class NearbyRobots {
    
    private final RobotInfo[] nearbyRobots;
    private final boolean areSoldiers;
    
    public NearbyRobots(RobotInfo[] nearbyRobots, boolean areSoldiers) {
      if (nearbyRobots.length == 1 && nearbyRobots[0].type == RobotType.HQ) {
        this.nearbyRobots = new RobotInfo[0];
      } else {
        this.nearbyRobots = nearbyRobots;
      }
      this.areSoldiers = areSoldiers;
    }
    
    public RobotInfo[] getNearby(boolean queryAreSoldiers) {
      if (areSoldiers ^ queryAreSoldiers) {
        return new RobotInfo[0];
      }
      return nearbyRobots;
    }
  }
  
  private String printout(int[] ints) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i : ints) {
      sb.append(i);
      sb.append(' ');
    }
    sb.append(']');
    return sb.toString();
  }

  private void debug(String string) {
//    debug(string, 0);
  }
  
  private void debug(String string, int debugLevel) {
    if (debugLevel < DEBUG_LEVEL) {
      System.out.println(string);
    }
  }

}
