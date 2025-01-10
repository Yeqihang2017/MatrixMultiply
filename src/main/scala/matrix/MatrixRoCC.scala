package matrix

import chisel3._
import chisel3.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._

class MatrixRoCC(implicit p: Parameters) extends LazyRoCC {
  val multiplier = Module(new MatrixMultiply(4, 32))

  override lazy val module = new LazyRoCCModuleImp(this) {
    // 连接 RoCC 接口
    io.cmd.ready := !multiplier.io.busy
    multiplier.io.start := io.cmd.fire()

    // 解析指令
    val rs1 = io.cmd.bits.rs1
    val rs2 = io.cmd.bits.rs2

    // 将输入数据传递给矩阵乘法模块
    for (i <- 0 until 4) {
      for (j <- 0 until 4) {
        multiplier.io.inA(i)(j) := io.cmd.bits.rs1(i * 4 + j).asSInt
        multiplier.io.inB(i)(j) := io.cmd.bits.rs2(i * 4 + j).asSInt
      }
    }

    // 将结果写回寄存器
    when (multiplier.io.done) {
      for (i <- 0 until 4) {
        for (j <- 0 until 4) {
          io.resp.bits.data(i * 4 + j) := multiplier.io.out(i)(j).asUInt
        }
      }
      io.resp.valid := true.B
    } otherwise {
      io.resp.valid := false.B
    }
  }
}