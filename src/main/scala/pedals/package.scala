package pedals

import fs2.Pipe

type Pedal[F[_]] = Pipe[F, Float, Float]

