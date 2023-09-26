import line.input.captureSamples
import mixer.{ getMixer, getSourceDataLine, getTargetDataLine }

import javax.sound.sampled.AudioFormat
import cats.effect.IOApp
import cats.effect.IO
import fs2.Stream
import line.output.playSamples
import javax.sound.sampled.TargetDataLine
import javax.sound.sampled.SourceDataLine

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
      .map(sample => math.min(sample, -0.5f))
      .through(outputLine.playSamples[IO](AUDIO_FORMAT))
