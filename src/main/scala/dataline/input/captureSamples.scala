// package dataline.input

// import cats.effect.kernel.Sync
// import cats.syntax.functor.*
// import constants.BYTES_BUFFER_SIZE
// import fs2.io.readInputStream
// import fs2.Stream

// import java.io.InputStream
// import javax.sound.sampled.{ AudioFormat, AudioInputStream, TargetDataLine }

// extension (line: TargetDataLine)
//   def captureSamples[F[_]: Sync](format: AudioFormat): Stream[F, Float] =
//     captureBytes(format).through(unpack.toSamples)

//   private def captureBytes[F[_]: Sync](format: AudioFormat): Stream[F, Byte] =
//     readInputStream(inputStream(format).widen[InputStream], BYTES_BUFFER_SIZE)

//   private def inputStream[F[_]: Sync](format: AudioFormat): F[AudioInputStream] =
//     Sync[F].delay {
//       line.open(format)
//       line.start()
//       new AudioInputStream(line)
//     }
