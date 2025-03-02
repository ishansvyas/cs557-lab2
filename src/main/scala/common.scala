package sha3

import chisel3._
import chisel3.util._

object common {
  def ROTL(x: UInt, shift: Int, w: Int): UInt = {
    Cat( x(w - 1 - shift, 0), x(w - 1, w - 1 - (shift - 1)) )
  }
}
