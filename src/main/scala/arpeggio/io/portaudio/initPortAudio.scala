package arpeggio.io.portaudio

import cats.effect.{Resource, Sync}
import cats.syntax.functor.toFunctorOps

import cbindings.portaudio.functions

def initPortAudio[F[_]](using F: Sync[F]): Resource[F, Unit] =
  Resource.make(F.delay(functions.Pa_Initialize()).void)(_ =>
    F.delay(functions.Pa_Terminate()).void
  )
