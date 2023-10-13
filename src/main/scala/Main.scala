import cats.effect.{ IO, IOApp, Resource }
import scala.scalanative.unsafe.Zone

object Main extends IOApp.Simple:
  def run: IO[Unit] = appResource.use(_ => IO.unit)
  def appResource: Resource[IO, Unit] = for {
    _ <- portaudio.init[IO].toResource
    // TODO: don't use global zone
    given Zone <- portaudio.zone[IO]
    pointer <- portaudio.streamPointer[IO]
    inputStream = portaudio.foo[IO](pointer)
    outputPipe = portaudio.bar[IO](pointer)
    _          <-
      inputStream
        .through(outputPipe)
        .compile
        .drain
        .toResource
  } yield ()
