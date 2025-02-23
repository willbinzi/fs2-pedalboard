package arpeggio
package pedals.delay

import arpeggio.constants.SAMPLE_RATE
import arpeggio.routing.Fork
import cats.effect.Concurrent
import fs2.{Chunk, Stream}
import cats.syntax.semigroup.*
import arpeggio.routing.parallel
import arpeggio.pubsub.ChunkedChannel.*

def silence[F[_]](timeInMillis: Float): Stream[F, Float] =
  val timeInFrames = (timeInMillis * SAMPLE_RATE / 1000).toInt
  Stream.chunk(Chunk.constant(0, timeInFrames))

def buffered[F[_]: Concurrent](pedal: Pedal[F]): Pedal[F] = stream =>
  Stream
    .eval(ChunkedChannel.unbounded[F, Float])
    .flatMap(channel =>
      channel.stream
        .through(pedal)
        .concurrently(stream.through(channel.sendAll))
    )

def delayLine[F[_]: Concurrent](timeInMillis: Float): Pedal[F] =
  buffered(silence(timeInMillis) ++ _)

def echo[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeMillis: Float
): Pedal[F] =
  parallel(
    identity,
    echoRepeats(repeatGain, delayTimeMillis).andThen(_.map(_ * repeatGain))
  )

def echoRepeats[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeMillis: Float
): Pedal[F] = stream =>
  Stream
    .resource(Fork[F])
    .flatMap(fork =>
      fork.lOut
        .concurrently(
          fork.in(stream |+| fork.rOut.through(delayLine(delayTimeMillis)).map(_ * repeatGain))
        )
        .through(delayLine(delayTimeMillis))
    )
