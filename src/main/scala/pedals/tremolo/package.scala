package pedals

import pedals.tremolo.waveforms.{ WaveFormType, getWaveform }

package object tremolo:
  def apply[F[_]](waveformType: WaveFormType, cycleLengthInSeconds: Float): Pedal[F] =
    _.zipWith(getWaveform(waveformType, cycleLengthInSeconds))(_ * _)
