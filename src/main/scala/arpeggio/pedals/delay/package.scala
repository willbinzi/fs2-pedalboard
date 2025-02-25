package arpeggio
package pedals.delay

import arpeggio.constants.SAMPLE_RATE
import cats.effect.Concurrent
import fs2.{Pure, Stream}
import cats.syntax.semigroup.*
import arpeggio.pubsub.ChunkedChannel.*
import arpeggio.pubsub.ChunkedTopic.*
import arpeggio.routing.parallel

def silence(timeInMillis: Float): Stream[Pure, Float] =
  val timeInFrames = (timeInMillis * SAMPLE_RATE / 1000).toInt
  Stream.constant(0f).take(timeInFrames)

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
  for {
    topic <- Stream.eval(ChunkedTopic[F, Float])
    outStream <- Stream.resource(topic.subscribeAwait(1))
    feedbackStream <- Stream.resource(
      topic.subscribeAwait(1).map(_.map(_ * repeatGain))
    )
    out <- outStream
      .concurrently(
        (stream |+| feedbackStream)
          .through(delayLine(delayTimeMillis))
          .through(topic.publish)
      )
  } yield out
