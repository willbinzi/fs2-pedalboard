package pedals

import cats.effect.Concurrent
import constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import fs2.{Chunk, Stream}
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.semigroup.*
import pubsub.ChunkedChannel.*

def silence[F[_]](timeInSeconds: Float): Stream[F, Float] =
  val delayTimeInChunks = (timeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunkArray = Array.fill(FRAMES_PER_BUFFER)(0f)
  val silenceChunk = Chunk.array(silenceChunkArray)
  Stream.emit(silenceChunk).repeatN(delayTimeInChunks).unchunks

def combFilterF[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): F[Pedal[F]] =
  ChunkedChannel
    .unbounded[F, Float]
    .map(repeatsChannel =>
      stream =>
        (
          stream |+|
            (silence(delayTimeInSeconds) ++ repeatsChannel.stream
              .mapChunks(_ * repeatGain))
        ).through(repeatsChannel.observePublishChunks)
    )

def allPassFilterF[F[_]: Concurrent](
    repeatGain: Float,
    delayTimeInSeconds: Float
): F[Pedal[F]] =
  for {
    outputCopyChannel <- ChunkedChannel.unbounded[F, Float]
    inputCopyChannel <- ChunkedChannel.unbounded[F, Float]
  } yield { stream =>
    (
      stream.through(inputCopyChannel.observePublishChunks).mapChunks(_ * -repeatGain) |+|
        (silence(delayTimeInSeconds) ++ inputCopyChannel.stream) |+|
        (silence(delayTimeInSeconds) ++ outputCopyChannel.stream.mapChunks(_ * repeatGain))
    ).through(outputCopyChannel.observePublishChunks)
  }
