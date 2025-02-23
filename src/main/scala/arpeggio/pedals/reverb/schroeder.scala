package arpeggio
package pedals.reverb

import arpeggio.pedals.delay.echoRepeats
import arpeggio.routing.parallel
import cats.effect.Concurrent

def schroeder[F[_]: Concurrent](
    predelayMillis: Float,
    decayMillis: Float
): Pedal[F] =
  val t1 = predelayMillis
  val t2 = predelayMillis * 1.17f
  val t3 = predelayMillis * 1.34f
  val t4 = predelayMillis * 1.5f
  parallel(
    identity,
    parallel(
      echoRepeats(gain(decayMillis, t1), t1),
      echoRepeats(gain(decayMillis, t2), t2),
      echoRepeats(gain(decayMillis, t3), t3),
      echoRepeats(gain(decayMillis, t4), t4)
    ).andThen(
      allPassStage(0.7, 5)
        .andThen(allPassStage(0.7, 1.7))
    )
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
