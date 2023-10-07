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
  
  // def run: IO[Unit] =
  //   appResource.use(_ => IO.unit)

  // def appResource: Resource[IO, Unit] = for {
  //   _ <- controllers.setUp[IO].evalOn(MainThread).toResource
  //   stateRef <- IO.ref(1f).toResource
  //   inputLine  <- getMixer[IO](KOMPLETE_AUDIO).flatMap(_.getTargetDataLine).toResource
  //   outputLine <- getMixer[IO](MACBOOK_SPEAKERS).flatMap(_.getSourceDataLine).toResource
  //   _          <-
  //     inputLine
  //       .captureSamples[IO](AUDIO_FORMAT)
  //       .through(pedals.tremolo.sweep[IO](stateRef))
  //       .through(outputLine.playSamples[IO](AUDIO_FORMAT))
  //       .concurrently(controllers.TriggerStr(stateRef).translate[IO, IO](evalOnMain).drain)
  // val evalOnMain: FunctionK[IO, IO] = new FunctionK[IO, IO]:
  //   def apply[A](fa: IO[A]): IO[A] = fa.evalOn(MainThread)
