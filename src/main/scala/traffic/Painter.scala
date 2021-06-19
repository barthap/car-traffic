package traffic

import akka.actor.Actor
import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

object CarColor extends Enumeration {
  protected case class Val(value: Color) extends super.Val {}
  type CarColor = Value

  import scala.language.implicitConversions
  implicit def valueToColor(x: Value): Val = x.asInstanceOf[Val]

  val Accelerating = Val(Color.GREEN)
  val Breaking = Val(Color.RED)
  val Reacting = Val(Color.YELLOW)
  val Driving = Val(Color.BLUE)
}
import CarColor._

case class Draw(posX: Double, carColor: CarColor) {}

class Painter(val group: Group) extends Actor {
  println("Initializing painter")
  var rectMap = scala.collection.mutable.Map[String, Rectangle]()

  def receive = {
    case updatePos: Draw => {
      val name = sender().path.name
      val rect = rectMap.getOrElseUpdate(name, {
        val rect = new Rectangle(updatePos.posX, Config.CARS_Y, Config.CAR_SIZE, Config.CAR_SIZE)
        group.getChildren.add(rect)
        rect
      })
      rect.setFill(updatePos.carColor.value)
      rect.setX(updatePos.posX)
    }
  }
}
