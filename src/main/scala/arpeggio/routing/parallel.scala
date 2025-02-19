package arpeggio
package routing

import cats.data.NonEmptySeq
import cats.effect.std.CountDownLatch
import cats.effect.Concurrent
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import fs2.concurrent.Topic
import fs2.{Chunk, Stream}

def parallel[F[_]: Concurrent](pedals: Pedal[F]*): Pedal[F] =
  NonEmptySeq.fromSeq(pedals).fold[Pedal[F]](_.map(_ * 0))(parallelNonEmpty)

// Adapted from implementation of broadcastThrough from fs2:
// https://github.com/typelevel/fs2/blob/f8105b7a22842e4f97d188fcf8f11283c2f617e2/core/shared/src/main/scala/fs2/Stream.scala#L239
def parallelNonEmpty[F[_]: Concurrent](pedals: NonEmptySeq[Pedal[F]]): Pedal[F] =
  stream => {
    Stream.force {
      for {
        // topic: contains the chunk that the pipes are processing at one point.
        // until and unless all pipes are finished with it, won't move to next one
        topic <- Topic[F, Chunk[Float]]
        // Coordination: neither the producer nor any consumer starts
        // until and unless all consumers are subscribed to topic.
        allReady <- CountDownLatch[F](pedals.length)
      } yield {
        val checkIn = allReady.release >> allReady.await

        def dump(pipe: Pedal[F]): Stream[F, Float] =
          Stream.resource(topic.subscribeAwait(1)).flatMap { sub =>
            // Wait until all pipes are ready before consuming.
            // Crucial: checkin is not passed to the pipe,
            // so pipe cannot interrupt it and alter the latch count
            Stream.exec(checkIn) ++ pipe(sub.unchunks)
          }

        val dumpAll: Stream[F, Float] = pedals.map(dump).reduce
        // Wait until all pipes are checked in before pulling
        val pump = Stream.exec(allReady.await) ++ topic.publish(stream.chunks)
        dumpAll.concurrently(pump)
      }
    }
  }
