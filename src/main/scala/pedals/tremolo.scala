package pedals

import fs2.{ Chunk, Pull, Pure, Stream }
import constants.{ AUDIO_FORMAT, FLOAT_BUFFER_SIZE }

val CHUNKS_PER_SECOND: Float = AUDIO_FORMAT.getSampleRate / FLOAT_BUFFER_SIZE

def waveSection(sectionLengthInChunks: Int, f: Long => Float): Stream[Pure, Float] =
  val buffer = new Array[Float](FLOAT_BUFFER_SIZE)

  def go(chunkNumber: Long): Pull[Pure, Float, Option[Long]] =
    for (i <- 0 until FLOAT_BUFFER_SIZE) do
      buffer(i) = f((chunkNumber * FLOAT_BUFFER_SIZE) + i)
    Pull.output(Chunk.array(buffer)) >> (
      if chunkNumber < sectionLengthInChunks then
        Pull.pure(Some(chunkNumber + 1))
      else
        Pull.pure(None)
    )
  Pull.loop[Pure, Float, Long](go)(0).stream

def triangleWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val slopeLengthInChunks = (cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  val slopeLengthInFrames = slopeLengthInChunks * FLOAT_BUFFER_SIZE
  (waveSection(slopeLengthInChunks, slope(slopeLengthInFrames)) ++
    waveSection(slopeLengthInChunks, x => 1 - slope(slopeLengthInFrames)(x))).repeat

def sineWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val slopeLengthInChunks = (2 * cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  val slopeLengthInFrames = slopeLengthInChunks * FLOAT_BUFFER_SIZE
  waveSection(slopeLengthInChunks, sine(slopeLengthInFrames)).repeat

def sine(cycleLengthInFrames: Long)(timestamp: Long): Float =
  (1 + math.sin(timestamp.toDouble * 2 * math.Pi / cycleLengthInFrames).toFloat) / 2

def slope(slopeLengthInFrames: Long)(timestamp: Long): Float =
  timestamp / (slopeLengthInFrames).toFloat

def squareWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val halfCycleLengthInChunks = (cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  (waveSection(halfCycleLengthInChunks, _ => 1) ++
    waveSection(halfCycleLengthInChunks, _ => 0)).repeat

enum Waveform:
  case Sine, Triangle, Square

def tremolo[F[_]](waveform: Waveform, cycleLengthInSeconds: Float): Pedal[F] =
  val waveFunction = waveform match
    case Waveform.Sine => sineWave(cycleLengthInSeconds)
    case Waveform.Triangle => triangleWave(cycleLengthInSeconds)
    case Waveform.Square => squareWave(cycleLengthInSeconds)

  _.zipWith(waveFunction)(_ * _)
