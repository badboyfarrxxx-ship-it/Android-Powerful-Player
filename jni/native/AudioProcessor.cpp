#include "AudioProcessor.h"

// This function should be called by the native engine.
// The AudioProcessor instance should be managed by the engine state,
// not as a static local variable, to allow for per-stream configuration.
void processAudioBuffer(AudioProcessor* processor, float* buffer, size_t numSamples) {
    if (processor) {
        processor->normalize(buffer, numSamples);
    }
}
