import cats.effect.{ IO, IOApp, Resource }
import scala.scalanative.unsafe.Zone

object Main extends IOApp.Simple:
  def run: IO[Unit] = appResource.use(_ => IO.unit)
  def appResource: Resource[IO, Unit] = for {
    _ <- io.initPortaudio[IO]
    // TODO: don't use global zone
    given Zone <- io.zone[IO]
    output <- io.outputR[IO]
    input <- io.inputR[IO]
    drive <- pedals.overdrive.blended[IO](0, 0.1)
    reverb <- pedals.reverbR[IO](0.7, 0.5)
    _          <-
      input
        .through(drive)
        .through(reverb)
        .through(output)
        .compile
        .drain
        .toResource
  } yield ()
