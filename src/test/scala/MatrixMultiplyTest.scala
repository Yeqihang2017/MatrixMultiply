import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MatrixMultiplyTest extends AnyFlatSpec with ChiselScalatestTester {
  "MatrixMultiply" should "correctly multiply two 4x4 matrices" in {
    test(new MatrixMultiply) { c =>
      // 定义输入矩阵 A 和 B
      val a = Array(
        Array(1, 2, 3, 4),
        Array(5, 6, 7, 8),
        Array(9, 10, 11, 12),
        Array(13, 14, 15, 16)
      )
      val b = Array(
        Array(17, 18, 19, 20),
        Array(21, 22, 23, 24),
        Array(25, 26, 27, 28),
        Array(29, 30, 31, 32)
      )

      // 将矩阵 A 和 B 输入到模块中
      for (i <- 0 until 4) {
        for (j <- 0 until 4) {
          c.io.a(i)(j).poke(a(i)(j).U)
          c.io.b(i)(j).poke(b(i)(j).U)
        }
      }

      // 计算期望的结果
      val expected = Array.ofDim[Int](4, 4)
      for (i <- 0 until 4) {
        for (j <- 0 until 4) {
          expected(i)(j) = 0
          for (k <- 0 until 4) {
            expected(i)(j) += a(i)(k) * b(k)(j)
          }
        }
      }

      // 验证输出矩阵 C
      for (i <- 0 until 4) {
        for (j <- 0 until 4) {
          c.io.c(i)(j).expect(expected(i)(j).U)
        }
      }
    }
  }
}