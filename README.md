## fs2-pedalboard

fs2-pedalboard is a small audio processing app that was created as an idea for a talk. It's currently nowhere near as polished as it should be...

It currently provides implementations of tremolo, overdrive, delay and reverb effects.

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).

### Notes
The reverb algorithm in this project is based on the [JCRev Schroeder reverberator](https://ccrma.stanford.edu/~jos/Reverb/A_Schroeder_Reverberator_called.html) (signposted by [this blog post](https://medium.com/the-seekers-project/coding-a-basic-reverb-algorithm-part-2-an-introduction-to-audio-programming-4db79dd4e325) - early implementations of decoding the input stream from the java sound api were also adapted from this post, although explicit decoding is no longer necessary due to switching to portaudio and scala native).
