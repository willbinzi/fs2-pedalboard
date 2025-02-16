package io.portaudio

import boxing.toBytePointer
import cats.effect.{Resource, Sync}
import cbindings.portaudio.functions.{Pa_ReadStream, Pa_WriteStream}
import constants.FRAMES_PER_BUFFER
import fs2.{Chunk, Pull, Pipe, Stream}
import io.AudioSuite

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

object PortAudioAudioSuite:
  def apply[F[_]](using F: Sync[F]): Resource[F, AudioSuite[F]] =
    for {
      _ <- initPortaudio[F]
      pStream <- inputOutputStreamPointer[F]
    } yield new AudioSuite[F]:
      def input: Stream[F, Float] =
        // Scala native 0.4 is single threaded so we can re-use the same buffer
        // Once this project moves to use multiprocessing, this will no longer be possible
        val buffer = new Array[Float](FRAMES_PER_BUFFER)
        Pull
          .eval(F.blocking {
            Pa_ReadStream(
              pStream,
              buffer.atUnsafe(0).toBytePointer,
              FRAMES_PER_BUFFER.toULong
            )
            Chunk.ArraySlice(buffer, 0, FRAMES_PER_BUFFER)
          })
          .flatMap(Pull.output)
          .streamNoScope
          .repeat

      def output: Pipe[F, Float, Nothing] =
        // As above, we can share the same buffer until the project moves to use multiprocessing
        val buffer = new Array[Float](FRAMES_PER_BUFFER)
        _.chunks.foreach { chunk =>
          F.blocking {
            chunk.copyToArray(buffer, 0)
            Pa_WriteStream(
              pStream,
              buffer.atUnsafe(0).toBytePointer,
              FRAMES_PER_BUFFER.toULong
            )
            ()
          }
        }
