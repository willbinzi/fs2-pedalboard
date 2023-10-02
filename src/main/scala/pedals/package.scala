import cats.Semigroup
import fs2.{ Chunk, Stream }

package object pedals {
  implicit def streamPointwiseAddChunks[F[_]]: Semigroup[Stream[F, Chunk[Float]]] =
    new Semigroup[Stream[F, Chunk[Float]]]:
      def combine(x: Stream[F, Chunk[Float]], y: Stream[F, Chunk[Float]]): Stream[F, Chunk[Float]] =
        x.zipWith(y)(_ pointWiseAdd _)

  extension (chunk: Chunk[Float])
    def pointWiseAdd(other: Chunk[Float]): Chunk[Float] =
      chunk.zipWith(other)(_ + _)

    def * (scalar: Float): Chunk[Float] =
      chunk.map(_ * scalar)
}
