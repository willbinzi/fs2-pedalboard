package pedals

import cats.effect.kernel.syntax.resource.*
import cats.effect.{ Concurrent, Resource }
import cats.syntax.all.*
import fs2.concurrent.Topic
import fs2.Chunk

def reverbR[F[_]: Concurrent]: Resource[F, Pedal[F]] =
  for {
    topic    <- Topic[F, Chunk[Float]].toResource
    stream1  <- topic.subscribeAwaitUnbounded.map(_.unchunks)
    stream2  <- topic.subscribeAwaitUnbounded.map(_.unchunks)
    stream3  <- topic.subscribeAwaitUnbounded.map(_.unchunks)
    stream4  <- topic.subscribeAwaitUnbounded.map(_.unchunks)
    delay1   <- delayF(0.742, 4.799).toResource
    delay2   <- delayF(0.733, 4.999).toResource
    delay3   <- delayF(0.715, 5.399).toResource
    delay4   <- delayF(0.697, 5.801).toResource
    allPass1 <- allPassFilterF(0.7, 1.051).toResource
    allPass2 <- allPassFilterF(0.7, 0.337).toResource
    allPass3 <- allPassFilterF(0.7, 0.113).toResource
  } yield (
    _
      .through(allPass1)
      .through(allPass2)
      .through(allPass3)
      .chunks
      .evalMap(topic.publish1)
      .zipRight(
        stream1.through(delay1).chunks |+|
        stream2.through(delay2).chunks |+|
        stream3.through(delay3).chunks |+|
        stream4.through(delay4).chunks
      ).unchunks
  )
