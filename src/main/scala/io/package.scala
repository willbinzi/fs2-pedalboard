package io

import cats.effect.{Resource, Sync}
import cats.syntax.functor.*
import portaudio.aliases
import portaudio.functions

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

val paFloat32 = aliases.PaSampleFormat(0x00000001.toCSize)
val paClipOff = aliases.PaStreamFlags(0x00000001.toCSize)

def initPortaudio[F[_]: Sync]: Resource[F, Unit] =
  Resource.make(Sync[F].delay(functions.Pa_Initialize()).void)(_ =>
    Sync[F].delay(functions.Pa_Terminate()).void
  )
