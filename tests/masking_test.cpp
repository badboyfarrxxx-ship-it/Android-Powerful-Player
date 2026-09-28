#include <iostream>
#include <vector>
#include <cassert>
#include <cstring>
#include <algorithm>

class XORMasker {
public:
    struct State {
        uint64_t seed;
        uint64_t counter;
    };

    static void mask(uint8_t* data, size_t len, State& state) {
        for (size_t i = 0; i < len; ++i) {
            data[i] ^= generate_byte(state);
        }
    }

    static void unmask(uint8_t* data, size_t len, State& state) {
        mask(data, len, state); // XOR is symmetric
    }

private:
    static uint8_t generate_byte(State& state) {
        // Simple LCG for pseudo-random stream based on seed and counter
        // In production, a more robust PRNG (like ChaCha) would be used
        uint64_t val = state.seed + state.counter;
        val ^= (val >> 33);
        val *= 0xff51afd7ed558ccdLLU;
        val ^= (val >> 33);
        val *= 0xc4ceb9fe1a85ec53LLU;
        val ^= (val >> 33);
        
        state.counter++;
        return static_cast<uint8_t>(val & 0xFF);
    }
};

void test_symmetry() {
    std::cout << "Testing Symmetry... ";
    uint8_t data[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0x01, 0x02, 0x03, 0x04 };
    size_t len = sizeof(data);
    uint8_t original[sizeof(data)];
    memcpy(original, data, len);

    XORMasker::State send_state = { 12345, 0 };
    XORMasker::State recv_state = { 12345, 0 };

    XORMasker::mask(data, len, send_state);
    
    // Ensure data was actually masked
    bool changed = false;
    for(size_t i=0; i<len; ++i) if(data[i] != original[i]) changed = true;
    assert(changed);

    XORMasker::unmask(data, len, recv_state);

    for(size_t i=0; i<len; ++i) {
        assert(data[i] == original[i]);
    }
    std::cout << "PASSED" << std::endl;
}

void test_seed_rotation() {
    std::cout << "Testing Seed Rotation... ";
    uint8_t data[] = { 0xAA, 0xBB, 0xCC, 0xDD };
    size_t len = sizeof(data);
    
    XORMasker::State state = { 999, 0 };
    
    // Mask first set
    uint8_t masked1[4];
    memcpy(masked1, data, len);
    XORMasker::mask(masked1, len, state);

    // Simulate rotation
    state.seed += 1; 
    state.counter = 0;

    uint8_t masked2[4];
    memcpy(masked2, data, len);
    XORMasker::mask(masked2, len, state);

    // Masked results should be different because seed changed
    bool different = false;
    for(size_t i=0; i<len; ++i) if(masked1[i] != masked2[i]) different = true;
    assert(different);
    
    std::cout << "PASSED" << std::endl;
}

int main() {
    test_symmetry();
    test_seed_rotation();
    std::cout << "All masking tests passed!" << std::endl;
    return 0;
}
