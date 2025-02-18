package arpeggio
package routing

import arpeggio.pubsub.ChunkedTopic.*
import arpeggio.pedals.passThrough
import cats.data.NonEmptySeq
import cats.effect.kernel.syntax.resource.effectResourceOps
import cats.effect.{Concurrent, Resource}
import cats.syntax.applicative.*
import cats.syntax.traverse.*
import fs2.Stream

extension [F[_]: Concurrent, A](stream: Stream[F, A])
  def parZipChunksKeepR(that: Stream[F, A]): Stream[F, A] =
    stream.chunks.parZipWith(that.chunks)((_, b) => b).unchunks

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
        topic <- ChunkedTopic[F, Float].toResource
        streams <- pedals.traverse(pedal => topic.subscribeAwaitUnbounded.map(_.through(pedal)))
      } yield _.through(topic.observePublish).parZipChunksKeepR(streams.reduce)
    )
