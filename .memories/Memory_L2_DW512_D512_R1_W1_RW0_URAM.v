
(* keep_hierarchy = "yes" *)
module Memory_L2_DW512_D512_R1_W1_RW0_URAM (
  input CE,
  input [0:0] WEB,
  input OEB,
  output [511:0] O,
  input [8:0] A_read,
  input [511:0] I,
  input [8:0] A_write,
  input CSB_read,
  input CSB_write);

(* ram_style = "ultra" *)
reg [511:0] mem [0:0][511:0];        // Memory Declaration
reg [511:0] memreg;
reg [511:0] mem_pipe_reg [1:0];    // Pipelines for memory

integer i, gi;
always @ (posedge CE) begin
  if(CSB_read) begin
    memreg <= mem[0][A_read];
  end

  if(CSB_write) begin
    
    if (WEB) begin
      mem[0][A_write] <= I;
    end

  end

  // RAM output data goes through a pipeline.
  mem_pipe_reg[0] <= memreg ;
  for (i = 0; i < 2-1; i = i+1) begin
    mem_pipe_reg[i+1] <= mem_pipe_reg[i];
  end

end

assign O = mem_pipe_reg[2-1];
endmodule
