package matrix

import chisel3._
import chisel3.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._

class MatrixRoCC(opcodes: OpcodeSet, val dim: Int, val dataWidth: Int)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new LazyRoCCModuleImp(this) {
    val mm = Module(new MatrixMultiply(dim, dataWidth))
    
    // 控制状态
    val busy = RegInit(false.B)
    val loadCounter = RegInit(0.U(log2Ceil(dim*dim+1).W))
    
    // 双缓冲存储（A行存储，B列存储）
    val bufferA = Reg(Vec(dim*dim, SInt(dataWidth.W)))
    val bufferB = Reg(Vec(dim*dim, SInt(dataWidth.W)))
    val result = Reg(Vec(dim*dim, SInt((2*dataWidth).W)))

    // 默认连接
    mm.io.start := false.B
    mm.io.inA := DontCare
    mm.io.inB := DontCare

    // 指令解码
    val cmd = io.cmd
    val funct = cmd.bits.inst.funct
    val isLoadA = cmd.fire() && (funct === 0.U)
    val isLoadB = cmd.fire() && (funct === 1.U)
    val isCompute = cmd.fire() && (funct === 2.U)
    val isGet = cmd.fire() && (funct === 3.U)

    // 矩阵A加载（行优先）
    when(isLoadA) {
      val data = cmd.bits.rs1.asTypeOf(UInt(64.W))
      val idx = loadCounter << 1 // 每次加载两个元素
      bufferA(idx) := data(31, 0).asSInt
      bufferA(idx + 1.U) := data(63, 32).asSInt
      loadCounter := Mux(loadCounter === (dim*dim/2 - 1).U, 0.U, loadCounter + 1.U)
    }

    // 矩阵B加载（列优先，关键修复）
    when(isLoadB) {
      val data = cmd.bits.rs1.asTypeOf(UInt(64.W))
      val col = loadCounter / (dim/2).U  // 每列需要dim/2次加载
      val row = (loadCounter % (dim/2).U) << 1 // 每次加载两行
      
      // 列优先存储公式：address = col * dim + row
      bufferB(col*dim.U + row) := data(31, 0).asSInt
      bufferB(col*dim.U + row + 1.U) := data(63, 32).asSInt
      loadCounter := Mux(loadCounter === (dim*dim/2 - 1).U, 0.U, loadCounter + 1.U)
    }

    // 计算控制（修正矩阵B访问）
    when(isCompute) {
      busy := true.B
      mm.io.start := true.B
      // 行优先读取矩阵A
      mm.io.inA := VecInit.tabulate(dim)(i => 
        VecInit.tabulate(dim)(j => bufferA(i*dim + j))
      )
      // 列优先读取矩阵B（直接访问无需转置）
      mm.io.inB := VecInit.tabulate(dim)(i => 
        VecInit.tabulate(dim)(j => bufferB(i + j*dim)) // B[j][i]
      )
    }

    // 结果存储
    when(mm.io.done) {
      result := VecInit.tabulate(dim*dim)(i => mm.io.out(i/dim)(i%dim))
      busy := false.B
      loadCounter := 0.U
    }

    // 结果返回
    io.resp.valid := isGet || (busy && loadCounter =/= 0.U)
    io.resp.bits.rd := cmd.bits.inst.rd
    io.resp.bits.data := result(loadCounter).asUInt()
    when(io.resp.fire()) {
      loadCounter := Mux(loadCounter === (dim*dim - 1).U, 0.U, loadCounter + 1.U)
    }

    // 控制信号
    io.cmd.ready := !busy
    io.busy := busy
  }
}