package arpeggio
package pedals.reverb

import arpeggio.pedals.delay.{allPassFilterF, combFilterF}
import arpeggio.pedals.passThrough
import arpeggio.routing.parallel
import cats.effect.kernel.syntax.resource.effectResourceOps
import cats.effect.{Concurrent, Resource}

def reverbRepeatsR[F[_]: Concurrent](
    decay: Float,
    mix: Float
): Resource[F, Pedal[F]] =
  for {
    comb1 <- combFilterF(decay + 0.009f, 4.799).toResource
    comb2 <- combFilterF(decay, 4.999).toResource
    comb3 <- combFilterF(decay - 0.018f, 5.399).toResource
    comb4 <- combFilterF(decay - 0.036f, 5.801).toResource
    combs <- parallel(comb1, comb2, comb3, comb4)
    allPass1 <- allPassFilterF(0.7, 1.051).toResource
    allPass2 <- allPassFilterF(0.7, 0.337).toResource
    allPass3 <- allPassFilterF(0.7, 0.113).toResource
  } yield (
    _.through(allPass1)
      .through(allPass2)
      .through(allPass3)
      .through(combs)
      .map(
        _ * (mix * 0.25f)
      ) // Divide by 4 to compensate for the 4 parallel comb filters
  )

def reverbR[F[_]: Concurrent](decay: Float, mix: Float): Resource[F, Pedal[F]] =
  for {
    reverbRepeats <- reverbRepeatsR[F](decay, mix)
    withDry <- parallel(passThrough, reverbRepeats)
  } yield withDry
