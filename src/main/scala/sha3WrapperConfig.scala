import beethoven.Platforms.FPGA.Xilinx.F2._
import beethoven._

class sha3WrapperConfig extends AcceleratorConfig(List(
  AcceleratorSystemConfig(
    nCores = 1,
    name = "sha3Wrapper",
    moduleConstructor = ModuleBuilder(p => new sha3Wrapper()(p)),

    // double check
    memoryChannelConfig = List(
      ReadChannelConfig("vec_in", dataBytes = 4),
      WriteChannelConfig("vec_out", dataBytes = 4))
  ),
  new DMAHelperConfig, new MemsetHelperConfig(4)))

object sha3WrapperConfig extends BeethovenBuild(new sha3WrapperConfig,
  buildMode = BuildMode.Simulation,
  platform = new AWSF2Platform)


