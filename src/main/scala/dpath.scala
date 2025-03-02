package sha3

import chisel3._

class DpathModule(val W: Int) extends Module {
  //constants
  val r = 2*256
  val c = 25*W - r
  val round_size_words = c/W
  val rounds = 24 //12 + 2l
  val hash_size_words = 256/W
  val bytes_per_word = W/8

  val io = IO(new Bundle { 
    //YOU MAY ADD MORE PORTS
    val state_in = Input(Vec(5 * 5, Bits(W.W)))
    val state_out = Output(Vec(5 * 5, Bits(W.W)))
    val round = Input(UInt(5.W))
  })
  //YOUR IMPLEMENTATION HERE

  // are the constants needed for the dpath?
  // ^ i don't think so?

  val med1 = Wire(Vec(5*5, Bits(W.W)))
  val med2 = Wire(Vec(5*5, Bits(W.W)))
  val med3 = Wire(Vec(5*5, Bits(W.W)))

  // Theta
  val theta = Module(new ThetaModule(W))
  theta.io.state_i := io.state_in
  med1 := theta.io.state_o

  // RhoPi
  val rhopi = Module(new RhoPiModule(W))
  rhopi.io.state_i := med1
  med2 := rhopi.io.state_o

  // Chi
  val chi = Module(new ChiModule(W))
  chi.io.state_i := med2
  med3 := chi.io.state_o

  // Iota
  val iota = Module(new IotaModule(W))
  iota.io.state_i := med3
  iota.io.round := io.round // indexing is correct;
  io.state_out := iota.io.state_o
}
