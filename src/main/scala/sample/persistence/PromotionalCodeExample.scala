package sample.persistence

//#persistent-actor-example
import akka.actor._
import akka.persistence._

//comandos
case class DarDescuento(valor: Float)
case class AplicarDescuento()

//Eventos
case class DescuentoAplicado(estadoBono: EstadoBono)

case class EstadoBono(maxNumUso: Int, fechaFin: String, porcentajeDescuento: Float) {
  def darDescuento(valor: Float) = valor * porcentajeDescuento;
  def aplicarDescuento(descuentoAplicado: DescuentoAplicado): EstadoBono = copy(descuentoAplicado.estadoBono.maxNumUso-1, fechaFin, porcentajeDescuento)
}

class PromotionalCodeExample extends PersistentActor {
  override def persistenceId = "bancolombia2016"

  var state = EstadoBono(50, "", 0.1f)

  def darDescuento(value: Float): Float = state.darDescuento(value)

  def darEstado(descuentoAplicado: DescuentoAplicado) = state = descuentoAplicado.estadoBono

  def aplicarDescuento(descuentoAplicado: DescuentoAplicado): Unit = state = descuentoAplicado.estadoBono.aplicarDescuento(descuentoAplicado)

  val receiveRecover: Receive = {
    case evt: DescuentoAplicado                                 => darEstado(evt)
    case SnapshotOffer(_, snapshot: EstadoBono) =>
      println("offered state = " + snapshot)
      state = snapshot
  }

  val receiveCommand: Receive = {
    case DarDescuento(value) =>
      println(darDescuento(value))
    case AplicarDescuento =>
      aplicarDescuento(DescuentoAplicado(state))
      persist(DescuentoAplicado(state))(darEstado)
    case "snap"  => saveSnapshot(state)
    case "print" => println(state)
  }

}
//#persistent-actor-example

object PromotionalCodeExample extends App {

  val system = ActorSystem("example")
  val persistentActor = system.actorOf(Props[PromotionalCodeExample], "persistentActor-5-scala")

    persistentActor ! "print"
    persistentActor ! DarDescuento(1000000)
    persistentActor ! AplicarDescuento
    persistentActor ! "snap"

  Thread.sleep(1000)
  system.terminate()
}
