#ifndef DECODERCORE_H
#define DECODERCORE_H

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/frame.h>
#include <libavutil/mem.h>
}

#include <string>
#include <iostream>
#include <chrono>

class DecoderCore {
public:
    DecoderCore();
    ~DecoderCore();

    bool init(AVCodecID codec_id);
    void destroy();

    // Returns true if a frame was decoded
    bool decode(AVPacket* packet, AVFrame* frame);

    // Metrics getters
    float getAverageDecodeTimeMs() const { return avg_decode_time_ms_; }
    int getBitrateKbps() const;
    float getFps() const;

private:
    AVCodecContext* codec_ctx_ = nullptr;
    const AVCodec* codec_ = nullptr;

    // Performance metrics
    std::chrono::steady_clock::time_point last_decode_start_;
    double avg_decode_time_ms_ = 0.0;
    size_t total_bytes_decoded_ = 0;
    uint64_t frame_count_ = 0;
    std::chrono::steady_clock::time_point start_time_;
};

#endif // DECODERCORE_H
