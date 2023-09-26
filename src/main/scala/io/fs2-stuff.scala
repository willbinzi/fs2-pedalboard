package io

import fs2.{ Chunk, Pipe, Stream }

import cats.effect.kernel.Sync

import javax.sound.sampled.SourceDataLine

/** Writes all bytes to the specified `SourceDataLine`. Each chunk is flushed
  * immediately after writing. Set `closeAfterUse` to false if
  * the `SourceDataLine` should not be closed after use.
  */
def writeSourceDataLine[F[_]](
    fos: F[SourceDataLine],
    closeAfterUse: Boolean = true
)(implicit F: Sync[F]): Pipe[F, Byte, Nothing] =
  writeSourceDataLineGeneric(fos, closeAfterUse) { (os, b, off, len) =>
    F.interruptible {
      os.write(b, off, len)
      ()
    }
  }

private def writeSourceDataLineGeneric[F[_]](
    fos: F[SourceDataLine],
    closeAfterUse: Boolean
)(
    write: (SourceDataLine, Array[Byte], Int, Int) => F[Unit]
)(implicit F: Sync[F]): Pipe[F, Byte, Nothing] =
  s => {
    def useOs(os: SourceDataLine): Stream[F, Nothing] =
      s.chunks.foreach { c =>
        val Chunk.ArraySlice(b, off, len) = c.toArraySlice[Byte]
        write(os, b, off, len)
      }

    val os =
      if (closeAfterUse) Stream.bracket(fos)(os => F.blocking(os.close()))
      else Stream.eval(fos)
    os.flatMap(os => useOs(os) ++ Stream.exec(F.blocking(os.flush())))
  }


