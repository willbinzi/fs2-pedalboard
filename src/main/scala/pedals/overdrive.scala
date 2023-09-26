package pedals

def overdrive[F[_]]: Pedal[F] = 
  Pedal(sample => math.min(sample, -0.5f))