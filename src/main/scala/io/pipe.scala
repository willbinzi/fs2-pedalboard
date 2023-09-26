package io

import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.SourceDataLine
import util.unpack
import util.pack

val bufferSize: Int = 4096

// Transfers data from an input stream to a data line
def pipe(in: AudioInputStream, out: SourceDataLine): Unit =
  val bufferBytes: Array[Byte] = new Array[Byte](bufferSize)
  val bufferFloats: Array[Float] = new Array[Float](bufferSize / 2)
  var readBytes: Int = -1
  while true do
    readBytes = in.read(bufferBytes, 0, bufferSize)
    unpack(bufferBytes, bufferFloats, readBytes)
    pack(bufferFloats, bufferBytes, readBytes / 2)
    out.write(bufferBytes, 0, readBytes)
    ()
