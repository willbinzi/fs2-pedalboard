package arpeggio
package pedals.reverb

import arpeggio.pedals.delay.echoRepeats
import arpeggio.pedals.passThrough
import arpeggio.routing.parallel
import cats.effect.Concurrent

def schroeder[F[_]: Concurrent](
    predelayTimeInMs: Float, // = 30
    reverbTimeInMs: Float
): Pedal[F] =
  val t1 = predelayTimeInMs
  val t2 = predelayTimeInMs * 1.17f
  val t3 = predelayTimeInMs * 1.34f
  val t4 = predelayTimeInMs * 1.5f
  parallel(
    passThrough,
    parallel(
      echoRepeats(gain(reverbTimeInMs, t1), t1),
      echoRepeats(gain(reverbTimeInMs, t2), t2),
      echoRepeats(gain(reverbTimeInMs, t3), t3),
      echoRepeats(gain(reverbTimeInMs, t4), t4)
    ).andThen(
      allPassStage(0.7, 5)
        .andThen(allPassStage(0.7, 1.7))
    )
  )

def gain(reverbTimeInMs: Float, predelayTimeInMs: Float): Float =
  scala.math.pow(2, (-3f * predelayTimeInMs) / reverbTimeInMs).toFloat

def allPassStage[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): Pedal[F] = parallel(
  _.map(_ * -repeatGain),
  echoRepeats(repeatGain, delayTimeInSeconds).andThen(_.map(_ * (1 - repeatGain * repeatGain)))
)
