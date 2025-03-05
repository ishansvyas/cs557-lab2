import beethoven.Platforms.FPGA.Xilinx.F2._
import beethoven._

class sha3WrapperConfig(W: Int) extends AcceleratorConfig(List(
  AcceleratorSystemConfig(
    nCores = 1,
    name = "sha3Wrapper",
    moduleConstructor = ModuleBuilder(p => new sha3Wrapper(W)(p)),

    //  dataBytes
    memoryChannelConfig = List(
      // ReadChannelConfig dataByes must be 2^n; assume W=64 so 25*64 < 2^11
      ReadChannelConfig("vec_in", dataBytes = 2048),
      WriteChannelConfig("vec_out", dataBytes = 256 /* hash is always 256 */))
  ),
  new DMAHelperConfig, new MemsetHelperConfig(4)))

object sha3WrapperConfig extends BeethovenBuild(new sha3WrapperConfig(W = 64),
  buildMode = BuildMode.Simulation,
  platform = new AWSF2Platform)


