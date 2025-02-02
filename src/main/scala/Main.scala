import cats.effect.{IO, ResourceApp, Resource}

object Main extends ResourceApp.Simple:
  def run: Resource[IO, Unit] = for {
    _ <- Resource.eval(IO.println("Starting"))
    drive <- pedals.overdrive.blended[IO](0.7, 0.1)
    reverb <- pedals.reverbR[IO](0.7, 0.1)
    _ <- Resource.eval(IO.println("Pedals set up"))
    audioSuite <- io.AudioSuite.default[IO]
    _ <- Resource.eval(IO.println("Audio suite set up"))
    _ <-
      audioSuite.input
        .through(drive)
        .through(reverb)
        .through(audioSuite.output)
        .compile
        .drain
        .toResource
  } yield ()
