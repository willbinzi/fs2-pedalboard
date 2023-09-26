import cats.effect.{ IOApp, IO }
import dataline.input.captureSamples
import dataline.output.playSamples
import fs2.Stream
import mixer.{ getMixer, getSourceDataLine, getTargetDataLine }

import javax.sound.sampled.{ AudioFormat, SourceDataLine, TargetDataLine }

val MACBOOK_SPEAKERS: String = "MacBook Pro Speakers"
val MACBOOK_MIC: String = "MacBook Pro Microphone"
val KOMPLETE_AUDIO: String = "Komplete Audio 2"
val HEADPHONES: String = "External Headphones"

val AUDIO_FORMAT = new AudioFormat(
  44100,  // Sample rate
  16,     // Sample size in bits
  2,      // Channels
  true,   // Signed
  true    // Big endian
)

object Main extends IOApp.Simple:
  def run: IO[Unit] = for {
    inputLine <- getMixer[IO](KOMPLETE_AUDIO).flatMap(_.getTargetDataLine)
    outputLine <- getMixer[IO](MACBOOK_SPEAKERS).flatMap(_.getSourceDataLine)
    _ <- signalStream(inputLine, outputLine).compile.drain
  } yield ()

  def signalStream(inputLine: TargetDataLine, outputLine: SourceDataLine): Stream[IO, Unit] =
    inputLine
      .captureSamples[IO](AUDIO_FORMAT)
      .through(outputLine.playSamples[IO](AUDIO_FORMAT))
