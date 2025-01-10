package matrix

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec

class MatrixMultiplyTest extends FreeSpec with ChiselScalatestTester {
  "MatrixMultiply should calculate matrix multiplication correctly" in {
    test(new MatrixMultiply(4, 32)) { dut =>
      // 初始化输入矩阵
      val A = Array(
        Array(1, 2, 3, 4),
        Array(5, 6, 7, 8),
        Array(9, 10, 11, 12),
        Array(13, 14, 15, 16)
      )
      val B = Array(
        Array(1, 0, 0, 0),
        Array(0, 1, 0, 0),
        Array(0, 0, 1, 0),
        Array(0, 0, 0, 1)
      )

      // 将输入矩阵写入 DUT
      for (i <- 0 until 4) {
        for (j <- 0 until 4) {
          dut.io.inA(i)(j).poke(A(i)(j).S)
          dut.io.inB(i)(j).poke(B(i)(j).S)
        }
      }

      // 启动计算
      dut.io.start.poke(true.B)
      dut.clock.step(1)
      dut.io.start.poke(false.B)

      // 等待计算完成
      while (!dut.io.done.peek().litToBoolean) {
        dut.clock.step(1)
      }

      // 验证输出矩阵
      for (i <- 0 until 4) {
        for (j <- 0 until 4) {
          dut.io.out(i)(j).expect((A(i)(j) * B(i)(j)).S)
        }
      }
    }
  }
}