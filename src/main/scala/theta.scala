package sha3

import chisel3._
import sha3.common._

class ThetaModule(val W: Int = 64) extends Module {
  val io = IO(new Bundle {
    val state_i = Input (Vec(5*5, Bits(W.W)))
    val state_o = Output(Vec(5*5, Bits(W.W)))
  })
  //YOUR IMPLEMENTATION HERE
  val bc = Wire(Vec(5, Bits(W.W)))
  val list = List.range(0, 5)

  // The following is the C code for the first part of theta:
  // (1) for (i = 0; i < 5; i++)
  // (2)     bc[i] = st[i] ^ st[i + 5] ^ st[i + 10] ^ st[i + 15] ^ st[i + 20];
  for (i <- 0 until 5) {
    bc(i) := io.state_i(i) ^ io.state_i(i + 5) ^ io.state_i(i + 10) ^ io.state_i(i + 15) ^ io.state_i(i + 20)
  }

  // The following is the C code for the second part of theta:
  // (3) for (i = 0; i < 5; i++)
  // (4)   {
  // (5)     t = bc[(i + 4) % 5] ^ ROTL64(bc[(i + 1) % 5], 1);
  // (6)     for (j = 0; j < 25; j += 5)
  // (7)       st[j + i] ^= t;
  list zip bc map { //bc.zipWithIndex() instead
    case(i, bc_i) => {
      val t = bc((i + 4) % 5) ^ ROTL(bc((i + 1) % 5), 1, W)
      for (j <- 0 until 5) {
        io.state_o(5*j + i) := io.state_i(5*j + i) ^ t
      }
      bc_i // returns bc_i, though line is irrelevant
    }
  }
}

// PART 1
/*
  val s1 = io.state_i.slice(0,4)
  val s2 = io.state_i.slice(5,9)
  val s3 = io.state_i.slice(9,14)
  val s4 = io.state_i.slice(15,19)
  val s5 = io.state_i.slice(20,24)

  list zip s1 zip s2 zip s3 zip s4 zip s5 map {
    case(((((index, a1), a2), a3),a4),a5) => {
      bc(index) := a1 ^ a2 ^ a3 ^ a4 ^ a5
      (s1, s2, s3, s4, s5) // returns 5-tuple, tho line is irrelevant
    }
  } */

// PART 2
/*
val t = Wire(Bits(W.W))
  for (i <- 0 until 5) {
    t := bc((i + 4) % 5) ^ ROTL(bc((i + 1) % 5), 1, W)
    for (j <- 0 until 5) {
      io.state_o((5*j) + i) := io.state_i((5*j) + i) ^ t
    }
  }

 */
