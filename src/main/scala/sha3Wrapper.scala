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
    // i/o
    val msg_addr = Address()
    val hash_addr = Address() // returns an address

  }, EmptyAccelResponse())
  // active command logic pt. 1
  val activeCmd = RegInit(false.B)
  when (io.req.fire) {
    activeCmd := true.B
  }
  // pairs with the AcceleratorSystemConfig in sha3WrapperConfig
  // create and initialize Reader and Writer modules
  val vec_in = getReaderModule("vec_in")
  val vec_out = getWriterModule("vec_out")
  vec_in.dataChannel.data.ready := false.B

    // vec_in initialization
//  vec_in.requestChannel.ready    GIVEN
  vec_in.requestChannel.valid := io.req.valid
  vec_in.requestChannel.bits.addr := io.req.bits.msg_addr
  vec_in.requestChannel.bits.len := common.Misc.multByIntPow2(round_size_words.U, 8) // 8 bc bigInt
  vec_in.dataChannel.data.ready := true.B // in VectorAdd, never touched.
//  vec_in.dataChannel.data.valid  GIVEN
//  vec_in.dataChannel.data.bits   GIVEN

    // vec_out initialization ---------
//  vec_out.requestChannel.ready   GIVEN
  vec_out.requestChannel.valid := io.resp.valid
  vec_out.requestChannel.bits.addr <> io.req.bits.hash_addr   // i think <> works but if not changed to :=
  vec_out.requestChannel.bits.len := 256.U
//  vec_out.dataChannel.data.ready  GIVEN
  vec_out.dataChannel.data.bits := DontCare
  vec_out.dataChannel.data.valid := true.B // in VectorAdd, never touched

    // io initialization
  io.req.ready := vec_in.requestChannel.ready && vec_out.requestChannel.ready && !activeCmd
//  io.req.valid  GIVEN
//  io.req.bits.msg_addr GIVEN
//  io.resp.ready GIVEN
  io.resp.valid := vec_in.requestChannel.ready && vec_out.requestChannel.ready && activeCmd

  // sha3_module initialization
  val sha3_module = Module(new Sha3Accel(W))
  sha3_module.io.message.bits := Vec(round_size_words, Bits(W.W))
  sha3_module.io.message.valid := false.B
//  sha3_module.io.message.ready GIVEN
//  sha3_module.io.hash.bits GIVEN
//  sha3_module.io.hash.valid GIVEN
  sha3_module.io.hash.ready := true.B

  // BELOW --------------------------------------------
  /*
  vec_out.dataChannel.data.valid := vec_in.dataChannel.data.valid && activeCmd
  vec_in.dataChannel.data.ready := vec_out.dataChannel.data.ready

  // actual output data

  sha3_module.io.message.valid := vec_in.dataChannel.data.valid
  vec_in.dataChannel.data.ready := sha3_module.io.message.ready

  // also try .asUInt
  vec_out.dataChannel.data.bits := sha3_module.io.hash.bits.asUInt
  vec_out.dataChannel.data.valid := sha3_module.io.hash.valid
  sha3_module.io.hash.ready := vec_out.dataChannel.data.ready

  val all_channels_are_idle = vec_in.requestChannel.ready && vec_out.requestChannel.ready
  io.req.ready := !activeCmd && all_channels_are_idle
  io.resp.valid := activeCmd && all_channels_are_idle
*/
  // ABOVE -----------------------------------------
  // active command logic pt. 2
  when(io.resp.fire) {
    activeCmd := false.B
  }

  // to do: state machine to hold the memory input.
    // assume sha3wrapper in core and NOT in rest.of.world.

  // reg file that holds 17 * 8 bytes.
  val inputRegFile = RegInit(VecInit(Seq.fill(17)(0.U(64.W))))
  val outputRegFile = RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
  val counter_in = RegInit(0.U(5.W))
  val counter_out = RegInit(0.U(5.W))
  val start_export = RegInit(false.B)

  // when valid/ready handshake, initiate counter to 17.
  when (vec_in.dataChannel.data.valid && counter_in < 17.U) {
      inputRegFile(counter_in) := vec_in.dataChannel.data.bits
      counter_in := counter_in + 1.U
  }
  // then, once data stored, pass to sha3.input
  when (counter_in === 17.U) {
    sha3_module.io.message.bits := inputRegFile
    sha3_module.io.message.valid := true.B
    counter_in := 0.U
  }

  // same idea for sha3 output
  // to do: state machine to hold the memory output
  when (sha3_module.io.hash.valid && vec_out.dataChannel.data.ready) {
    outputRegFile := sha3_module.io.hash.bits
    start_export := true.B
  }
  when (start_export && counter_out < 4.U) {
    vec_out.dataChannel.data.bits := outputRegFile(counter_out)
    vec_out.dataChannel.data.valid := true.B
    counter_out := counter_out + 1.U
  }


}