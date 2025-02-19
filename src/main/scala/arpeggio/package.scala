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
      x.chunkN(constants.FRAMES_PER_BUFFER)
        .parZipWith(y.chunkN(constants.FRAMES_PER_BUFFER))(_ |+| _)
        .unchunks

extension (chunk: Chunk[Float])
  // Pointwise addition of two chunks
  def |+|(other: Chunk[Float]): Chunk[Float] =
    chunk.zipWith(other)(_ + _)
