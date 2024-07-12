package io

import cats.effect.{Resource, Sync}
import fs2.{Pipe, Stream}

trait AudioSuite[F[_]] {
  def input: Stream[F, Float]
  def output: Pipe[F, Float, Nothing]
}

object AudioSuite {
  def default[F[_]: Sync]: Resource[F, AudioSuite[F]] =
    for {
      _ <- initPortaudio[F]
      pStream <- io.inputOutputStreamPointer[F]
    } yield new AudioSuite[F] {
      def input: Stream[F, Float] = inputStreamFromPointer[F](pStream)
      def output: Pipe[F, Float, Nothing] = outputPipeFromPointer[F](pStream)
    }
  }
