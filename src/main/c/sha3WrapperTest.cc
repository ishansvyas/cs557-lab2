// written by Ishan Vyas

#include <iostream>
#include <cstdint>
#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>

using namespace beethoven;

/**************************************
* FUNCTIONS TO MOVE DATA TO/FROM FPGA
***************************************/

void dma_workaround_copy_to_fpga(remote_ptr &q) {
    int sz = q.getLen() / 4;
    auto * intar = (int*)q.getHostAddr();
    for (int i = 0; i < sz; ++i) {
        auto ptr = q + i * 4;
        DMAHelper::memcmd(0, q + i * 4, intar[i], 1).get();
    }
}
void dma_workaround_copy_from_fpga(remote_ptr &q) {
    int sz = q.getLen() / 4;
    auto * intar = (int*)q.getHostAddr();
    for (int i = 0; i < sz; ++i) {
        auto ptr = q + i * 4;
        auto resp = DMAHelper::memcmd(0, q + i * 4, 0, 0).get();
        intar[i] = resp.payload;
    }
}

int main() {
    // connection to the FPGA management runtime, needed in the TB.
    fpga_handle_t handle;

    // create array of 0s
    remote_ptr in_alloc = handle.malloc(sizeof(uint64_t) * 24);
    uint64_t* host_alloc = (uint64_t*)in_alloc.getHostAddr();
    std::fill(host_alloc, host_alloc + 24, 0);

    // create result destination in memory
    remote_ptr res = handle.malloc(sizeof(uint64_t) * 4);
    uint64_t * host_alloc_rest = (uint64_t*)res.getHostAddr();

    // move it over to the FPGA (IMPORTANT)
    dma_workaround_copy_to_fpga(in_alloc);
    dma_workaround_copy_to_fpga(res);

    // call accelerator
    sha3Wrapper::sha3(
        0, // core ID
        in_alloc, // memory allocation for input state
        res       // memory of output
    ).get();

    // values to expect
    uint64_t a = 0xF1258F7940E1DDE7;
    uint64_t b = 0x84D5CCF933C0478A;
    uint64_t c = 0xD598261EA65AA9EE;
    uint64_t d = 0xBD1547306F80494D;
    uint64_t expect[4] = {a, b, c, d};

    dma_workaround_copy_from_fpga(in_alloc);
    dma_workaround_copy_from_fpga(res);

    for (int i = 0; i < 4; i++) {
        if (*host_alloc_rest != expect[i]) {
            printf("FAIL hash idx[%d] is %llu, expected %llu\n", i, *host_alloc_rest, expect[i]);
            printf("By the way, res is %llu\n", *res);
        }
        host_alloc_rest++;
    }
    printf("If no fails, then success!\n");
    return 0;
}