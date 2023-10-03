package pedals

import cats.effect.kernel.syntax.resource.*
import cats.effect.{ Concurrent, Resource }
import pedals.routing.parallel

def reverbRepeatsR[F[_]: Concurrent](mix: Float): Resource[F, Pedal[F]] =
  for {
    comb1    <- combFilterF(0.742, 4.799).toResource
    comb2    <- combFilterF(0.733, 4.999).toResource
    comb3    <- combFilterF(0.715, 5.399).toResource
    comb4    <- combFilterF(0.697, 5.801).toResource
    combs    <- parallel(comb1, comb2, comb3, comb4)
    allPass1 <- allPassFilterF(0.7, 1.051).toResource
    allPass2 <- allPassFilterF(0.7, 0.337).toResource
    allPass3 <- allPassFilterF(0.7, 0.113).toResource
  } yield (
    _
      .through(allPass1)
      .through(allPass2)
      .through(allPass3)
      .through(combs)
      .map(_ * 0.8f)
  )

def reverbR[F[_]: Concurrent]: Resource[F, Pedal[F]] =
  for {
    reverbRepeats <- reverbRepeatsR[F](0.8f)
    withDry       <- parallel(passThrough, reverbRepeats)
  } yield withDry
