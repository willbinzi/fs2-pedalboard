package pedals

def overdrive[F[_]](threshold: Float): Pedal[F] =
  Pedal(sample => math.min(math.max(sample, -threshold), threshold))

def asymmetricOverdrive[F[_]](threshold: Float): Pedal[F] =
  Pedal(sample => math.min(sample, threshold))
