package pedals.tremolo.waveforms

import constants.{CHUNKS_PER_SECOND, FLOAT_BUFFER_SIZE}
import fs2.{Pure, Stream}
import pedals.tremolo.waveforms.util.waveSection

def sineWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val slopeLengthInChunks = (2 * cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  val slopeLengthInFrames = slopeLengthInChunks * FLOAT_BUFFER_SIZE
  waveSection(slopeLengthInChunks, sine(slopeLengthInFrames)).repeat

private def sine(cycleLengthInFrames: Long)(timestamp: Long): Float =
  (1 + math
    .sin(timestamp.toDouble * 2 * math.Pi / cycleLengthInFrames)
    .toFloat) / 2
