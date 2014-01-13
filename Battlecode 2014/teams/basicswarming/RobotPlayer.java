package basicswarming;

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

public class RobotPlayer {
  
  private static final int DEBUG_LEVEL = 0;
  private static Random RAND;
  private static MapLocation ENEMY_HQ_LOCATION;
  private static MapLocation HQ_LOCATION;
  private static RobotController RC;
  private static Team MY_TEAM;
  private static Team ENEMY_TEAM;

  public static void run(RobotController rc) {
    RAND = new Random();
    RC = rc;
    HQ_LOCATION = RC.senseHQLocation();
    ENEMY_HQ_LOCATION = RC.senseEnemyHQLocation();
    MY_TEAM = RC.getTeam();
    ENEMY_TEAM = MY_TEAM.opponent();
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
      Direction retreatDirection = micro();
      if (RC.isActive()) {
        if (retreatDirection == Direction.NONE) {
          MapLocation destination = determineSoldierDestination();
          attemptMoveTowards(destination);
        } else {
          attemptMove(retreatDirection);
        }
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
  
  private static Direction micro() throws GameActionException {
    NearbyRobots attackableRobotInfos = getAttackableRobotInfos();
    RobotInfo[] attackableSoldierInfos = attackableRobotInfos.getNearby(true);
    RobotInfo[] attackableNotSoldierInfos = attackableRobotInfos.getNearby(false);
    if (attackableSoldierInfos.length == 0) {
      if (attackableNotSoldierInfos.length > 0) {
        debug("No enemy soldiers within attacking range; attacking non-soldier.");
        attackWeakest(attackableNotSoldierInfos);
        return Direction.NONE;
      }
      debug("No enemy robots nearby; not attacking.");
      return Direction.NONE;
    }
    RobotInfo[] attackingAlliedRobotInfos = getAttackingAlliedRobotInfos().getNearby(true);
    int outcome = simulateOutcome(attackableSoldierInfos, attackingAlliedRobotInfos);
    int selfDestructOutcome =
        simulateSelfDestruct(attackableSoldierInfos, attackingAlliedRobotInfos);
    if (selfDestructOutcome > outcome && selfDestructOutcome > 0) {
      RC.selfDestruct();
      return Direction.NONE;
    }
    if (outcome > 0) {
      attackWeakest(attackableSoldierInfos);
      return Direction.NONE;
    }
    MapLocation averageAttackableRobotLocation = getAverageLocation(attackableSoldierInfos);
    if (averageAttackableRobotLocation == RC.getLocation()) {
      attackWeakest(attackableSoldierInfos);
      return Direction.NONE;
    }
    return RC.getLocation().directionTo(averageAttackableRobotLocation).opposite();
  }

  private static int simulateSelfDestruct(RobotInfo[] attackableRobotInfos,
      RobotInfo[] attackingAlliedRobotInfos) {
    MapLocation myLocation = RC.getLocation();
    double destructDamage = RC.getHealth() * GameConstants.SELF_DESTRUCT_DAMAGE_FACTOR
            + GameConstants.SELF_DESTRUCT_BASE_DAMAGE;
    int outcome = -1;
    outcome += countDestructDeaths(attackableRobotInfos, myLocation, destructDamage);
    outcome -= countDestructDeaths(attackingAlliedRobotInfos, myLocation, destructDamage);
    return outcome;
  }

  private static int countDestructDeaths(RobotInfo[] robotInfos, MapLocation destructLocation,
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

  private static NearbyRobots getAttackingAlliedRobotInfos() throws GameActionException {
    return getSoldiersWithinAttackRange(MY_TEAM);
  }

  private static NearbyRobots getAttackableRobotInfos() throws GameActionException {
    return getSoldiersWithinAttackRange(ENEMY_TEAM);
  }

  private static NearbyRobots getSoldiersWithinAttackRange(Team team) throws GameActionException {
    Robot[] robots =
        RC.senseNearbyGameObjects(Robot.class, RC.getType().attackRadiusMaxSquared, team);
    RobotInfo[] robotInfos = new RobotInfo[robots.length];
    int numberOfSoldiers = 0;
    int[] soldierIndices = new int[robots.length];
    for (int i = 0; i < robots.length; i++) {
      RobotInfo robotInfo = RC.senseRobotInfo(robots[i]);
      robotInfos[i] = robotInfo;
      if (robotInfo.type == RobotType.SOLDIER) {
        soldierIndices[numberOfSoldiers++] = i;
      }
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

  private static int simulateOutcome(RobotInfo[] attackableRobotInfos,
      RobotInfo[] attackingAlliedRobotInfos) {
    int[] enemyBuckets = bucketRobotsByHealth(attackableRobotInfos);
    int[] alliedBuckets = bucketRobotsByHealth(attackingAlliedRobotInfos);
    debug("alliedBuckets: " + alliedBuckets + "; enemyBuckets: " + enemyBuckets);
    int totalEnemies = attackableRobotInfos.length;
    int totalAllies = attackingAlliedRobotInfos.length;
    int numberOfKilledAllies = 0;
    int numberOfKilledEnemies = 0;
    numberOfKilledEnemies += simulateHits(1, enemyBuckets); // my initial hit
    while (totalEnemies > 0 && totalAllies > 0) {
      numberOfKilledAllies += simulateHits(totalEnemies, alliedBuckets);
      debug("numberOfKilledAllies: " + numberOfKilledAllies + "; alliedBuckets: " + alliedBuckets);
      totalAllies -= numberOfKilledAllies;
      numberOfKilledEnemies += simulateHits(totalAllies, enemyBuckets);
      debug("numberOfKilledEnemies: " + numberOfKilledEnemies + "; enemyBuckets: " + enemyBuckets);
      totalEnemies -= numberOfKilledEnemies;
      if (numberOfKilledEnemies - numberOfKilledAllies != 0) {
        return numberOfKilledEnemies - numberOfKilledAllies;
      }
    }
    return totalAllies - totalEnemies;
  }

  private static int[] bucketRobotsByHealth(RobotInfo[] robotInfos) {
    int numBuckets = (int) (RobotType.SOLDIER.maxHealth / RobotType.SOLDIER.attackPower);
    int[] buckets = new int[numBuckets];
    for (RobotInfo robotInfo : robotInfos) {
      buckets[(int) ((robotInfo.health - 0.01)/ RobotType.SOLDIER.attackPower)]++;
    }
    return buckets;
  }

  private static int simulateHits(int totalTeamA, int[] bucketsTeamB) {
    int hitsTeamA = totalTeamA;
    int bucketIndex = 0;
    int kills = 0;
    while (hitsTeamA > 0 && bucketIndex < bucketsTeamB.length) {
      debug("Checking bucket " + bucketIndex, 2);
      if (bucketsTeamB[bucketIndex] > 0) {
        debug("Hitting bucket " + bucketIndex + " with " + bucketsTeamB[bucketIndex] + " robots; have " + hitsTeamA + " hits left.", 1);
        int bucketKills = Math.min(hitsTeamA / (bucketIndex + 1), bucketsTeamB[bucketIndex]);
        debug(bucketKills + " kills.", 1);
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

  private static void attackWeakest(RobotInfo[] attackableRobotInfos) throws GameActionException {
    double minHealth = Integer.MAX_VALUE;
    MapLocation weakestLocation = null;
    for (RobotInfo attackableRobotInfo : attackableRobotInfos) {
      if (attackableRobotInfo.health < minHealth) {
        minHealth = attackableRobotInfo.health;
        weakestLocation = attackableRobotInfo.location;
      }
    }
    if (weakestLocation != null) {
      RC.attackSquare(weakestLocation);
    }
  }

  private static MapLocation getAverageLocation(RobotInfo[] robotInfos) {
    int totalX = 0;;
    int totalY = 0;
    for (RobotInfo robotInfo : robotInfos) {
      MapLocation location = robotInfo.location;
      totalX += location.x;
      totalY += location.y;
    }
    return new MapLocation(Math.round(totalX), Math.round(totalY));
  }

  private static boolean attemptAttack() throws GameActionException {
    RobotInfo[] attackableRobotInfos = getAttackableRobotInfos().getNearbyRobots();
    if (attackableRobotInfos.length <= 0) {
      return false;
    }
    double minimumHealth = Integer.MAX_VALUE;
    MapLocation minimumHealthLocation = null;
    for (RobotInfo attackableRobotInfo : attackableRobotInfos) {
      double enemyRobotHealth = attackableRobotInfo.health;
      if (enemyRobotHealth < minimumHealth) {
        minimumHealth = enemyRobotHealth;
        minimumHealthLocation = attackableRobotInfo.location;
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
    attemptMove(desiredDirection);
  }
    
  private static void attemptMove(Direction desiredDirection) throws GameActionException {
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
        if (RC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
          MapLocation myLocation = RC.getLocation();
          Direction desiredDirection = myLocation.directionTo(ENEMY_HQ_LOCATION);
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
    
    public RobotInfo[] getNearbyRobots() {
      return nearbyRobots;
    }
  }

  private static void debug(String string) {
    debug(string, 0);
  }
  
  private static void debug(String string, int debugLevel) {
    if (debugLevel < DEBUG_LEVEL) {
      System.out.println(string);
    }
  }

}
