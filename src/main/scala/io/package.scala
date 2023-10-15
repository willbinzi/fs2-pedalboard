package io

import cats.effect.{ Resource, Sync }
import cats.syntax.functor.*
import portaudio.aliases
import portaudio.functions

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UnsignedRichInt

val paFloat32 = aliases.PaSampleFormat(0x00000001.toULong)
val paClipOff = aliases.PaStreamFlags(0x00000001.toULong)

def initPortaudio[F[_]: Sync]: Resource[F, Unit] =
  Resource.make(Sync[F].delay(functions.Pa_Initialize()).void)
    (_ => Sync[F].delay(functions.Pa_Terminate()).void)

def printDevices[F[_]: Sync]: F[Unit] = Sync[F].delay {
  val count = functions.Pa_GetDeviceCount()
  (0 to count.value - 1).foreach { i =>
    val info = functions.Pa_GetDeviceInfo(aliases.PaDeviceIndex(i))
    println(s"Device $i: ${(!info).name}")
  }
}

def zone[F[_]: Sync]: Resource[F, Zone]  =
  Resource.make[F, Zone](Sync[F].delay(Zone.open()))(z => Sync[F].delay(z.close()))
