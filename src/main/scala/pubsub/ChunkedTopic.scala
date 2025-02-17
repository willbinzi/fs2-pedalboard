package pubsub

import cats.effect.{Concurrent, Resource}
import cats.syntax.functor.toFunctorOps
import cats.Functor
import fs2.concurrent.Topic
import fs2.{Chunk, Pipe, Stream}

object ChunkedTopic:
  opaque type ChunkedTopic[F[_], A] = Topic[F, Chunk[A]]

  extension [F[_]: Functor, A](chunkTopic: ChunkedTopic[F, A])
    def observePublish: Pipe[F, A, A] =
      _.chunks.flatMap(chunk => Stream.evalUnChunk(chunkTopic.publish1(chunk).as(chunk)))

    def subscribeAwaitUnbounded: Resource[F, Stream[F, A]] =
      chunkTopic.subscribeAwaitUnbounded.map(_.unchunks)

  object ChunkedTopic:
    def apply[F[_]: Concurrent, A]: F[ChunkedTopic[F, A]] =
      Topic[F, Chunk[A]]
