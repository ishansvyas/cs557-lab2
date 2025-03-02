package sha3

import chisel3._
import sha3.common._

class ChiModule(val W: Int = 64) extends Module {
  val io = IO(new Bundle {
    val state_i = Input(Vec(5 * 5, Bits(W.W)))
    val state_o = Output(Vec(5 * 5, Bits(W.W)))
  })
  //YOUR IMPLEMENTATION HERE
  (0 until 5).zipWithIndex.map { case (_, j_0) => {
    val j = j_0 * 5
    val bc = Wire(Vec(5, Bits(W.W)))
    (0 until 5).zipWithIndex.map { case (_, i) =>
      bc(i) := io.state_i(i + j)
      0.U
    }
    (0 until 5).zipWithIndex.map { case (_, i) =>
      io.state_o(i + j) := io.state_i(i + j) ^ (~bc((i + 1) % 5) & bc((i + 2) % 5))
      0.U
    }
  }
  }

}

/* BELOW WORKS:
for (j_0 <- 0 until 5) {
  val j = j_0 * 5
  val bc = Wire(Vec(5, Bits(W.W)))
  for (i <- 0 until 5) {
    bc(i) := io.state_i(i + j)
  }
  for (i <- 0 until 5) {
    io.state_o(i + j) := io.state_i(i+j) ^ (~bc((i+1) % 5) & bc((i+2) % 5))
    // ignore error above - compiles fine
  }
}
 */