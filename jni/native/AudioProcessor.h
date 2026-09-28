#ifndef AUDIO_PROCESSOR_H
#define AUDIO_PROCESSOR_H

#include <vector>
#include <cmath>
#include <algorithm>
#include <iostream>

class AudioProcessor {
public:
    AudioProcessor(float targetPeak = 0.9f)
        : mTargetPeak(targetPeak), mCurrentGain(1.0f) {}

    /**
     * Normalizes a buffer of PCM float samples to the target peak.
     * Implements linear gain smoothing to prevent audible clicks.
     */
    void normalize(float* buffer, size_t numSamples) {
        if (numSamples == 0) return;

        float maxAmplitude = 0.0f;
        for (size_t i = 0; i < numSamples; ++i) {
            float absVal = std::abs(buffer[i]);
            if (absVal > maxAmplitude) {
                maxAmplitude = absVal;
            }
        }

        float targetGain = 1.0f;
        if (maxAmplitude > 0.0f) {
            targetGain = mTargetPeak / maxAmplitude;
        }

        // Linear gain smoothing: ramp from mCurrentGain to targetGain
        float gainStep = (targetGain - mCurrentGain) / static_cast<float>(numSamples);

        for (size_t i = 0; i < numSamples; ++i) {
            float interpolatedGain = mCurrentGain + (gainStep * static_cast<float>(i));
            buffer[i] *= interpolatedGain;
        }

        mCurrentGain = targetGain;
    }

    void setTargetPeak(float peak) {
        mTargetPeak = std::clamp(peak, 0.0f, 1.0f);
    }

private:
    float mTargetPeak;
    float mCurrentGain;
};

#endif // AUDIO_PROCESSOR_H
