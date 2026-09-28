#include <iostream>
#include <vector>
#include <cassert>
#include <cmath>
#include <algorithm>
#include "AudioProcessor.h"

void testPeakNormalization() {
    std::cout << "Running testPeakNormalization..." << std::endl;

    AudioProcessor processor(0.9f);

    // Case 1: Signal needs boosting
    std::vector<float> buffer1 = {0.1f, -0.2f, 0.1f, -0.1f};
    processor.normalize(buffer1.data(), buffer1.size());

    float max1 = 0.0f;
    for(float s : buffer1) max1 = std::max(max1, std::abs(s));

    assert(std::abs(max1 - 0.9f) < 0.001f);
    std::cout << "Case 1 passed: Max amplitude normalized to 0.9" << std::endl;

    // Case 2: Signal needs attenuating (clipping prevention)
    std::vector<float> buffer2 = {1.5f, -2.0f, 0.5f};
    processor.normalize(buffer2.data(), buffer2.size());

    float max2 = 0.0f;
    for(float s : buffer2) max2 = std::max(max2, std::abs(s));

    assert(std::abs(max2 - 0.9f) < 0.001f);
    std::cout << "Case 2 passed: High amplitude signal normalized to 0.9" << std::endl;

    // Case 3: Silent buffer
    std::vector<float> buffer3 = {0.0f, 0.0f, 0.0f};
    processor.normalize(buffer3.data(), buffer3.size());

    for(float s : buffer3) assert(s == 0.0f);
    std::cout << "Case 3 passed: Silent buffer remains silent" << std::endl;
}

int main() {
    try {
        testPeakNormalization();
        std::cout << "All native audio processor tests passed!" << std::endl;
    } catch (const std::exception& e) {
        std::cerr << "Test failed: " << e.what() << std::endl;
        return 1;
    }
    return 0;
}
