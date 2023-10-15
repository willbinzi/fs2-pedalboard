package io

import cats.effect.{ Resource, Sync }
import cats.syntax.functor.*
import fs2.Pipe
import portaudio.aliases.PaStream
import portaudio.functions
import portaudio.structs.PaStreamParameters

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

private def outputStreamPointer[F[_]: Sync](using Zone): Resource[F, Ptr[PaStream]] =
  Resource.make[F, Ptr[PaStream]](Sync[F].delay {
    val outputDevice = functions.Pa_GetDefaultOutputDevice()
    val outputLatency = (!functions.Pa_GetDeviceInfo(outputDevice)).defaultLowOutputLatency
    val outputParams = PaStreamParameters(
      outputDevice,
      1,
      paFloat32,
      outputLatency,
      null
    )

    unsafeOpenStream(stackalloc(), null, outputParams)
  })(closeStream)

def outputR[F[_]](using Zone)(implicit F: Sync[F]): Resource[F, Pipe[F, Float, Nothing]] =
  outputStreamPointer.map(pStream =>
    val pFloat = alloc[Float](FRAMES_PER_BUFFER)
    _.chunks.foreach { chunk =>
      (0 until chunk.size).foreach(i =>
        pFloat(i) = chunk(i)
      )
      F.blocking {
        functions.Pa_WriteStream(pStream, pFloat.toBytePointer, FRAMES_PER_BUFFER.toULong)
        ()
      }
    }
  )
