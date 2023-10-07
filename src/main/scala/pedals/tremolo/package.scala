package pedals

import cats.effect.kernel.Ref
import fs2.Stream
import pedals.tremolo.waveforms.{ WaveFormType, getWaveform }

package object tremolo:
  def apply[F[_]](
      waveformType: WaveFormType,
      cycleLengthInSeconds: Float
  ): Pedal[F] =
    _.zipWith(getWaveform(waveformType, cycleLengthInSeconds))(_ * _)

  def sweep[F[_]](ref: Ref[F, Float]): Pedal[F] =
    _.chunks.zipWith(Stream.repeatEval(ref.get))(_ * _).unchunks
