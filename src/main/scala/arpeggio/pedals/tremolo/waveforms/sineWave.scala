package arpeggio
package pedals.tremolo.waveforms

import arpeggio.constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import arpeggio.pedals.tremolo.waveforms.util.waveSection
import fs2.{Pure, Stream}

import java.lang.Math

def sineWave(cycleLengthInSeconds: Float): Stream[Pure, Float] =
  val slopeLengthInChunks = (2 * cycleLengthInSeconds * CHUNKS_PER_SECOND).toInt
  val slopeLengthInFrames = slopeLengthInChunks * FRAMES_PER_BUFFER
  waveSection(slopeLengthInChunks, sine(slopeLengthInFrames)).repeat

private def sine(cycleLengthInFrames: Long)(timestamp: Long): Float =
  (1 + Math
    .sin(timestamp.toDouble * 2 * Math.PI / cycleLengthInFrames)
    .toFloat) / 2
