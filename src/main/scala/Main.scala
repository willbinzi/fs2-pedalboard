import line.output.play
import line.input.record
import mixer.{ getMixer, getSourceDataLine, getTargetDataLine }

import javax.sound.sampled.AudioFormat

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

@main def go: Unit =
  val outputLine = getMixer(MACBOOK_SPEAKERS)
    .getSourceDataLine

  val inputLine = getMixer(KOMPLETE_AUDIO)
    .getTargetDataLine

  outputLine.play(inputLine.record(AUDIO_FORMAT))

  inputLine.stop()
  inputLine.close()
