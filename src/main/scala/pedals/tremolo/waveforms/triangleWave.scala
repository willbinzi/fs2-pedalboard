package pedals.tremolo.waveforms

import constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import fs2.{Pure, Stream}
import pedals.tremolo.waveforms.util.waveSection

def triangleWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val slopeLengthInChunks = (cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  val slopeLengthInFrames = slopeLengthInChunks * FRAMES_PER_BUFFER
  (waveSection(slopeLengthInChunks, slope(slopeLengthInFrames)) ++
    waveSection(
      slopeLengthInChunks,
      x => 1 - slope(slopeLengthInFrames)(x)
    )).repeat

private def slope(slopeLengthInFrames: Long)(timestamp: Long): Float =
  timestamp / slopeLengthInFrames.toFloat
