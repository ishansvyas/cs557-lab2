package sha3

import chisel3._
import chisel3.util.Enum

class CtrlModule(val W: Int) extends Module {
  //val W = 64
  val r = 2*256
  val c = 25*W - r
  val round_size_words = c/W
  val rounds = 24 //12 + 2l
  val hash_size_words = 256/W
  val bytes_per_word = W/8

  val io = IO(new Bundle {
    val message_val = Input(Bool())
    val message_rdy = Output(Bool())
    val hash_val    = Output(Bool())
    val hash_rdy    = Input(Bool())
    //YOU MAY ADD MORE PORTS
    val is_compute  = Output(Bool()) // is it in compute stage?
    val round       = Output(UInt(5.W))
  })
  //YOUR IMPLEMENTATION HERE

  // initialization
  io.message_rdy := false.B
  io.hash_val := false.B
  io.is_compute := false.B

  // fsm
  val sIdle :: sHash :: sDone :: Nil = Enum(3)
  val state = RegInit(sIdle)
  val i_round = RegInit(0.U(5.W))

  when(state === sIdle) {
    // wait for message valid, send to next step
    io.message_rdy := true.B
    io.hash_val := false.B
    io.is_compute := false.B

    when(io.message_val) {
      state := sHash
      i_round := 0.U
      io.is_compute := true.B // lower = higher priority, right?
      i_round := i_round + 1.U
    }
  }
  when(state === sHash) {
    io.is_compute := true.B
    i_round := i_round + 1.U
    when (i_round + 1.U >= rounds.U) {
      // case when done with all rounds
      state := sDone
    }
  }
  when(state === sDone) {
    io.is_compute := false.B
    // wait until hash valid, send to first step (and complete)
    when (io.hash_rdy) {
      io.hash_val := true.B
      io.message_rdy := true.B
      state := sIdle
    }
  }
  io.round := i_round
}
