package pedals
package routing

import cats.data.NonEmptySeq
import cats.effect.kernel.syntax.resource.*
import cats.effect.{ Concurrent, Resource }
import cats.syntax.applicative.*
import cats.syntax.reducible.*
import cats.syntax.traverse.*
import fs2.concurrent.Topic
import fs2.{ Chunk, Stream }

def parallel[F[_]: Concurrent](
  pedals: Pedal[F]*
): Resource[F, Pedal[F]] =
  NonEmptySeq.fromSeq(pedals).fold(
    // No pedals provided, so just pass through the input
    identity[Stream[F, Float]].pure[Resource[F, *]]
  )(pedals =>
      for {
        topic   <- Topic[F, Chunk[Float]].toResource
        streams <- pedals.traverse(pedal =>
          topic.subscribeAwaitUnbounded
            .map(_.unchunks.through(pedal))
          )
      } yield (
        _
          .chunks
          .evalMap(topic.publish1)
          .zipRight(streams.reduceMap(_.chunks))
          .unchunks
      )
    )
