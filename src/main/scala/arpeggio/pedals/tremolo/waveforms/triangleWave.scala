package arpeggio
package pedals.tremolo.waveforms

import arpeggio.constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import arpeggio.pedals.tremolo.waveforms.util.waveSection
import fs2.{Pure, Stream}

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
