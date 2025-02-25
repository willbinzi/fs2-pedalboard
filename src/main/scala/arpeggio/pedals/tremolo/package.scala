package arpeggio
package pedals.tremolo

import arpeggio.constants.SAMPLE_RATE
import arpeggio.pedals.volume
import cats.effect.Concurrent
import fs2.Stream

def squareWave[F[_]: Concurrent](
    cycleLengthMillis: Float
): Pedal[F] =
  val halfCycleLengthInFrames = (cycleLengthMillis * SAMPLE_RATE / 2000).toInt
  volume(
    (Stream.constant(1f).take(halfCycleLengthInFrames) ++
      Stream.constant(0f).take(halfCycleLengthInFrames)).repeat
  )
