package traffic

import akka.actor.{Actor, ActorRef}
import traffic.{CarColor, Config, Draw}

case class UpdateMsg(prevCarPosX: Double) {}

case class CarParams(
                      maxSpeed: Double = 50,       //pixels / second
                      accel: Double = 60,          //pixels / second squared
                      decel: Double = 110,         //pixels / second squared
                      reactionTime: Double = 0.3   //second
                    ) {}


class CarActor(val p: CarParams, var posX: Double, painter: ActorRef, nextCar: ActorRef = null) extends Actor {
  println(f"Initializing car ${self.path.name} at pos ${posX}")
  painter ! Draw(posX, CarColor.Driving)

  var speed = 0.0
  var delayElapsed = 0.0
  var prevDist = 0.0

  def update(): Unit = {
    speed = math.min(math.max(speed, 0.0), p.maxSpeed)
    posX += speed * Config.DELTA_TIME

    if (nextCar != null)
      nextCar ! UpdateMsg(posX)
  }

  def accelerating: Receive = {
    case u: UpdateMsg => {
      speed += p.accel * Config.DELTA_TIME
      update()
      painter ! Draw(posX, CarColor.Accelerating)

      val dist = u.prevCarPosX - posX
      if (dist < Config.MIN_TIME_DIST || speed >= p.maxSpeed)
        context.unbecome()

      prevDist = dist
    }
  }

  def breaking: Receive = {
    case u: UpdateMsg => {
      speed -= p.accel * Config.DELTA_TIME
      update()
      painter ! Draw(posX, CarColor.Breaking)

      val dist = u.prevCarPosX - posX
      if (dist > Config.MAX_TIME_DIST /* || prevDist - dist < 0*/)
        context.unbecome()

      prevDist = dist
    }
  }

  // default state - constant speed
  def receive: Receive = {
    case u: UpdateMsg => {
      update()
      painter ! Draw(posX, CarColor.Driving)
      val dist = u.prevCarPosX - posX

      if ( (dist > Config.MAX_TIME_DIST && speed < p.maxSpeed))
        context.become(willAcceleate)
      else if (dist < Config.MIN_TIME_DIST && speed > 0)
        context.become(willBreak)

      prevDist = dist
    }
  }

  def willAcceleate: Receive = delayCtx(accelerating)

  def willBreak: Receive = delayCtx(breaking)

  /*
  Delays switching to real context by reaction time
  Assuming UpdateMsg is received every DELTA_TIME
   */
  private def delayCtx(targetCtx: Receive): Receive = {
    case u: UpdateMsg => {
      delayElapsed += Config.DELTA_TIME
      update()
      painter ! Draw(posX, CarColor.Reacting)

      if (delayElapsed >= p.reactionTime) {
        delayElapsed = 0.0
        context.become(targetCtx, discardOld = true)
      }
    }
  }
}
