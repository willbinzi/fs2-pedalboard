package constants

val MACBOOK_SPEAKERS: String = "MacBook Pro Speakers"
val MACBOOK_MIC: String = "MacBook Pro Microphone"
val KOMPLETE_AUDIO: String = "Komplete Audio 2"
val HEADPHONES: String = "External Headphones"

val SAMPLE_RATE = 44100f
val SAMPLE_SIZE_IN_BITS = 16
val CHANNELS = 2
val SIGNED = true
val BIG_ENDIAN = false

val BYTES_BUFFER_SIZE: Int = 512
val FLOAT_BUFFER_SIZE: Int = BYTES_BUFFER_SIZE / 2

val CHUNKS_PER_SECOND: Float = SAMPLE_RATE / FLOAT_BUFFER_SIZE

// Samples are give as 16 bit signed integers. The full scale is therefore 2^15 = 32768
val FULL_SCALE: Int = 32768
