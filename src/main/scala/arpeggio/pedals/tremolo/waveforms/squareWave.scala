package arpeggio.pedals.tremolo.waveforms

import arpeggio.constants.CHUNKS_PER_SECOND
import arpeggio.pedals.tremolo.waveforms.util.waveSection
import fs2.{Pure, Stream}

def squareWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val halfCycleLengthInChunks = (cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  (waveSection(halfCycleLengthInChunks, _ => 1) ++
    waveSection(halfCycleLengthInChunks, _ => 0)).repeat
