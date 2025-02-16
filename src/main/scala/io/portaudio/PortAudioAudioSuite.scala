package io.portaudio

import cats.effect.{Resource, Sync}
import fs2.{Pipe, Stream}
import io.AudioSuite

object PortAudioAudioSuite:
  def apply[F[_]: Sync]: Resource[F, AudioSuite[F]] =
    for {
      _ <- initPortaudio[F]
      pStream <- inputOutputStreamPointer[F]
    } yield new AudioSuite[F]:
      def input: Stream[F, Float] = inputStreamFromPointer[F](pStream)
      def output: Pipe[F, Float, Nothing] = outputPipeFromPointer[F](pStream)
