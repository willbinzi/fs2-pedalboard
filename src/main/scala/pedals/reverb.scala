package pedals

import cats.effect.kernel.syntax.resource.*
import cats.effect.{ Concurrent, Resource }
import cats.syntax.all.*
import fs2.concurrent.Topic
import fs2.Chunk

def parallel[F[_]: Concurrent](
  pedal1: Pedal[F],
  pedal2: Pedal[F],
  pedal3: Pedal[F],
  pedal4: Pedal[F],
): Resource[F, Pedal[F]] =
  for {
    topic   <- Topic[F, Chunk[Float]].toResource
    stream1 <- topic.subscribeAwaitUnbounded.map(_.unchunks)
    stream2 <- topic.subscribeAwaitUnbounded.map(_.unchunks)
    stream3 <- topic.subscribeAwaitUnbounded.map(_.unchunks)
    stream4 <- topic.subscribeAwaitUnbounded.map(_.unchunks)
  } yield (
    _
      .chunks
      .evalMap(topic.publish1)
      .zipRight(
        stream1.through(pedal1).chunks |+|
        stream2.through(pedal2).chunks |+|
        stream3.through(pedal3).chunks |+|
        stream4.through(pedal4).chunks
      ).unchunks
  )

def reverbR[F[_]: Concurrent]: Resource[F, Pedal[F]] =
  for {
    comb1   <- delayF(0.742, 4.799).toResource
    comb2   <- delayF(0.733, 4.999).toResource
    comb3   <- delayF(0.715, 5.399).toResource
    comb4   <- delayF(0.697, 5.801).toResource
    combs   <- parallel(comb1, comb2, comb3, comb4)
    allPass1 <- allPassFilterF(0.7, 1.051).toResource
    allPass2 <- allPassFilterF(0.7, 0.337).toResource
    allPass3 <- allPassFilterF(0.7, 0.113).toResource
  } yield (
    _
      .through(allPass1)
      .through(allPass2)
      .through(allPass3)
      .through(combs)
  )
