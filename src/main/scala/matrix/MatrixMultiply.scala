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

  // 组合逻辑计算
  val result = Reg(Vec(dim, Vec(dim, SInt((2*dataWidth).W))))
  for (i <- 0 until dim; j <- 0 until dim) {
    result(i)(j) := (0 until dim).map(k => io.inA(i)(k) * io.inB(k)(j)).reduce(_ +& _)
  }

  // 控制信号
  val done = RegNext(io.start, false.B)
  io.out := result
  io.done := done
}