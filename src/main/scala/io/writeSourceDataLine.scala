package io

import cats.effect.kernel.Sync
import fs2.{ Chunk, Pipe, Stream }

import javax.sound.sampled.SourceDataLine

// Implementations in this file are basically copied wholesale from fs2.io.writeOutputStream

extension (line: SourceDataLine)
  def writeChunk[F[_]](c: Chunk[Byte])(implicit F: Sync[F]): F[Unit] =
    val Chunk.ArraySlice(b, off, len) = c.toArraySlice[Byte]
    F.interruptible {
      line.write(b, off, len)
      ()
    }

def writeSourceDataLine[F[_]](
    fsdl: F[SourceDataLine]
)(implicit F: Sync[F]): Pipe[F, Byte, Nothing] =
  s => {
    Stream.bracket(fsdl)(sdl => F.blocking(sdl.close()))
      .flatMap(sdl =>
        s.chunks.foreach(sdl.writeChunk) ++
          Stream.exec(F.blocking(sdl.flush()))
      )
  }


