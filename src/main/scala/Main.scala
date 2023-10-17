import cats.effect.{IO, IOApp, Resource}

object Main extends IOApp.Simple:
  def run: IO[Unit] = appResource.use(_ => IO.unit)
  def appResource: Resource[IO, Unit] = for {
    audioSuite <- io.AudioSuite.default[IO]
    drive <- pedals.overdrive.blended[IO](0, 0.1)
    reverb <- pedals.reverbR[IO](0.7, 0.5)
    _ <-
      audioSuite.input
        .through(drive)
        .through(reverb)
        .through(audioSuite.output)
        .compile
        .drain
        .toResource
  } yield ()
