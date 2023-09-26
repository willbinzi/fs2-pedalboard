import cats.effect.{ IOApp, IO }
import config.{ AUDIO_FORMAT, KOMPLETE_AUDIO, MACBOOK_SPEAKERS }
import dataline.input.captureSamples
import dataline.output.playSamples
import fs2.Stream
import mixer.{ getMixer, getSourceDataLine, getTargetDataLine }

import javax.sound.sampled.{ SourceDataLine, TargetDataLine }

object Main extends IOApp.Simple:
  def run: IO[Unit] = for {
    inputLine  <- getMixer[IO](KOMPLETE_AUDIO).flatMap(_.getTargetDataLine)
    outputLine <- getMixer[IO](MACBOOK_SPEAKERS).flatMap(_.getSourceDataLine)
    _          <- signalStream(inputLine, outputLine).compile.drain
  } yield ()

  def signalStream(inputLine: TargetDataLine, outputLine: SourceDataLine): Stream[IO, Unit] =
    inputLine
      .captureSamples[IO](AUDIO_FORMAT)
      .through(outputLine.playSamples[IO](AUDIO_FORMAT))
