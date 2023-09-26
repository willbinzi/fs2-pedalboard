package pedals

import fs2.Pipe

type Pedal[F[_]] = Pipe[F, Float, Float]

object Pedal:
  def apply[F[_]](f: Float => Float): Pedal[F] =
    _.map(f)
