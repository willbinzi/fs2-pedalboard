package arpeggio

import arpeggio.io.portaudio.PortAudioAudioSuite
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple:
  def run: IO[Unit] = PortAudioAudioSuite
    .resource[IO]
    .use(audioSuite =>
      audioSuite.input
        .through(pedals.reverb.schroeder(30, 100))
        .through(audioSuite.output)
        .compile
        .drain
    )
