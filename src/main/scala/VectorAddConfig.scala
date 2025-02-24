import beethoven.Platforms.FPGA.Xilinx.F2._
import beethoven._

class VectorAddConfig extends AcceleratorConfig(List(
  AcceleratorSystemConfig(
    // if you want to duplicate the functionality and have a multi-core design
    // you can crank up this parameter
    nCores = 1,
    // you have to give your accelerator system a name. The software will be able
    // to call your accelerator from your C++ testbench using <System_name>::<Command_name>(...);
    name = "MyVectorAdd",
    // an unfortunately complicated boilerplate.
    moduleConstructor = ModuleBuilder(p => new VectorAdd()(p)),
    // declare all of your memory interfaces here!
    memoryChannelConfig = List(
      ReadChannelConfig("vec_in", dataBytes = 4),
      WriteChannelConfig("vec_out", dataBytes = 4))
  ),
  // AWS has recently moved from the F1 instances to F2 instances, incidentally removing
  // some infrastructure we relied on in previous versions of the course. We believe this
  // functionality should return soon but we have a workaround for initializing FPGA DRAM
  // in the meantime.
  new DMAHelperConfig, new MemsetHelperConfig(4)))

// Beethoven uses separate BuildModes to enable/disable the generation of certain files
// than can disrupt the hardware compile/simulation process. Whenever you want to run
// simulation/synthesis, ensure that the right option is enabled.
object VectorAddConfig extends BeethovenBuild(new VectorAddConfig,
  buildMode = BuildMode.Simulation, // BuildMode.Simulation
  // We currently support the F1, F2 instances, a local "Kria" platform (the KV260 FPGA)
  // and are developing support for an ASIC tapeout platform. For your purposes, you
  // should leave this as the AWSF2Platform.
  platform = new AWSF2Platform)

// For building the AWS F2 FPGA image. Enable the synthesis build, and run Chisel build.
// You should do this locally either using an IDE, or you can run `run` from the sbt
// shell. When it asks you for the EC2 IP address, enter the address for the compile
// instance you'd started on AWS. Make sure you've logged into the instance in your shell
// so that you can "fingerprint" it before you do the hardware build. Beethoven will
// use the IP address to move over and set up all of the necessary build files.

// From there, you can follow the FPGA image build process in the lab document.