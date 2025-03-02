import beethoven._
import beethoven.common._
import chipsalliance.rocketchip.config._
import chisel3._
import chisel3.util._
import sha3.Sha3Accel

class sha3Wrapper(W: Int)(implicit p: Parameters) extends AcceleratorCore {
  // constants
  val r = 2 * 256
  val c = 25 * W - r
  val round_size_words = c / W
  val rounds = 24 // 12 + 2l
  val hash_size_words = 256 / W
  val bytes_per_word = W / 8

  val io = BeethovenIO(new AccelCommand("sha3") {
    // input
    val message = Flipped(new DecoupledIO(Vec(round_size_words, Bits(W.W))))

    // output?
    val hash = new DecoupledIO(Vec(hash_size_words, Bits(W.W)))
  },
    EmptyAccelResponse())

  val activeCmd = RegInit(false.B)
  when (io.req.fire) {
    activeCmd := true.B
  }


  // pairs with the AcceleratorSystemConfig in sha3WrapperConfig
  // create and initialize Reader and Writer modules
  val vec_in = getReaderModule("vec_in")
  val vec_out = getWriterModule("vec_out")
  vec_in.dataChannel.data.ready := false.B
  vec_in.requestChannel.valid := io.req.valid
  vec_in.requestChannel.bits := io.req.bits
//  vec_in.requestChannel.bits := ?????????????



  // instantiation definitely not correct
  val sha3mod = Module(new Sha3Accel(W)(/*how to put DeqIO in here*/))


}