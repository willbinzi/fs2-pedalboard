package arpeggio

import cats.Semigroup
import cats.effect.Concurrent
import fs2.{Chunk, Pipe, Stream}

type Pedal[F[_]] = Pipe[F, Float, Float]

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
