package io

import cats.effect.{Resource, Sync}
import cats.syntax.functor.*
import portaudio.aliases
import portaudio.functions

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

val paFloat32 = aliases.PaSampleFormat(0x00000001.toULong)
val paClipOff = aliases.PaStreamFlags(0x00000001.toULong)

def initPortaudio[F[_]: Sync]: Resource[F, Unit] =
  Resource.make(Sync[F].delay(functions.Pa_Initialize()).void)(_ =>
    Sync[F].delay(functions.Pa_Terminate()).void
  )

def zone[F[_]: Sync]: Resource[F, Zone] =
  Resource.make[F, Zone](Sync[F].delay(Zone.open()))(z =>
    Sync[F].delay(z.close())
  )
