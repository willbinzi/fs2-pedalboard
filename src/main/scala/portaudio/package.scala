package portaudio

import cats.effect.{ Resource, Sync }
import cats.syntax.functor.*
import scala.scalanative.unsafe.*

def init[F[_]: Sync]: Resource[F, Unit] =
  Resource.make(Sync[F].delay(functions.Pa_Initialize()).void)
    (_ => Sync[F].delay(functions.Pa_Terminate()).void)

def printDevices[F[_]: Sync]: F[Unit] = Sync[F].delay {
  val count = functions.Pa_GetDeviceCount()
  (0 to count.value - 1).foreach { i =>
    val info = functions.Pa_GetDeviceInfo(aliases.PaDeviceIndex(i))
    println(s"Device $i: ${(!info).name}")
  }
}
