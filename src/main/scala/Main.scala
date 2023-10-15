import cats.effect.{ IO, IOApp, Resource }
import scala.scalanative.unsafe.Zone

object Main extends IOApp.Simple:
  def run: IO[Unit] = appResource.use(_ => IO.unit)
  def appResource: Resource[IO, Unit] = for {
    _ <- portaudio.init[IO]
    // TODO: don't use global zone
    given Zone <- portaudio.zone[IO]
    output <- portaudio.outputR[IO]
    input <- portaudio.inputR[IO]
    _          <-
      input
        .through(output)
        .compile
        .drain
        .toResource
  } yield ()
