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
    val hash_addr = Address()
  },
    EmptyAccelResponse())

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

  /*
  Notes: 1a. dataChannel comes from FPGA to Host
         1b. requestChannel goes from Host to FPGA
         if 1b is true; is vec_out initialized correctly?
   */

    // input channel initialization
  vec_in.requestChannel.valid := io.req.valid
  vec_in.requestChannel.bits.addr := io.req.bits.msg_addr
  vec_in.requestChannel.bits.len := c.U
  vec_in.dataChannel.data.ready := false.B

    // output channel initialization
  vec_out.requestChannel.valid := io.req.valid
  vec_out.requestChannel.bits.addr := io.req.bits.hash_addr
  vec_out.requestChannel.bits.len := (hash_size_words * W).U// hash always 256
  vec_out.dataChannel.data.bits := DontCare
  vec_out.dataChannel.data.valid := false.B


  /* --------------------------------
  * HERE WE ADD THE SHA3 MODULE W/ IO (below line is applicable - don't ignore)
  * ------------------------------- */

  vec_out.dataChannel.data.valid := vec_in.dataChannel.data.valid && activeCmd
  vec_in.dataChannel.data.ready := vec_out.dataChannel.data.ready

  // actual output data
  val sha3_module = Module(new Sha3Accel(W))
  sha3_module.io.message.bits <> vec_in.dataChannel.data.bits
  sha3_module.io.message.valid <> vec_in.dataChannel.data.valid
  sha3_module.io.message.ready <> something

  sha3_module.io.hash.bits <> vec_out.dataChannel.data.bits
  sha3_module.io.hash.valid <> something
  sha3_module.io.hash.ready <> something


  val all_channels_are_idle = vec_in.requestChannel.ready && vec_out.requestChannel.ready
  io.req.ready := !activeCmd && all_channels_are_idle
  io.resp.valid := activeCmd && all_channels_are_idle

  // active command logic pt. 2
  when(io.resp.fire) {
    activeCmd := false.B
  }

  // IGNORE FOR NOW
  // instantiation definitely not correct


}