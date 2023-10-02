import cats.effect.{ IO, ResourceApp }
import constants.{ AUDIO_FORMAT, KOMPLETE_AUDIO, MACBOOK_SPEAKERS }
import dataline.input.captureSamples
import dataline.output.playSamples
import mixer.{ getMixer, getSourceDataLine, getTargetDataLine }
import cats.effect.kernel.Resource

object Main extends ResourceApp.Simple:
  def run: Resource[IO, Unit] = for {
    inputLine  <- Resource.eval(getMixer[IO](KOMPLETE_AUDIO).flatMap(_.getTargetDataLine))
    outputLine <- Resource.eval(getMixer[IO](MACBOOK_SPEAKERS).flatMap(_.getSourceDataLine))
    reverb     <- pedals.reverbR[IO](2, 0.3)
    _          <- Resource.eval(
      inputLine
        .captureSamples[IO](AUDIO_FORMAT)
        .through(reverb)
        .through(outputLine.playSamples[IO](AUDIO_FORMAT)).compile.drain
      )
  } yield ()
