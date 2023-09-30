package pedals.tremolo.waveforms

import constants.CHUNKS_PER_SECOND
import fs2.{ Stream, Pure }
import pedals.tremolo.waveforms.util.waveSection

def squareWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val halfCycleLengthInChunks = (cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  (waveSection(halfCycleLengthInChunks, _ => 1) ++
    waveSection(halfCycleLengthInChunks, _ => 0)).repeat
