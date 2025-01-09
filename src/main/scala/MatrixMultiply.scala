import chisel3._
import chisel3.util._

class MatrixMultiply extends Module {
  val io = IO(new Bundle {
    val a = Input(Vec(4, Vec(4, UInt(8.W))))  // 4x4 矩阵 A
    val b = Input(Vec(4, Vec(4, UInt(8.W))))  // 4x4 矩阵 B
    val c = Output(Vec(4, Vec(4, UInt(8.W)))) // 4x4 矩阵 C (结果)
  })

  // 初始化输出矩阵 C
  for (i <- 0 until 4) {
    for (j <- 0 until 4) {
      io.c(i)(j) := 0.U
    }
  }

  // 矩阵乘法计算
  for (i <- 0 until 4) {
    for (j <- 0 until 4) {
      for (k <- 0 until 4) {
        io.c(i)(j) := io.c(i)(j) + io.a(i)(k) * io.b(k)(j)
      }
    }
  }
}

object MatrixMultiply extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new MatrixMultiply(), Array("--target-dir", "generated"))
}