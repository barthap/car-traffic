package traffic

object Config {
  val SIMULATION_TIME = 30  // seconds
  val DELTA_TIME = 0.02     // seconds

  val MIN_TIME_DIST = 100   // min time-distance [secondss] between cars - start breaking when shorter
  val MAX_TIME_DIST = 120   // max time-distance [s] between cars - accelerate when longer

  val FINISH_LINE = 1000    // X position before which first car starts breaking

  val CARS_Y = 150          // y position of all cars (they move along x axis)
  val CAR_SIZE = 10         // car square size

  // window dimmensions
  val WINDOW_WIDTH = 1200
  val WINDOW_HEIGHT = 300
}

object CarPresets {
  val normalCarParams = new CarParams()
  val sleepyDriverParams = new CarParams(reactionTime = 0.5)
  val truckParams = new CarParams(maxSpeed = 40, accel = 30, decel = 70, reactionTime = 0.2)
  val raceCarParams = new CarParams(maxSpeed = 80, accel = 80, decel = 120)
}
