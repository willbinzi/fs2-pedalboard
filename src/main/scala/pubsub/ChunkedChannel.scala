package pubsub

import cats.effect.Concurrent
import fs2.concurrent.Channel
import fs2.{Chunk, Pipe, Stream}

object ChunkedChannel:
  opaque type ChunkedChannel[F[_], A] = Channel[F, Chunk[A]]

  extension [F[_], A](chunkChannel: ChunkedChannel[F, A])
    def observePublishChunks: Pipe[F, A, A] =
      _.chunks.evalTap(chunkChannel.send).unchunks

    def stream: Stream[F, A] = chunkChannel.stream.unchunks

  object ChunkedChannel:
    def unbounded[F[_]: Concurrent, A]: F[ChunkedChannel[F, A]] =
      Channel.unbounded[F, Chunk[A]]
