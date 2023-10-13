import cats.effect.{ IO, IOApp, Resource }
import scala.scalanative.unsafe.Zone

object Main extends IOApp.Simple:
  def run: IO[Unit] = appResource.use(_ => IO.unit)
  def appResource: Resource[IO, Unit] = for {
    _ <- portaudio.init[IO].toResource
    _ <- portaudio.streamPointer[IO]
    _ <- IO.println("Should be running now").toResource
    _ <- IO(Thread.sleep(90000)).toResource
    // _ <- portaudio.printDevices[IO].toResource
    // foo <- portaudio.inputOutput[IO].toResource
    // inputStream = foo._1
    // // outputPipe = foo._2
    // // outputPipe <- portaudio.output[IO].toResource
    // _          <-
    //   inputStream
    //     // .through(toSamples)
    //     .evalMapChunk(IO.println)
    //     // .through(outputPipe)
    //     .compile
    //     .drain
    //     .toResource
  } yield ()
