package sha3

import chisel3._
import chisel3.util._

class Sha3Accel(val W: Int) extends Module {
  // constants
  val r = 2 * 256
  val c = 25 * W - r
  val round_size_words = c / W
  val rounds = 24 // 12 + 2l
  val hash_size_words = 256 / W
  val bytes_per_word = W / 8

  val io = IO(new Bundle {
    val message = Flipped(new DecoupledIO(Vec(round_size_words, Bits(W.W))))
    val hash = new DecoupledIO(Vec(hash_size_words, Bits(W.W)))
  })

  // ctrl circuit
  val ctrl = Module(new CtrlModule(W))

  ctrl.io.message_val <> io.message.valid
  ctrl.io.message_rdy <> io.message.ready

  ctrl.io.hash_val <> io.hash.valid
  ctrl.io.hash_rdy <> io.hash.ready

  // extra vars
  val paddedMessage = RegInit(VecInit(Seq.fill(25)(0.U(W.W))))
  val initialized = RegInit(false.B)

  when(io.message.valid && !initialized) {
    paddedMessage := VecInit(io.message.bits ++ Seq.fill(25 - round_size_words)(0.U(W.W)))
    initialized := true.B
  }

  // dpath circuit
  val dpath = Module(new DpathModule(W))
  dpath.io.round := ctrl.io.round

  when (ctrl.io.is_compute) {
    dpath.io.state_in := paddedMessage
    paddedMessage := dpath.io.state_out
    // is the above allowed?
  } .otherwise {
    val extra = Wire(Vec(25, Bits(W.W)))
    extra := VecInit(Seq.fill(25)(0.U(W.W)))
    // don't put state thru datapath if not in compute state
    dpath.io.state_in := extra
  }
  io.hash.bits := paddedMessage.slice(0, hash_size_words)
}

// object Sha3AccelMain extends App {
//   Driver.execute(args, () => new Sha3Accel(64))
// }
