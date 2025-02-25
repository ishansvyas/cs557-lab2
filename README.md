# Cs557 Lab2

These installations rely on you working on Mac or Linux. I'm unable to promise Windows support at this time.
## Beethoven Software Library

Install the beethoven software library
```bash
git clone https://github.com/Composer-Team/Beethoven-Software
mkdir -p Beethoven-Software/build
cd Beethoven-Software/build
cmake .. -DCMAKE_BUILD_TYPE=Release -DPLATFORM=discrete
make -j
sudo make install
```

## Beethoven Runtime/Simulator

Get the simulator
```bash
git clone https://github.com/Composer-Team/Beethoven-Runtime
```

We're using Icarus Verilog. You can install locally with brew, apt, usually, or it is also installed on the class server.
```bash
# Linux
sudo apt-get install iverilog
# Mac
brew install icarus-verilog
```

Set up your environment. The Hardware build system needs to know _where_ to place your hardware. You
specify this by setting an environment in your shell's rc setup (e.g., `~/.bashrc` for BASH, `~/.zshrc` for ZSH).
```bash
# This sets the Beethoven build path to `$HOME/Code/Beethoven` but you can put it wherever you like.
# If you're on mac/using ZSH, use ~/.zshrc instead
echo "export BEETHOVEN_PATH=$HOME/Code/Beethoven" >> ~/.bashrc
```

Run the simulator. Once you run `make sim_icarus` it will look for your built hardware
```bash
cd Beethoven-Runtime
make sim_icarus
```

Once you're running the simulator, run your testbench (e.g., main.cc in /src/main/c/ as provided).
```bash
cd src/main/c
mkdir build
cd build
# The following line depends on you having installed the Beethoven-Software repo (above)
cmake ..
make -j
./vec_add_test
```

Once the test has run, you can type `CTRL+C` in the simulator terminal window to stop the simulation.
That will put you into the shell for the simulator. Type "finish" to finish the simulation and flush
the waveform. You'll now find the waveform for your simulation in `dump.vcd`.

When you view the waveform, you'll see the hierarchy for the entire accelerator, infrastructure and
all. You can find your accelerators by navigating in the hierarchy:
```
BeethovenTopVCSHarness => top => beethovenDeviceX => sdX_sys<Your-Accelerator-System> => system<Your-Accelerator-System>_coreY_impl 
```
In the system hierarchies you'll see things like `readerModule_yada_yada_yada` with names specifying which system, core,
reader name, and channel index it corresponds to. As you write your accelerator you'll become more familiar with how
this is structured.

If you want to view the waveform for the IO you declared in your core, there is an `io_...` module inside your core
implementation hierarchy. With these more complex modules and designs, you'll find that Chisel generates quite a few
signals that make navigating challenging. In the search box, you can search for strings inside the signal you're looking
for.

For instance, if I have a state register, I can search "state". If I'm looking for all of the "io_*" signals,
though, I can't just search "io" because this will return all signals with the characters "io" in it
(e.g., "not_my_signalio" would be returned if it existed). However, if I search "^io", this stipulates that "io" must
come at the beginning of the screen.
