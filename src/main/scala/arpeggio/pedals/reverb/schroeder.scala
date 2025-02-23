package arpeggio
package pedals.reverb

import arpeggio.pedals.delay.echoRepeats
import arpeggio.routing.parallel
import cats.effect.Concurrent

def schroeder[F[_]: Concurrent](
    predelayMillis: Float,
    decayMillis: Float,
    mix: Float
): Pedal[F] =
  parallel(
    identity,
    parallel(
      // Create 4 echo stages with slightly differing delays and gain factors
      Seq(1f, 1.17f, 1.34f, 1.5f)
        .map(predelayMillis * _)
        .map(t => echoRepeats(gain(decayMillis, t), t)): _*
    )
      .andThen(allPassStage(0.7, 5))
      .andThen(allPassStage(0.7, 1.7))
      .andThen(_.map(_ * mix))
  )

def gain(decayMillis: Float, predelayMillis: Float): Float =
  scala.math.pow(2, (-3f * predelayMillis) / decayMillis).toFloat

def allPassStage[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeMillis: Float
): Pedal[F] = parallel(
  _.map(_ * -repeatGain),
  echoRepeats(repeatGain, delayTimeMillis)
    .andThen(_.map(_ * (1 - repeatGain * repeatGain)))
)
