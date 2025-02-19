package arpeggio
package pedals.reverb

import arpeggio.pedals.delay.{allPassFilterF, combFilterF}
import arpeggio.pedals.passThrough
import arpeggio.routing.parallel
import cats.effect.Concurrent
import cats.syntax.flatMap.*
import cats.syntax.functor.*

def reverb[F[_]: Concurrent](
    decay: Float,
    mix: Float
): F[Pedal[F]] =
  for {
    comb1 <- combFilterF(decay + 0.009f, 4.799)
    comb2 <- combFilterF(decay, 4.999)
    comb3 <- combFilterF(decay - 0.018f, 5.399)
    comb4 <- combFilterF(decay - 0.036f, 5.801)
    allPass1 <- allPassFilterF(0.7, 1.051)
    allPass2 <- allPassFilterF(0.7, 0.337)
    allPass3 <- allPassFilterF(0.7, 0.113)
  } yield parallel(
    passThrough,
    _.through(allPass1)
      .through(allPass2)
      .through(allPass3)
      .through(parallel(comb1, comb2, comb3, comb4))
      .map(
        _ * (mix * 0.25f)
      ) // Divide by 4 to compensate for the 4 parallel comb filters
  )
