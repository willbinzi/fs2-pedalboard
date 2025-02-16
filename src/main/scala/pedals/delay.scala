package pedals

import cats.effect.Concurrent
import constants.{CHUNKS_PER_SECOND, FRAMES_PER_BUFFER}
import fs2.concurrent.Channel
import fs2.{Chunk, Stream}
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.semigroup.*
import pubsub.ChunkedChannel.*

def silenceChunks[F[_]](timeInSeconds: Float): Stream[F, Chunk[Float]] =
  val delayTimeInChunks = (timeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunkArray = Array.fill(FRAMES_PER_BUFFER)(0f)
  val silenceChunk = Chunk.array(silenceChunkArray)
  Stream.emit(silenceChunk).repeatN(delayTimeInChunks)

def silence[F[_]](timeInSeconds: Float): Stream[F, Float] =
  silenceChunks(timeInSeconds).unchunks

extension [F[_]](stream: Stream[F, Chunk[Float]])
  def delayed(timeInSeconds: Float): Stream[F, Chunk[Float]] =
    silenceChunks(timeInSeconds) ++ stream

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
    outputCopyChannel <- Channel.unbounded[F, Chunk[Float]]
    inputCopyChannel <- Channel.unbounded[F, Chunk[Float]]
  } yield { stream =>
    (
      stream.chunks.evalTap(inputCopyChannel.send).map(_ * (-repeatGain)) |+|
        inputCopyChannel.stream.delayed(delayTimeInSeconds) |+|
        outputCopyChannel.stream.map(_ * repeatGain).delayed(delayTimeInSeconds)
    ).evalTap(outputCopyChannel.send).unchunks
  }
