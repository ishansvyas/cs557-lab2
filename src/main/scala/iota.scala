package sha3

import chisel3._

class IotaModule(val W: Int = 64) extends Module {
  val io = IO(new Bundle {
    val state_i = Input(Vec(5 * 5, Bits(W.W)))
    val state_o = Output(Vec(5 * 5, Bits(W.W)))
    val round = Input(UInt(5.W))
  })

  for (i <- 1 to 24) {
    io.state_o(i) := io.state_i(i)
  }

  val round_const = VecInit(
    "h0000000000000001".asUInt(64.W),
    "h0000000000008082".asUInt(64.W),
    "h800000000000808a".asUInt(64.W),
    "h8000000080008000".asUInt(64.W),
    "h000000000000808b".asUInt(64.W),
    "h0000000080000001".asUInt(64.W),
    "h8000000080008081".asUInt(64.W),
    "h8000000000008009".asUInt(64.W),
    "h000000000000008a".asUInt(64.W),
    "h0000000000000088".asUInt(64.W),
    "h0000000080008009".asUInt(64.W),
    "h000000008000000a".asUInt(64.W),
    "h000000008000808b".asUInt(64.W),
    "h800000000000008b".asUInt(64.W),
    "h8000000000008089".asUInt(64.W),
    "h8000000000008003".asUInt(64.W),
    "h8000000000008002".asUInt(64.W),
    "h8000000000000080".asUInt(64.W),
    "h000000000000800a".asUInt(64.W),
    "h800000008000000a".asUInt(64.W),
    "h8000000080008081".asUInt(64.W),
    "h8000000000008080".asUInt(64.W),
    "h0000000080000001".asUInt(64.W),
    "h8000000080008008".asUInt(64.W)
  )
  io.state_o(0) := io.state_i(0) ^ round_const(io.round)
}
