package constants

val MACBOOK_SPEAKERS: String = "MacBook Pro Speakers"
val MACBOOK_MIC: String = "MacBook Pro Microphone"
val KOMPLETE_AUDIO: String = "Komplete Audio 2"
val HEADPHONES: String = "External Headphones"

val SAMPLE_RATE = 44100f
val FRAMES_PER_BUFFER = 256
val BYTES_PER_BUFFER = FRAMES_PER_BUFFER * 4

val CHUNKS_PER_SECOND: Float = SAMPLE_RATE / FRAMES_PER_BUFFER
