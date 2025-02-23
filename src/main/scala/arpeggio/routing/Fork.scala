package arpeggio
package routing

import arpeggio.pubsub.ChunkedTopic.*
import cats.effect.{Concurrent, Resource}
import fs2.{Pipe, Stream}

trait Fork[F[_]] {
  def in: Pipe[F, Float, Nothing]
  def lOut: Stream[F, Float]
  def rOut: Stream[F, Float]
}

object Fork:
  def apply[F[_]: Concurrent]: Resource[F, Fork[F]] = for {
    topic <- Resource.eval(ChunkedTopic[F, Float])
    left <- topic.subscribeAwait(Int.MaxValue)
    right <- topic.subscribeAwait(Int.MaxValue)
  } yield new:
    def in: Pipe[F, Float, Nothing] = topic.publish
    def lOut: Stream[F, Float] = left
    def rOut: Stream[F, Float] = right
