// import line.output.play
import line.input.capture
import mixer.{ getMixer, getSourceDataLine, getTargetDataLine }

import javax.sound.sampled.AudioFormat
import cats.effect.IOApp
import cats.effect.IO
import fs2.Stream
import io.writeSourceDataLine

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
  def run: IO[Unit] = (for {
    inputLine <- Stream.eval(IO(getMixer(KOMPLETE_AUDIO).getTargetDataLine))
    outputLine <- Stream.eval(IO(getMixer(MACBOOK_SPEAKERS).getSourceDataLine))
    _ <- inputLine.capture[IO](AUDIO_FORMAT)
      .through(util.toSamples)
      .through(util.toBytes)
      .through(writeSourceDataLine(
        IO {
          outputLine.open(AUDIO_FORMAT)
          outputLine.start()
          outputLine
        },
        closeAfterUse = false
      ))
  } yield ()).compile.drain
