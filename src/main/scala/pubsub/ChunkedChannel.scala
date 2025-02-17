package pubsub

import cats.effect.Concurrent
import cats.syntax.functor.toFunctorOps
import cats.Functor
import fs2.concurrent.Channel
import fs2.{Chunk, Pipe, Stream}

object ChunkedChannel:
  opaque type ChunkedChannel[F[_], A] = Channel[F, Chunk[A]]

  extension [F[_]: Functor, A](chunkChannel: ChunkedChannel[F, A])
    def observeSend: Pipe[F, A, A] =
      _.chunks.flatMap(chunk => Stream.evalUnChunk(chunkChannel.send(chunk).as(chunk)))

    def stream: Stream[F, A] = chunkChannel.stream.unchunks

  object ChunkedChannel:
    def unbounded[F[_]: Concurrent, A]: F[ChunkedChannel[F, A]] =
      Channel.unbounded[F, Chunk[A]]
