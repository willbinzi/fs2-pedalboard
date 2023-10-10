// package dataline.output

// import cats.effect.kernel.Sync
// import fs2.Pipe

// import javax.sound.sampled.{ AudioFormat, SourceDataLine }

// extension (line: SourceDataLine)
//   def playSamples[F[_]: Sync](format: AudioFormat): Pipe[F, Float, Unit] =
//     pack.toBytes andThen
//       util.writeSourceDataLine(
//         Sync[F].delay {
//           line.open(format)
//           line.start()
//           line
//         }
//       )
