package pedals

import cats.Semigroup
import cats.effect.Concurrent
import fs2.{Chunk, Stream}

given streamPointwiseAddChunksChunks[F[_]: Concurrent]: Semigroup[Stream[F, Chunk[Float]]] = new:
  def combine(
      x: Stream[F, Chunk[Float]],
      y: Stream[F, Chunk[Float]]
  ): Stream[F, Chunk[Float]] =
    x.parZipWith(y)(_ |+| _)

given streamPointwiseAddChunks[F[_]: Concurrent]: Semigroup[Stream[F, Float]] =
  new:
    def combine(
        x: Stream[F, Float],
        y: Stream[F, Float]
    ): Stream[F, Float] =
      x.chunks.parZipWith(y.chunks)(_ |+| _).unchunks

extension (chunk: Chunk[Float])
  // Pointwise addition of two chunks
  def |+|(other: Chunk[Float]): Chunk[Float] =
    chunk.zipWith(other)(_ + _)

  def *(scalar: Float): Chunk[Float] =
    chunk.map(_ * scalar)
