package sha3

import chisel3._
import sha3.common._

class RhoPiModule(val W: Int = 64) extends Module {
  val io = IO(new Bundle {
    val state_i = Input (Vec(5*5, Bits(W.W)))
    val state_o = Output(Vec(5*5, Bits(W.W)))
  })
  // constants
  val rotc = List(
    1,  3,  6,  10, 15, 21, 28, 36, 45, 55, 2,  14,
    27, 41, 56, 8,  25, 43, 62, 18, 39, 61, 20, 44)
  val piln = List(
    10, 7,  11, 17, 18, 3, 5,  16, 8,  21, 24, 4,
    15, 23, 19, 13, 12, 2, 20, 14, 22, 9,  6,  1)

  // Try zip, map, and case in Scala REPL if you are not familiar with them
  piln zip (1 :: piln) zip rotc map {
    case ((idx_o, idx_i), rotc_i) => {
      io.state_o(idx_o) := ROTL(io.state_i(idx_i), rotc_i, W)
    }
  }
  
  io.state_o(0) := io.state_i(0)
}
