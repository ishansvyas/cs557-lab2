import beethoven._
import beethoven.common._
import chipsalliance.rocketchip.config._
import chisel3._
import chisel3.util._

class VecAdd(implicit p: Parameters) extends AcceleratorCore {
  // declare a new software interface to this core called "vector_add"
  // if one piece of hardware/Accelerator core can perform more than one action, you can
  // expose these using different BeethovenIOs in the same module
  // For instance, you could have a ``vector_add`` interface as well as a ``cleanup`` interface
  // that triggers a reset of registers back to known states. You cannot trigger the reset signal
  // from your testbench. That is only used when starting up the FPGA.
  val io = BeethovenIO(new AccelCommand("vector_add") {
    // Inputs go inside the "AccelCommand" part of the BeethovenIO
    // Most input types should be well supported, but we suggest against large vectors of input
    // Large inputs should be DMA'd into FPGA memory and read into the accelerator using a reader
    // interface
    val addend = UInt(32.W)
    // different platforms may have different address widths. We provide an ``Address`` type that
    // grows/shrinks appropriately with the platform you are building your accelerator for.
    // Underneath, this is just a UInt and if you want to do arithmetic on it, it may be necessary
    // to cast it to a UInt with ``.asUInt()``
    val vec_addr = Address()
    val n_eles = UInt(20.W)
  },
    // This accelerator core does not return a payload, only an ACK.
    // If you want to return a payload(s) to the testbench when you're done executing, then
    // you can declare a `new AccelResponse("response_t") {
    //   <YOUR TYPES HERE>
    // }
    EmptyAccelResponse())

  // Below is the core implementation that we want to expose to the user
  val addendReg = Reg(UInt(32.W))
  val activeCmd = RegInit(false.B)
  when (io.req.fire) {
    addendReg := io.req.bits.addend
    activeCmd := true.B
  }
  // readers and writers are streams from/to memory that both come with a "request" channel
  // and a "data" channel. You should not attempt to load data to/from the data channel
  // before the request handshake.
  //
  // You might notice there is basically no specification/customization here for these channels!
  // The configuration for these channels happens in the config part of your design which is
  // separate from the accelerator core implementation.
  // The name here is associated with a reader that you declare and customize in the configuration.
  val ReaderModuleChannel(vec_in_request, vec_in_data) = getReaderModule("vec_in")
  val WriterModuleChannel(vec_out_request, vec_out_data) = getWriterModule("vec_out")
  // request fields have a ready/valid handshake as well as address and length (in bytes) payload
  // we have attempted to assert against the common pitfalls, but, generally speaking try and read
  // and write on well-aligned addresses.
  // For instance, if you have a read channel of with data-width=8 bytes, read from an address
  // starting on an 8-byte boundary.
  // Or if you have a data-width of 8 bytes, the length should be multiple of 8-bytes
  vec_in_request.valid := io.req.valid
  vec_in_request.bits.addr := io.req.bits.vec_addr
  // we provide some common idioms that chisel doesn't come with.
  // In Verilog you might want to do ```assign a = {my_signal, 4'b0};```
  // In Chisel, this can be written ``` a := Cat(my_signal, 0.U(4.W))
  // but we think that ```a := multByIntPow2(my_signal, 16)``` might be
  // a more readable way to do this. Just personal preference here.
  val write_len_bytes = common.Misc.multByIntPow2(io.req.bits.n_eles, 4)
  vec_in_request.bits.len := write_len_bytes
  vec_out_request.valid := io.req.valid
  vec_out_request.bits.addr := io.req.bits.vec_addr
  vec_out_request.bits.len := write_len_bytes
  // split vector into 32b chunks and add addend to it
  // The data-rate of your stream is limited by ``clock_rate * data_width``.
  // Therefore, you sometimes might want to make the data channel for readers/writers
  // wider than the individual datums you're working with.
  // Here, we split the data vector into chunks of 32 for this reason.
  // This way, even if we don't know the real length of the vector (the core takes
  // in no parameters), we can still split it up into 32b payloads.
  vec_out_data.data <> vec_in_data.data.map(bitVec =>
    applyToChunks(bitVec, 32, _ + addendReg))

  // make sure to drive the response bus if you're using it!
  io.req.ready := vec_in_request.ready && vec_out_request.ready && !activeCmd
  io.resp.valid := vec_in_request.ready && vec_out_request.ready && activeCmd
  when (io.resp.fire) {
    activeCmd := false.B
  }
}