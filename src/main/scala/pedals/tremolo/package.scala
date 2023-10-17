package pedals

import cats.effect.kernel.Ref
import fs2.{Chunk, Stream}
import pedals.tremolo.waveforms.{WaveFormType, getWaveform}

package object tremolo:
  def apply[F[_]](
      waveformType: WaveFormType,
      cycleLengthInSeconds: Float
  ): Pedal[F] =
    _.zipWith(getWaveform(waveformType, cycleLengthInSeconds))(_ * _)

  def interpolated(a: Float, b: Float): Chunk[Float] =
    val step: Float = (b - a) / constants.FLOAT_BUFFER_SIZE
    Chunk(
      (0 until constants.FLOAT_BUFFER_SIZE).map(i => a + (step * i)): _*
    )

  def sweep[F[_]](ref: Ref[F, Float]): Pedal[F] =
    _.chunks
      .zipWith(
        Stream
          .repeatEval(ref.get)
          .scan(
            Chunk.constant(1f, constants.FLOAT_BUFFER_SIZE)
          )((prev, next) => interpolated(prev.last.getOrElse(1f), next))
      )(_ * _)
      .unchunks
