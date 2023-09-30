package constants

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
  false   // Big endian
)

val BYTES_BUFFER_SIZE: Int = 4096
val FLOAT_BUFFER_SIZE: Int = BYTES_BUFFER_SIZE / 2

// Samples are give as 16 bit signed integers. The full scale is therefore 2^15 = 32768
val FULL_SCALE: Int = 32768
