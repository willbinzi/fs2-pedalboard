import cats.effect.{ IO, Resource, ResourceApp }
import constants.{ AUDIO_FORMAT, KOMPLETE_AUDIO, MACBOOK_SPEAKERS }
import dataline.input.captureSamples
import dataline.output.playSamples
import mixer.{ getMixer, getSourceDataLine, getTargetDataLine }

object Main extends ResourceApp.Simple:
  def run: Resource[IO, Unit] = for {
    inputLine  <- getMixer[IO](KOMPLETE_AUDIO).flatMap(_.getTargetDataLine).toResource
    outputLine <- getMixer[IO](MACBOOK_SPEAKERS).flatMap(_.getSourceDataLine).toResource
    reverb     <- pedals.reverbR[IO](0.4)
    _          <-
      inputLine
        .captureSamples[IO](AUDIO_FORMAT)
        .through(reverb)
        .through(outputLine.playSamples[IO](AUDIO_FORMAT))
        .compile
        .drain
        .toResource
  } yield ()
