// package mixer

// import cats.effect.kernel.Sync
// import cats.syntax.flatMap.*

// import javax.sound.sampled.{ Mixer, SourceDataLine, TargetDataLine }

// extension (mixer: Mixer)
//   def getSourceDataLine[F[_]: Sync]: F[SourceDataLine] =
//     Sync[F].delay {
//       mixer
//         .getSourceLineInfo
//         .headOption
//         .fold(
//           throw new RuntimeException("No source data line (output) found")
//         )(info => mixer.getLine(info).asInstanceOf[SourceDataLine])
//     }

//   def getTargetDataLine[F[_]: Sync]: F[TargetDataLine] =
//     Sync[F].delay {
//       mixer
//         .getTargetLineInfo
//         .headOption
//         .fold(
//           throw new RuntimeException("No target data line (input) found")
//         )(info => mixer.getLine(info).asInstanceOf[TargetDataLine])
//     }
