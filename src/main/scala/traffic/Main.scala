package traffic

import akka.actor.{ActorSystem, Props, Terminated}
import javafx.application.Application
import javafx.scene.shape.Line
import javafx.scene.{Group, Scene}
import javafx.stage.Stage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * I see at least 2 approaches:
 * - (Currently implemented) Scheduler (a main thread actor) sends Update msg to first car
 *   and then the update message is chained to car behind him  (simpler and shorter implementation)
 * -  Scheduler broadcasts update message to all cars, and each car knows only car before him, he asks repeatedly
 *    for position. IMO it's more correct and extensible
 *
 * For the first car (in both cases), the red light / finish line is treated as the car before him.
 *
 * I am not happy with car dynamics at all. I could add more factors, but the best solution would be to
 * add some real physics equations, e.g. from Adaptive Cruise Control systems (variable acceleration and breaking
 * based on distance).
 *
 * I probably spent too much time on JavaFX visualisation instead of designing proper Actor system.
 */

class MainApp extends Application {

  private def simulate(system: ActorSystem, group: Group): Future[Unit] = {
    // Painter actor must dispatch messages on JavaFX UI thread
    val painter = system.actorOf(Props(new Painter(group)).withDispatcher("javafx-dispatcher"), name = "painter")

    val car4 = system.actorOf(Props(new CarActor(CarPresets.sleepyDriverParams, 20,  painter)),       name = "car4")
    val car3 = system.actorOf(Props(new CarActor(CarPresets.truckParams,        70,  painter, car4)), name = "car3")
    val car2 = system.actorOf(Props(new CarActor(CarPresets.normalCarParams,    120, painter, car3)), name = "car2")
    val car1 = system.actorOf(Props(new CarActor(CarPresets.raceCarParams,      170, painter, car2)), name = "car1")

    Future[Unit] {
      var timeElapsed = 0.0
      println("Starting simulation...")
      while (timeElapsed < Config.SIMULATION_TIME) {
        // First car treats finish line as the car before him
        car1 ! UpdateMsg(Config.FINISH_LINE)

        Thread.sleep((1000 * Config.DELTA_TIME).toLong)
        timeElapsed += Config.DELTA_TIME
      }
    }
  }

  override def start(primaryStage: Stage) {
    primaryStage.setTitle("Car Simulation")


    println("Creating actor system")
    val system = ActorSystem("Default")
    val root = new Group

    val sim = simulate(system, root)

    val termination: Future[Terminated] = sim flatMap { _ =>
      println("Simulation complete, terminating...");
      system.terminate()
    }

    termination onComplete { _ =>
      println("Actor System terminated. Window can now be closed")
    }

    // handle close window click
    primaryStage.setOnCloseRequest(_ => {
      Await.result(termination, 10.seconds)
    })

    val line = new Line(Config.FINISH_LINE, 0, Config.FINISH_LINE, Config.WINDOW_HEIGHT)
    root.getChildren.add(line)

    val scene = new Scene(root, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT)
    primaryStage.setScene(scene)
    primaryStage.show()
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    println("Init app")
    Application.launch(classOf[MainApp], args: _*)
  }
}
