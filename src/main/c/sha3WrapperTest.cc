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

    remote_ptr arr = handle.malloc(sizeof() * 24);
    long long arr[24];
    std::fill(arr, arr + 24, 0);

    uint64_t a = 0xF1258F7940E1DDE7;
    uint64_t b = 0x84D5CCF933C0478A;
    uint64_t c = 0xD598261EA65AA9EE;
    uint64_t d = 0xBD1547306F80494D;


/*BELOW IS NOT APPROVED*/
    int n_eles = 1024;
    // use the handle to allocate memory in the host/fpga simultaneously
    remote_ptr in_alloc = handle.malloc(sizeof(int) * n_eles);
    // put data in the host CPU side of the allocation
    int * host_alloc = (int*)in_alloc.getHostAddr();
    for (int i = 0; i < n_eles; i++) {
        host_alloc[i] = i;
    }
    // move it over to the FPGA (IMPORTANT)
    dma_workaround_copy_to_fpga(in_alloc);

    // call your accelerator. If you used the Address() interface in your BeethovenIO, you pass in a `remote_ptr` for
    // that. `remote_ptr` is the structure we use for allocating in the host/fpga.
    MyVectorAdd::vector_add(0, // CORE ID (always needed, but only non-zero if you have more than one core)
        15, // addend
        n_eles, // n_elements
        in_alloc // memory allocation
    ).get();

    // copy back from the FPGA (IMPORTANT)
    dma_workaround_copy_from_fpga(in_alloc);

    // check for correctness
    for (int i = 0; i < n_eles; i++) {
        if (host_alloc[i] != i + 15) {
            printf("FAIL idx[%d] is %d, expect %d\n", i, host_alloc[i], i + 15);
        }
    }
    printf("If no fails, then success!\n");
    return 0;
}