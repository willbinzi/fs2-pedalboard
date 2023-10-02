package pedals

import cats.effect.Concurrent
import constants.{ CHUNKS_PER_SECOND, FLOAT_BUFFER_SIZE }
import fs2.concurrent.Channel
import fs2.{ Chunk, Stream }
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.semigroup.*

def delay[F[_]](delayTimeInSeconds: Float, feedback: Float): Pedal[F] =
  val delayTimeInChunks = (delayTimeInSeconds * CHUNKS_PER_SECOND).toInt
  val delayBuffer = Array.fill[Chunk[Float]](delayTimeInChunks)(Chunk.array(Array.fill(FLOAT_BUFFER_SIZE)(0f)))
  _.scanChunks(0) { (n, chunk) =>
    val i = n % delayTimeInChunks
    val wetChunk = chunk.zipWith(delayBuffer(i))( _ + _ * feedback)
    delayBuffer(i) = wetChunk
    ((n + 1) % delayTimeInChunks, wetChunk)
  }

def silence[F[_]](timeInSeconds: Float): Stream[F, Chunk[Float]] =
  val delayTimeInChunks = (timeInSeconds * CHUNKS_PER_SECOND).toInt
  val silenceChunkArray = Array.fill(FLOAT_BUFFER_SIZE)(0f)
  val silenceChunk = Chunk.array(silenceChunkArray)
  Stream.emit(silenceChunk).repeatN(delayTimeInChunks)

def delayF[F[_]: Concurrent](repeatGain: Float, delayTimeInSeconds: Float): F[Pedal[F]] =
  Channel.unbounded[F, Chunk[Float]].map( repeatsChannel =>
    stream =>
      (
        stream.chunks |+|
          (silence(delayTimeInSeconds) ++ repeatsChannel.stream.map(_ * repeatGain))
      ).evalTap(repeatsChannel.send)
      .unchunks
  )

def allPassFilterF[F[_]: Concurrent](repeatGain: Float, delayTimeInSeconds: Float): F[Pedal[F]] =
  for {
    outputCopyChannel <- Channel.unbounded[F, Chunk[Float]]
    inputCopyChannel <- Channel.unbounded[F, Chunk[Float]]
  } yield {
    stream =>
      (stream.chunks
        .evalTap(inputCopyChannel.send)
        .map(_ * (-repeatGain)) |+|
          (silence(delayTimeInSeconds) ++ inputCopyChannel.stream) |+|
          (silence(delayTimeInSeconds) ++ outputCopyChannel.stream.map(_ * repeatGain))
      ).evalTap(outputCopyChannel.send)
      .unchunks
  }
