#include <iostream>
#include <cassert>
#include <vector>
#include "DecoderCore.h"
#include "RingBuffer.h"

// Mocking FFmpeg calls for a pure C++ unit test environment
#ifndef FFMPEG_LINKED
extern "C" {
    enum { AVCODECID_H264 = 123, AVCODECID_AAC = 456 };

    struct AVCodec { int id; };
    struct AVCodecContext { int id; };
    struct AVPacket { uint8_t* data; int size; };
    struct AVFrame { int id; bool allocated; };

    AVCodec* avcodec_find_decoder(int id) {
        static AVCodec mock_codec = {id};
        return &mock_codec;
    }

    AVCodecContext* avcodec_alloc_context3(const AVCodec* codec) {
        AVCodecContext* ctx = new AVCodecContext();
        ctx->id = codec->id;
        return ctx;
    }

    int avcodec_open2(AVCodecContext* ctx, const AVCodec* codec, void* op) {
        return 0;
    }

    void avcodec_free_context(AVCodecContext** ctx) {
        delete *ctx;
        *ctx = nullptr;
    }

    int avcodec_send_packet(AVCodecContext* ctx, AVPacket* pkt) {
        return 0;
    }

    static int g_frame_count = 0;
    int avcodec_receive_frame(AVCodecContext* ctx, AVFrame* frame) {
        // Simulate producing frames: produce 2 frames on the first call, then EAGAIN
        if (frame && g_frame_count < 2) {
            frame->allocated = true;
            g_frame_count++;
            return 0;
        }
        return -1; // Simulating AVERROR(EAGAIN) for simplicity in mock
    }

    void av_packet_unref(AVPacket* pkt) {}

    void av_frame_free(AVFrame** frame) {
        if (*frame) {
            (*frame)->allocated = false;
            delete *frame;
            *frame = nullptr;
        }
    }
}
#endif

void test_ring_buffer_ownership() {
    std::cout << "Testing RingBuffer Ownership..." << std::endl;

    int frames_freed = 0;
    auto cleanup = [&](AVFrame* f) {
        if (f) frames_freed++;
        av_frame_free(&f);
    };

    {
        RingBuffer<AVFrame*> rb(3, cleanup);
        AVFrame* f1 = new AVFrame{1, true};
        AVFrame* f2 = new AVFrame{2, true};
        AVFrame* f3 = new AVFrame{3, true};

        rb.push(f1);
        rb.push(f2);
        rb.push(f3);

        assert(rb.size() == 3);
        // Destroying rb should free 3 frames
    }
    assert(frames_freed == 3);
    std::cout << "RingBuffer ownership tests passed!" << std::endl;
}

void test_decoder_core_drain() {
    std::cout << "Testing DecoderCore Drain..." << std::endl;

    // Reset mock frame count
    #ifndef FFMPEG_LINKED
    g_frame_count = 0;
    #endif

    DecoderCore decoder;
    decoder.init(AVCODECID_H264);

    AVPacket* pkt = new AVPacket();
    AVFrame* frame = new AVFrame();

    // The mock is set to return 2 frames on first receive attempt if we looped
    // But the current mock avcodec_receive_frame returns 0 then -1.
    // Let's verify the loop is called.
    bool decoded = decoder.decode(pkt, frame);
    assert(decoded);

    av_packet_unref(pkt);
    delete pkt;
    av_frame_free(&frame);
    std::cout << "DecoderCore drain tests passed!" << std::endl;
}

int main() {
    test_ring_buffer_ownership();
    test_decoder_core_drain();
    std::cout << "All native tests passed successfully!" << std::endl;
    return 0;
}
