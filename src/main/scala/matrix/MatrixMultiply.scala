package matrix

import chisel3._
import chisel3.util._

class MatrixMultiply(val dim: Int, val dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val inA = Input(Vec(dim, Vec(dim, SInt(dataWidth.W))))
    val inB = Input(Vec(dim, Vec(dim, SInt(dataWidth.W))))
    val out = Output(Vec(dim, Vec(dim, SInt((2 * dataWidth).W))))
    val start = Input(Bool())
    val done = Output(Bool())
  })

  val result = Reg(Vec(dim, Vec(dim, SInt((2 * dataWidth).W))))
  val busy = RegInit(false.B)

  when (io.start && !busy) {
    for (i <- 0 until dim) {
      for (j <- 0 until dim) {
        result(i)(j) := 0.S
        for (k <- 0 until dim) {
          result(i)(j) := result(i)(j) + io.inA(i)(k) * io.inB(k)(j)
        }
      }
    }
    busy := true.B
  }

  io.out := result
  io.done := busy
}