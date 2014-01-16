package team000;

public class NullState implements RobotState {

  @Override
  public RobotState run() {
    return this;
  }

}
