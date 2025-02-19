package arpeggio
package pedals.reverb

import arpeggio.pedals.delay.{allPassFilter, combFilter}
import arpeggio.pedals.passThrough
import arpeggio.routing.parallel
import cats.effect.Concurrent

def reverb[F[_]: Concurrent](
    decay: Float,
    mix: Float
): Pedal[F] =
  parallel(
    passThrough,
    _.through(allPassFilter(0.7, 1.051))
      .through(allPassFilter(0.7, 0.337))
      .through(allPassFilter(0.7, 0.113))
      .through(
        parallel(
          combFilter(decay + 0.009f, 4.799),
          combFilter(decay, 4.999),
          combFilter(decay - 0.018f, 5.399),
          combFilter(decay - 0.036f, 5.801)
        )
      )
      .map(
        _ * (mix * 0.25f)
      ) // Divide by 4 to compensate for the 4 parallel comb filters
  )
