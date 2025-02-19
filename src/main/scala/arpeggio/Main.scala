package arpeggio

import arpeggio.io.portaudio.PortAudioAudioSuite
import cats.effect.{IO, Resource, ResourceApp}

object Main extends ResourceApp.Simple:
  def run: Resource[IO, Unit] = for {
    audioSuite <- PortAudioAudioSuite.resource[IO]
    reverb <- pedals.reverb.reverb[IO](0.7, 0.1).toResource
    _ <-
      audioSuite.input
        .through(pedals.overdrive.blended[IO](0.7, 0.1))
        .through(reverb)
        .through(audioSuite.output)
        .compile
        .drain
        .toResource
  } yield ()
