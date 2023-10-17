package pedals
package routing

import cats.data.NonEmptySeq
import cats.effect.kernel.syntax.resource.*
import cats.effect.{Concurrent, Resource}
import cats.syntax.applicative.*
import cats.syntax.reducible.*
import cats.syntax.traverse.*
import fs2.concurrent.Topic
import fs2.Chunk

def parallel[F[_]: Concurrent](
    pedals: Pedal[F]*
): Resource[F, Pedal[F]] =
  NonEmptySeq
    .fromSeq(pedals)
    .fold(
      // No pedals provided, so just pass through the input
      passThrough.pure[Resource[F, *]]
    )(pedals =>
      for {
        topic <- Topic[F, Chunk[Float]].toResource
        streams <- pedals.traverse(pedal =>
          topic.subscribeAwaitUnbounded
            .map(_.unchunks.through(pedal))
        )
      } yield (
        _.chunks
          .evalMap(topic.publish1)
          .parZipWith(streams.reduceMap(_.chunks))((_, b) => b)
          .unchunks
      )
    )
