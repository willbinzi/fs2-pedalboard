package portaudio

import cats.effect.Sync
import cats.syntax.functor.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

def init[F[_]: Sync]: F[Unit] =
  Sync[F].delay(functions.Pa_Initialize()).void

def printDevices[F[_]: Sync]: F[Unit] = Sync[F].delay {
  val count = functions.Pa_GetDeviceCount()
  (0 to count.value - 1).foreach { i =>
    val info = functions.Pa_GetDeviceInfo(aliases.PaDeviceIndex(i))
    println(s"Device $i: ${(!info).name}")
  }
}

def inputStream[F[_]: Sync]: F[fs2.Stream[F, Byte]] =
  val buffer = stackalloc[Byte](512)
  val streamPointer: Ptr[Ptr[aliases.PaStream]] = stackalloc[Ptr[aliases.PaStream]]()
  Sync[F].delay {
    functions.Pa_OpenDefaultStream(
      streamPointer,
      1,
      0,
      // aliases.PaSampleFormat.paFloat32,
      aliases.PaSampleFormat(0x00000008.toULong),  // aliases.PaSampleFormat.paFloat16
      44100,
      256.toULong,
      null,
      null
    )
    functions.Pa_StartStream(!streamPointer)
    !streamPointer
  }.map { streamPointer =>
    fs2.Stream.repeatEval(Sync[F].delay {
      // val buffer = stackalloc[Float](256)
      functions.Pa_ReadStream(streamPointer, buffer, 256.toULong)
      buffer
    }).flatMap { buffer =>
      fs2.Stream.emits((0 to 255).map(i => buffer(i).toByte))
    }
  }

// def output[F[_]: Sync]: F[fs2.Pipe[F, Byte, Nothing]] =
//   val buffer = stackalloc[Byte](512)
//   val streamPointer: Ptr[Ptr[aliases.PaStream]] = stackalloc[Ptr[aliases.PaStream]]()
//   Sync[F].delay {
//     functions.Pa_OpenDefaultStream(
//       streamPointer,
//       0,
//       1,
//       // aliases.PaSampleFormat.paFloat32,
//       aliases.PaSampleFormat(0x00000008.toULong),  // aliases.PaSampleFormat.paFloat16
//       44100,
//       256.toULong,
//       null,
//       null
//     )
//     functions.Pa_StartStream(!streamPointer)
//     !streamPointer
//   }.map { streamPointer =>
//     // val buffer = stackalloc[Byte](512)
//     _.chunks.evalMap { chunk =>
//       Sync[F].delay {
//         val fs2.Chunk.ArraySlice(b, off, len) = chunk.toArraySlice[Byte]
//         (0 to len - 1).foreach { i =>
//           buffer(i) = b(off + i)
//         }
//         functions.Pa_WriteStream(streamPointer, buffer, 256.toULong)
//       }
//     }.drain
//   }

def inputOutput[F[_]: Sync]: F[(fs2.Stream[F, Byte], fs2.Pipe[F, Byte, Nothing])] =
  val inputBuffer = stackalloc[Byte](512)
  val outputBuffer = stackalloc[Byte](512)
  val streamPointer: Ptr[Ptr[aliases.PaStream]] = stackalloc[Ptr[aliases.PaStream]]()
  Sync[F].delay {
    functions.Pa_OpenDefaultStream(
      streamPointer,
      1,
      1,
      // aliases.PaSampleFormat.paFloat32,
      aliases.PaSampleFormat(0x00000008.toULong),  // aliases.PaSampleFormat.paFloat16
      44100,
      256.toULong,
      null,
      null
    )
    functions.Pa_StartStream(!streamPointer)
  }.map { _ =>
    (
      fs2.Stream.repeatEval(Sync[F].delay {
        // val buffer = stackalloc[Float](256)
        functions.Pa_ReadStream(!streamPointer, inputBuffer, 256.toULong)
        inputBuffer
      }).flatMap { buffer =>
        fs2.Stream.emits((0 to 255).map(i => buffer(i).toByte))
      },
      _.chunks.evalMap { chunk =>
        Sync[F].delay {
          val fs2.Chunk.ArraySlice(b, off, len) = chunk.toArraySlice[Byte]
          (0 to len - 1).foreach { i =>
            outputBuffer(i) = b(off + i)
          }
          functions.Pa_WriteStream(!streamPointer, outputBuffer, 256.toULong)
        }
      }.drain
    )
  }

  // def play[F[_]]: fs2.Pipe[F, Byte, Unit] =
  //   _.evalMap { byte =>
  //     Sync[F].delay {
  //       val streamPointer: Ptr[Ptr[aliases.PaStream]] = stackalloc[Ptr[aliases.PaStream]]()
  //       functions.Pa_OpenDefaultStream(
  //         streamPointer,
  //         0,
  //         1,
  //         // aliases.PaSampleFormat.paFloat32,
  //         aliases.PaSampleFormat(0x00000008.toULong),  // aliases.PaSampleFormat.paFloat16
  //         44100,
  //         256.toULong,
  //         null,
  //         null
  //       )
        // functions.Pa_StartStream(!streamPointer)
        // val buffer = stackalloc[Byte](512)
        // buffer(0) = byte
        // functions.Pa_WriteStream(!streamPointer, buffer, 256.toULong)
  //       functions.Pa_StopStream(!streamPointer)
  //       functions.Pa_CloseStream(!streamPointer)
  //     }
  //   }

// extension (deviceInfo: structs.PaDeviceInfo)
//   def toStreamParameters(sampleRate: Long): structs.PaStreamParameters =
//     structs.PaStreamParameters(
//       device = aliases.PaDeviceIndex(0),
//       channelCount = 1,
//       sampleFormat = aliases.PaSampleFormat.paFloat32,
//       suggestedLatency = 0,
//       hostApiSpecificStreamInfo = null
//     )

// def getStreamParameters(device: structs.PaDeviceIndex): structs.PaStreamParameters =
//   structs.PaStreamParameters(
//     device,
//     2,

//   )
