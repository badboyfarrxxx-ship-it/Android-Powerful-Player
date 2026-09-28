#include "DecoderCore.h"

DecoderCore::DecoderCore() : codec_ctx_(nullptr), codec_(nullptr) {
    start_time_ = std::chrono::steady_clock::now();
}

DecoderCore::~DecoderCore() {
    destroy();
}

bool DecoderCore::init(AVCodecID codec_id) {
    destroy();

    codec_ = avcodec_find_decoder(codec_id);
    if (!codec_) {
        std::cerr << "Codec not found for ID: " << codec_id << std::endl;
        return false;
    }

    codec_ctx_ = avcodec_alloc_context3(codec_);
    if (!codec_ctx_) {
        std::cerr << "Could not allocate codec context" << std::endl;
        return false;
    }

    if (avcodec_open2(codec_ctx_, codec_, nullptr) < 0) {
        std::cerr << "Could not open codec" << std::endl;
        destroy();
        return false;
    }

    start_time_ = std::chrono::steady_clock::now();
    total_bytes_decoded_ = 0;
    frame_count_ = 0;
    avg_decode_time_ms_ = 0.0;

    return true;
}

void DecoderCore::destroy() {
    if (codec_ctx_) {
        avcodec_free_context(&codec_ctx_);
        codec_ctx_ = nullptr;
    }
    codec_ = nullptr;
}

bool DecoderCore::decode(AVPacket* packet, AVFrame* frame) {
    if (!codec_ctx_ || !packet || !frame) {
        return false;
    }

    auto start = std::chrono::steady_clock::now();

    int ret = avcodec_send_packet(codec_ctx_, packet);
    if (ret < 0) {
        std::cerr << "Error sending packet to decoder: " << ret << std::endl;
        return false;
    }

    while (true) {
        ret = avcodec_receive_frame(codec_ctx_, frame);
        if (ret == 0) {
            auto end = std::chrono::steady_clock::now();
            std::chrono::duration<double, std::milli> elapsed = end - start;

            // Simple moving average for decode time
            avg_decode_time_ms_ = (avg_decode_time_ms_ * 0.9) + (elapsed.count() * 0.1);

            total_bytes_decoded_ += packet->size;
            frame_count_++;

            return true;
        } else if (ret == AVERROR(EAGAIN)) {
            break;
        } else if (ret == AVERROR_EOF) {
            break;
        } else {
            std::cerr << "Error receiving frame from decoder: " << ret << std::endl;
            return false;
        }
    }
    return false;
}

int DecoderCore::getBitrateKbps() const {
    auto now = std::chrono::steady_clock::now();
    std::chrono::duration<double> elapsed = now - start_time_;
    if (elapsed.count() <= 0) return 0;
    return static_cast<int>((total_bytes_decoded_ * 8.0) / (elapsed.count() * 1000.0));
}

float DecoderCore::getFps() const {
    auto now = std::chrono::steady_clock::now();
    std::chrono::duration<double> elapsed = now - start_time_;
    if (elapsed.count() <= 0) return 0.0f;
    return static_cast<float>(frame_count_ / elapsed.count());
}
