import cats.effect.{IO, IOApp, Resource}

object Main extends IOApp.Simple:
  def run: IO[Unit] = appResource.use(_ => IO.unit)
  def appResource: Resource[IO, Unit] = for {
    audioSuite <- io.AudioSuite.default[IO]
    stateRef <- IO.ref(1f).toResource
    controllerState <- controllers.pollControllerStream(stateRef)
    drive <- pedals.overdrive.blended[IO](0, 0.1)
    reverb <- pedals.reverbR[IO](0.7, 0)
    _ <-
      audioSuite.input
        .through(pedals.tremolo.sweep(stateRef))
        .through(drive)
        .through(reverb)
        .through(audioSuite.output)
        .concurrently(controllerState.drain)
        .compile
        .drain
        .toResource
  } yield ()
